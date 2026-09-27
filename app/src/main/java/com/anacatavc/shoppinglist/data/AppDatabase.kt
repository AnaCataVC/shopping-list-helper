package com.anacatavc.shoppinglist.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anacatavc.shoppinglist.R
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun categories(): Flow<List<Category>>

    @Query("SELECT * FROM items ORDER BY name COLLATE NOCASE")
    fun items(): Flow<List<Item>>

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    suspend fun allCategories(): List<Category>

    @Query("SELECT * FROM items ORDER BY name COLLATE NOCASE")
    suspend fun allItems(): List<Item>

    @Upsert
    suspend fun upsertCategory(category: Category)

    @Upsert
    suspend fun upsertItem(item: Item)

    @Upsert
    suspend fun upsertItems(items: List<Item>)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("DELETE FROM items WHERE done = 1")
    suspend fun deleteBought()

    @Query("UPDATE items SET categoryId = :toId WHERE categoryId = :fromId")
    suspend fun moveItems(fromId: Long, toId: Long)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)

    @Transaction
    suspend fun moveItemsAndDeleteCategory(fromId: Long, toId: Long) {
        moveItems(fromId, toId)
        deleteCategoryById(fromId)
    }
}

@Database(entities = [Category::class, Item::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): ShoppingDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "shopping.db")
                .addCallback(SeedCategories(context.applicationContext))
                .build()
                .also { instance = it }
        }
    }
}

/** Default categories, named in the device language at the moment the database is created. */
private class SeedCategories(private val context: Context) : RoomDatabase.Callback() {
    private val defaults = listOf(
        R.string.seed_supermarket to "🛒", R.string.seed_clothes to "👕", R.string.seed_medicine to "💊",
        R.string.seed_toiletries to "🧴", R.string.seed_home to "🏠", R.string.seed_other to "📦",
    )

    override fun onCreate(db: SupportSQLiteDatabase) {
        defaults.forEach { (name, emoji) ->
            db.execSQL("INSERT INTO categories (name, emoji) VALUES (?, ?)", arrayOf(context.getString(name), emoji))
        }
    }
}
