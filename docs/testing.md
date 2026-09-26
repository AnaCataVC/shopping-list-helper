# Testing plan

Three automated layers plus a short manual pass for what automation cannot see (visual theme,
system language, the file picker).

| Layer | Where | Runs on | Command |
|-------|-------|---------|---------|
| Unit | `app/src/test/` | JVM, no device | `.\gradlew testDebugUnitTest` |
| Database | `app/src/androidTest/.../data/` | Device or emulator | `.\gradlew connectedDebugAndroidTest` |
| UI flows | `app/src/androidTest/.../ui/` | Device or emulator | `.\gradlew connectedDebugAndroidTest` |
| Manual | This document | Physical phone | — |

`JAVA_HOME` must point to a JDK 17+ (see README). Instrumented tests need a connected device or a
running emulator (API 26+).

> The UI flow tests wipe the app's database and settings before every test. Run them on an
> emulator or a test phone, not on the phone holding your real list.

## 1. Unit tests (JVM)

### `ItemTest` — due / purchase rules
| Case | Expected |
|------|----------|
| New item | Due |
| `done = true` | Never due |
| `nextDueAt` in the future | Not due |
| `nextDueAt` equal to or before now | Due |
| Buy one-off item | `done = true`, `lastBoughtAt = now`, never due again |
| Buy recurring item (7 days) | Stays `done = false`, `nextDueAt = now + 7 days` |
| Recurring item 1 ms before / at its date | Not due / due |
| Buy recurring item twice | Period restarts from the second purchase |
| 3650-day recurrence | No integer overflow |
| Urgency ordering | HIGH < MEDIUM < LOW (sort puts urgent first) |

### `BackupTest` — JSON export
| Case | Expected |
|------|----------|
| Header | `version = 1`, `exportedAt` as given |
| Categories | All exported with `id`, `name`, `emoji`; quotes in names are escaped |
| Items | Every field exported; multi-line notes survive |
| Optional values unset | Keys present with `null`, not missing |
| Empty database | Empty `categories` and `items` arrays |

## 2. Database tests (`ShoppingDaoTest`, in-memory Room)
| Case | Expected |
|------|----------|
| Upsert item twice | Second call updates the same row |
| Rename category | Items keep pointing at it |
| Duplicate category name | Rejected (unique index) |
| Item with unknown category | Rejected (foreign key) |
| Delete category that has items | Rejected by `RESTRICT` foreign key, category still there |
| Delete empty category | Works |
| `countItemsIn` | Counts pending **and** bought items |
| `moveItemsAndDeleteCategory` | Moves pending and bought items, deletes source, leaves other categories alone |
| Move to a non-existent category | Whole transaction rolls back |
| `deleteBought` | Removes only `done` items; bought recurring items stay |
| `upsertItems` with `bought()` | Whole list bought in one call |
| Category flow | Sorted case-insensitively |

## 3. UI flow tests (`AppFlowTest`, Compose on the real activity)

Each test starts with two categories (Supermercado, Ropa), no items, and default theme. Texts are
read from resources, so the suite passes with the device in English or Spanish.

| Area | Flow | Expected |
|------|------|----------|
| Pending | No items | Empty-state message |
| Pending | Add item with quantity | Shown under its category header, stored in DB |
| Pending | Blank name | Save disabled |
| Pending | Edit item name and category | DB updated |
| Pending | Delete from edit dialog | Item gone |
| Shopping | Pick a category | Only that category's due items; chip shows the count |
| Shopping | Tick one item | Only that item leaves the list; count drops |
| Shopping | Mark whole list | Empty message; one-off → done, recurring → rescheduled, other categories untouched |
| Recurrence | Buy recurring item | Still in Pending with "every N days" and a return date |
| Recurrence | Return date already passed | Back in the shopping list |
| Categories | Rename | New name shown |
| Categories | Duplicate name (any case) | Error shown, save disabled |
| Categories | Delete empty | Plain confirmation, then gone |
| Categories | Delete with items | Warning with item count; button disabled until a target is chosen; items (including bought) moved |
| Categories | Delete the only category that has items | Explains a new category is needed; button disabled |
| Theme | Choose Dark | Saved, survives activity recreation |

## 4. Manual checklist

Run on a physical phone after installing a debug build (`.\gradlew installDebug`).

**Icon and launch**
- [ ] Launcher icon is the cart, not cropped, on round and squircle launchers.
- [ ] First launch creates the six default categories in the phone's language.

**Theme**
- [ ] *Same as system*: toggling the phone's dark mode switches the app live.
- [ ] *Light* / *Dark*: app ignores the phone setting; status bar icons stay readable.
- [ ] Dialogs, chips, checkboxes and the bottom bar are readable in both themes.
- [ ] Choice survives closing the app from recents.

**Language**
- [ ] Phone in Spanish → app name "Por Comprar", all text in Spanish.
- [ ] Phone in English → app name "To Buy", all text in English.
- [ ] Android 13+: Settings → Apps → app → Language lists English and Spanish, and switching
      changes the app without changing the phone.
- [ ] Plurals read correctly with 1 and with several items in the delete-category dialog.

**Backup**
- [ ] Menu → Export: the system picker opens with a dated file name.
- [ ] Saving to Downloads and to Drive both work; the file opens and matches the format in
      `architecture.md`.
- [ ] Cancelling the picker shows no error.

**Robustness**
- [ ] Rotate the phone with a dialog open: nothing crashes (unsaved dialog text may reset).
- [ ] Long item and category names wrap without breaking the layout.
- [ ] Emoji field accepts multi-codepoint emoji (e.g. 🧑‍🍳).
- [ ] Recurring item: set recurrence to 1 day, buy it, change the phone date forward a day, reopen
      the app → it is due again.

## Not covered (on purpose)
- Database migrations: there is only schema version 1. Add a `MigrationTestHelper` test with the
  first schema change.
- Screenshot or visual regression tests: the manual theme pass is enough at this size.
- Import: not implemented yet.
