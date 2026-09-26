package com.anacatavc.shoppinglist.ui

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anacatavc.shoppinglist.R
import com.anacatavc.shoppinglist.data.AppDatabase
import com.anacatavc.shoppinglist.data.Category
import com.anacatavc.shoppinglist.data.Item
import com.anacatavc.shoppinglist.data.ShoppingDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end flows on the real app. Each test starts from a wiped database with two categories
 * and the default theme. Texts are read from resources so the suite runs in any device language.
 */
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dao: ShoppingDao = AppDatabase.get(context).dao()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private var marketId = 0L
    private var clothesId = 0L

    private fun s(id: Int, vararg args: Any) = context.getString(id, *args)

    @Before
    fun setUp() {
        AppDatabase.get(context).clearAllTables()
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().clear().commit()
        runBlocking {
            dao.upsertCategory(Category(name = "Supermercado", emoji = "🛒"))
            dao.upsertCategory(Category(name = "Ropa", emoji = "👕"))
            val byName = dao.allCategories().associateBy { it.name }
            marketId = byName.getValue("Supermercado").id
            clothesId = byName.getValue("Ropa").id
        }
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) scenario.close()
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun seed(vararg items: Item) = runBlocking { items.forEach { dao.upsertItem(it) } }

    private fun openTab(label: Int) = compose.onNodeWithText(s(label), useUnmergedTree = true).performClick()

    // --- Pending items ---------------------------------------------------------------------

    @Test
    fun emptyStateIsShownWithoutItems() {
        launch()
        compose.onNodeWithText(s(R.string.pending_empty)).assertExists()
    }

    @Test
    fun addItemShowsItUnderItsCategory() {
        launch()
        compose.onNodeWithContentDescription(s(R.string.add_item)).performClick()
        compose.onNodeWithText(s(R.string.item_name)).performTextInput("Leche")
        compose.onNodeWithText(s(R.string.item_quantity)).performTextInput("2 L")
        compose.onNodeWithText(s(R.string.save)).performClick()

        compose.onNodeWithText("🛒 Supermercado").assertExists()
        compose.onNodeWithText("Leche").assertExists()
        compose.onNodeWithText("2 L", substring = true).assertExists()
        assertEquals(1, runBlocking { dao.allItems() }.size)
    }

    @Test
    fun saveIsDisabledWithoutName() {
        launch()
        compose.onNodeWithContentDescription(s(R.string.add_item)).performClick()
        compose.onNodeWithText(s(R.string.save)).assertIsNotEnabled()
        compose.onNodeWithText(s(R.string.item_name)).performTextInput("   ")
        compose.onNodeWithText(s(R.string.save)).assertIsNotEnabled()
    }

    @Test
    fun editItemChangesNameAndCategory() {
        seed(Item(name = "Pan", categoryId = marketId))
        launch()
        compose.onNodeWithText("Pan").performClick()
        compose.onNodeWithText(s(R.string.item_edit)).assertExists()
        compose.onNodeWithText(s(R.string.item_name)).performTextClearance()
        compose.onNodeWithText(s(R.string.item_name)).performTextInput("Calcetines")
        compose.onNodeWithText("👕 Ropa").performClick()
        compose.onNodeWithText(s(R.string.save)).performClick()

        val item = runBlocking { dao.allItems() }.single()
        assertEquals("Calcetines", item.name)
        assertEquals(clothesId, item.categoryId)
    }

    @Test
    fun deleteItemFromEditDialog() {
        seed(Item(name = "Pan", categoryId = marketId))
        launch()
        compose.onNodeWithText("Pan").performClick()
        compose.onNodeWithText(s(R.string.delete)).performClick()
        compose.onNodeWithText(s(R.string.pending_empty)).assertExists()
    }

    // --- "Going shopping" -----------------------------------------------------------------

    @Test
    fun shoppingListOnlyShowsSelectedCategory() {
        seed(Item(name = "Pan", categoryId = marketId), Item(name = "Polera", categoryId = clothesId))
        launch()
        openTab(R.string.tab_shop)
        compose.onNodeWithText("Supermercado (1)", substring = true).performClick()

        compose.onNodeWithText("Pan").assertExists()
        compose.onNodeWithText("Polera").assertDoesNotExist()
    }

    @Test
    fun buyingOneItemRemovesOnlyThatOne() {
        seed(Item(name = "Pan", categoryId = marketId), Item(name = "Leche", categoryId = marketId))
        launch()
        openTab(R.string.tab_shop)
        compose.onNodeWithText("Supermercado", substring = true).performClick()
        compose.onNodeWithText("Pan").performClick()

        compose.onNodeWithText("Pan").assertDoesNotExist()
        compose.onNodeWithText("Leche").assertExists()
        compose.onNodeWithText("Supermercado (1)", substring = true).assertExists()
    }

    @Test
    fun markWholeListAsBought() {
        seed(
            Item(name = "Pan", categoryId = marketId),
            Item(name = "Leche", categoryId = marketId, recurrenceDays = 7),
            Item(name = "Polera", categoryId = clothesId),
        )
        launch()
        openTab(R.string.tab_shop)
        compose.onNodeWithText("Supermercado", substring = true).performClick()
        compose.onNodeWithText(s(R.string.shop_mark_all)).performClick()

        compose.onNodeWithText(s(R.string.shop_empty)).assertExists()
        val byName = runBlocking { dao.allItems() }.associateBy { it.name }
        assertEquals(true, byName.getValue("Pan").done)
        assertEquals(false, byName.getValue("Leche").done)
        assertEquals(false, byName.getValue("Polera").done)
    }

    @Test
    fun boughtRecurringItemStaysInPendingWithReturnDate() {
        seed(Item(name = "Leche", categoryId = marketId, recurrenceDays = 7))
        launch()
        openTab(R.string.tab_shop)
        compose.onNodeWithText("Supermercado", substring = true).performClick()
        compose.onNodeWithText("Leche").performClick()
        openTab(R.string.tab_pending)

        compose.onNodeWithText("Leche").assertExists()
        compose.onNodeWithText(s(R.string.every_n_days, 7), substring = true).assertExists()
        compose.onNodeWithText(s(R.string.comes_back_on, ""), substring = true).assertExists()
    }

    @Test
    fun recurringItemWhoseDateArrivedIsBackInShoppingList() {
        seed(Item(name = "Leche", categoryId = marketId, recurrenceDays = 7, nextDueAt = System.currentTimeMillis() - 1))
        launch()
        openTab(R.string.tab_shop)
        compose.onNodeWithText("Supermercado (1)", substring = true).performClick()
        compose.onNodeWithText("Leche").assertExists()
    }

    // --- Categories -----------------------------------------------------------------------

    @Test
    fun renameCategory() {
        launch()
        openTab(R.string.tab_categories)
        compose.onNodeWithText("Ropa").performClick()
        compose.onNodeWithText(s(R.string.category_name)).performTextClearance()
        compose.onNodeWithText(s(R.string.category_name)).performTextInput("Vestuario")
        compose.onNodeWithText(s(R.string.save)).performClick()

        compose.onNodeWithText("Vestuario").assertExists()
    }

    @Test
    fun duplicateCategoryNameIsRejected() {
        launch()
        openTab(R.string.tab_categories)
        compose.onNodeWithContentDescription(s(R.string.add_category)).performClick()
        compose.onNodeWithText(s(R.string.category_name)).performTextInput("ropa")

        compose.onNodeWithText(s(R.string.category_duplicate)).assertExists()
        compose.onNodeWithText(s(R.string.save)).assertIsNotEnabled()
    }

    @Test
    fun deleteEmptyCategoryAsksOnlyForConfirmation() {
        launch()
        openTab(R.string.tab_categories)
        compose.onNodeWithContentDescription(s(R.string.delete_category_cd, "Ropa")).performClick()
        compose.onNodeWithText(s(R.string.category_delete_empty)).assertExists()
        compose.onNodeWithText(s(R.string.delete)).performClick()

        compose.onNodeWithText("Ropa").assertDoesNotExist()
    }

    @Test
    fun deleteCategoryWithItemsRequiresMovingThemFirst() {
        seed(Item(name = "Polera", categoryId = clothesId), Item(name = "Zapatos", categoryId = clothesId, done = true))
        launch()
        openTab(R.string.tab_categories)
        compose.onNodeWithContentDescription(s(R.string.delete_category_cd, "Ropa")).performClick()

        val warning = context.resources.getQuantityString(R.plurals.category_delete_move, 2, 2)
        compose.onNodeWithText(warning).assertExists()
        compose.onNodeWithText(s(R.string.category_move_and_delete)).assertIsNotEnabled()

        compose.onNodeWithText("🛒 Supermercado").performClick()
        compose.onNodeWithText(s(R.string.category_move_and_delete)).assertIsEnabled().performClick()

        compose.onNodeWithText("Ropa").assertDoesNotExist()
        val items = runBlocking { dao.allItems() }
        assertEquals(2, items.size)
        assertEquals(setOf(marketId), items.map { it.categoryId }.toSet())
    }

    @Test
    fun lastCategoryWithItemsCannotBeDeleted() {
        runBlocking { dao.deleteCategoryById(marketId) }
        seed(Item(name = "Polera", categoryId = clothesId))
        launch()
        openTab(R.string.tab_categories)
        compose.onNodeWithContentDescription(s(R.string.delete_category_cd, "Ropa")).performClick()

        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.category_delete_no_target, 1, 1))
            .assertExists()
        compose.onNodeWithText(s(R.string.category_move_and_delete)).assertIsNotEnabled()
    }

    // --- Theme ----------------------------------------------------------------------------

    @Test
    fun themeChoiceIsSavedAndRestored() {
        launch()
        assertNull(context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("theme", null))

        compose.onNodeWithContentDescription(s(R.string.menu_more)).performClick()
        compose.onNodeWithText(s(R.string.menu_theme)).performClick()
        compose.onNodeWithText(s(R.string.theme_dark)).performClick()

        assertEquals(ThemeMode.DARK, loadThemeMode(context))
        scenario.recreate()
        assertEquals(ThemeMode.DARK, loadThemeMode(context))
    }
}
