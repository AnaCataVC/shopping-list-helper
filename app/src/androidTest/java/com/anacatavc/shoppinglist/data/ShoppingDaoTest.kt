package com.anacatavc.shoppinglist.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: ShoppingDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun category(name: String): Category {
        dao.upsertCategory(Category(name = name, emoji = "x"))
        return dao.allCategories().single { it.name == name }
    }

    @Test
    fun upsertInsertsThenUpdatesItem() = runTest {
        val market = category("Supermercado")
        dao.upsertItem(Item(name = "Pan", categoryId = market.id))
        val inserted = dao.allItems().single()

        dao.upsertItem(inserted.copy(name = "Pan integral", urgency = Urgency.HIGH))

        val updated = dao.allItems().single()
        assertEquals(inserted.id, updated.id)
        assertEquals("Pan integral", updated.name)
        assertEquals(Urgency.HIGH, updated.urgency)
    }

    @Test
    fun categoryRenameKeepsItsItems() = runTest {
        val market = category("Supermercado")
        dao.upsertItem(Item(name = "Pan", categoryId = market.id))

        dao.upsertCategory(market.copy(name = "Súper", emoji = "🧺"))

        assertEquals("Súper", dao.allCategories().single().name)
        assertEquals(market.id, dao.allItems().single().categoryId)
    }

    @Test
    fun categoryNamesAreUnique() = runTest {
        category("Ropa")
        try {
            dao.upsertCategory(Category(name = "Ropa", emoji = "y"))
            fail("Duplicate category name was accepted")
        } catch (expected: SQLiteConstraintException) {
        }
    }

    @Test
    fun itemRequiresExistingCategory() = runTest {
        try {
            dao.upsertItem(Item(name = "Huérfano", categoryId = 404))
            fail("Item with unknown category was accepted")
        } catch (expected: SQLiteConstraintException) {
        }
    }

    @Test
    fun deletingCategoryWithItemsIsRejectedByForeignKey() = runTest {
        val clothes = category("Ropa")
        dao.upsertItem(Item(name = "Calcetines", categoryId = clothes.id))
        try {
            dao.deleteCategoryById(clothes.id)
            fail("Category with items was deleted")
        } catch (expected: SQLiteConstraintException) {
        }
        assertEquals(1, dao.allCategories().size)
    }

    @Test
    fun deletingEmptyCategoryWorks() = runTest {
        val clothes = category("Ropa")
        dao.deleteCategoryById(clothes.id)
        assertTrue(dao.allCategories().isEmpty())
    }

    @Test
    fun moveItemsAndDeleteCategoryMovesEverythingThenDeletes() = runTest {
        val clothes = category("Ropa")
        val other = category("Otros")
        val untouched = category("Hogar")
        dao.upsertItem(Item(name = "Calcetines", categoryId = clothes.id))
        dao.upsertItem(Item(name = "Polera", categoryId = clothes.id, done = true))
        dao.upsertItem(Item(name = "Ampolleta", categoryId = untouched.id))

        dao.moveItemsAndDeleteCategory(fromId = clothes.id, toId = other.id)

        assertEquals(listOf("Hogar", "Otros"), dao.allCategories().map { it.name })
        val byName = dao.allItems().associateBy { it.name }
        assertEquals(other.id, byName.getValue("Calcetines").categoryId)
        assertEquals(other.id, byName.getValue("Polera").categoryId)
        assertEquals(untouched.id, byName.getValue("Ampolleta").categoryId)
    }

    @Test
    fun moveToMissingCategoryRollsBackWholeTransaction() = runTest {
        val clothes = category("Ropa")
        dao.upsertItem(Item(name = "Calcetines", categoryId = clothes.id))
        try {
            dao.moveItemsAndDeleteCategory(fromId = clothes.id, toId = 404)
            fail("Move to a missing category succeeded")
        } catch (expected: SQLiteConstraintException) {
        }
        assertEquals(clothes.id, dao.allItems().single().categoryId)
        assertEquals(1, dao.allCategories().size)
    }

    @Test
    fun deleteBoughtOnlyRemovesDoneItems() = runTest {
        val market = category("Supermercado")
        val now = System.currentTimeMillis()
        dao.upsertItem(Item(name = "Pan", categoryId = market.id).bought(now))
        dao.upsertItem(Item(name = "Leche", categoryId = market.id, recurrenceDays = 7).bought(now))
        dao.upsertItem(Item(name = "Huevos", categoryId = market.id))

        dao.deleteBought()

        assertEquals(listOf("Huevos", "Leche"), dao.allItems().map { it.name })
    }

    @Test
    fun upsertItemsBuysWholeListAtOnce() = runTest {
        val market = category("Supermercado")
        dao.upsertItem(Item(name = "Pan", categoryId = market.id))
        dao.upsertItem(Item(name = "Leche", categoryId = market.id, recurrenceDays = 7))
        val now = System.currentTimeMillis()

        dao.upsertItems(dao.allItems().map { it.bought(now) })

        assertTrue(dao.allItems().none { it.isDue(now) })
    }

    @Test
    fun flowsEmitSortedCaseInsensitive() = runTest {
        category("ropa")
        category("Hogar")
        category("abrigos")
        assertEquals(listOf("abrigos", "Hogar", "ropa"), dao.categories().first().map { it.name })
    }
}
