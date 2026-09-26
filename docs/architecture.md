# Architecture

Single-module Android app: Kotlin, Jetpack Compose (Material 3) and Room. No backend, no DI
framework, no navigation library.

```
app/src/main/java/com/anacatavc/shoppinglist/
├── data/
│   ├── Entities.kt      Category, Item, Urgency + purchase/recurrence rules
│   ├── AppDatabase.kt   Room database, DAO, default categories seed
│   └── Backup.kt        JSON backup serialization
└── ui/
    ├── MainActivity.kt  Top bar menu (theme, export, cleanup), bottom tabs
    ├── Theme.kt         Light/dark Material 3 color schemes and the saved theme choice
    ├── PendingScreen.kt All pending items grouped by category
    ├── ShopScreen.kt    "Voy a comprar": list for one category, buy per item or all
    ├── CategoriesScreen.kt  Create / edit / delete categories
    ├── ItemDialog.kt    Create / edit / delete an item
    └── ItemRow.kt       Shared item row
```

## Data flow

The DAO exposes `Flow`s for categories and items. `MainActivity` collects them once and passes the
lists down to the screens; screens write through the DAO in a coroutine and the flows re-emit.
Data volume is small (a personal shopping list), so filtering and grouping are done in memory.

## Theme

Material Design 3 with two hand-picked color schemes (light and dark) based on the icon palette.
The user's choice (`SYSTEM`, `LIGHT`, `DARK`) is stored in `SharedPreferences`; `SYSTEM` follows
the phone's dark mode. System bar icon colors are switched along with the in-app theme.
Dynamic color (Material You wallpaper colors) is intentionally not used, to keep the brand colors.

## Localization

All UI text lives in `res/values/strings.xml` (English, default) and `res/values-es/strings.xml`
(Spanish). `res/xml/locales_config.xml` declares both so Android 13+ offers a per-app language
setting. Default categories are inserted in the device language when the database is first
created; after that they are regular user data and are not translated.

## Data model

| Table        | Columns |
|--------------|---------|
| `categories` | `id`, `name` (unique), `emoji` |
| `items`      | `id`, `name`, `quantity`, `note`, `categoryId` → `categories.id`, `urgency` (`HIGH`/`MEDIUM`/`LOW`), `recurrenceDays` (nullable), `done`, `lastBoughtAt` (nullable, epoch ms), `nextDueAt` (nullable, epoch ms) |

Default categories (Supermarket, Clothes, Medicine, Toiletries, Home, Other) are inserted when the
database is first created, named in the device language (see Localization).

## Rules

### Due items and recurrence

An item is **due** when `done = false` and (`nextDueAt` is null or already in the past).
Only due items show up in the "Voy a comprar" list.

Buying an item (`Item.bought`):

- **One-off** (`recurrenceDays = null`): `done = true`. It disappears from all lists and can be
  removed with *Borrar ítems ya comprados*.
- **Recurring**: stays `done = false`, and `nextDueAt = now + recurrenceDays`. It is shown faded in
  *Pendientes* with the date it comes back, and reappears in the shopping list once due.

Turning recurrence off on an item clears `nextDueAt`, so it becomes due immediately.
No background job is needed: due-ness is evaluated against the current time when the screen renders.

### Deleting a category

A category that still has items (pending or bought) cannot be deleted directly. The delete dialog
shows how many items it has and asks for a destination category; confirming moves the items and
deletes the category in a single transaction (`moveItemsAndDeleteCategory`). The `items.categoryId`
foreign key uses `RESTRICT`, so the database also rejects deleting a category that is still
referenced.

## Backup format

`Exportar respaldo JSON` writes a file through the Storage Access Framework
(`ACTION_CREATE_DOCUMENT`), so no storage permission is requested.

```json
{
  "version": 1,
  "exportedAt": 1790000000000,
  "categories": [
    { "id": 1, "name": "Supermercado", "emoji": "🛒" }
  ],
  "items": [
    {
      "id": 1, "name": "Leche", "quantity": "2 L", "note": "",
      "categoryId": 1, "urgency": "HIGH",
      "recurrenceDays": 7, "done": false,
      "lastBoughtAt": null, "nextDueAt": null
    }
  ]
}
```

Timestamps are epoch milliseconds. `version` is bumped if the format changes.

## Schema changes

Room exports the schema to `app/schemas/`. Changing an entity requires bumping the database
`version` and adding a migration, otherwise existing installs fail to open the database.
