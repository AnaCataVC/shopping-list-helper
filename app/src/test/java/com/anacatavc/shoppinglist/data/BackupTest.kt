package com.anacatavc.shoppinglist.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    private val categories = listOf(
        Category(id = 1, name = "Supermercado", emoji = "🛒"),
        Category(id = 2, name = "Farmacia \"24h\"", emoji = "💊"),
    )
    private val items = listOf(
        Item(id = 10, name = "Leche", quantity = "2 L", categoryId = 1, urgency = Urgency.HIGH, recurrenceDays = 7),
        Item(
            id = 11, name = "Paracetamol", note = "línea 1\nlínea 2", categoryId = 2, urgency = Urgency.LOW,
            done = true, lastBoughtAt = 123L,
        ),
    )

    private fun export() = JSONObject(backupJson(categories, items, exportedAt = 999L))

    @Test
    fun `header carries format version and export time`() {
        val json = export()
        assertEquals(1, json.getInt("version"))
        assertEquals(999L, json.getLong("exportedAt"))
    }

    @Test
    fun `all categories are exported with their fields`() {
        val exported = export().getJSONArray("categories")
        assertEquals(2, exported.length())
        val second = exported.getJSONObject(1)
        assertEquals(2L, second.getLong("id"))
        assertEquals("Farmacia \"24h\"", second.getString("name"))
        assertEquals("💊", second.getString("emoji"))
    }

    @Test
    fun `all item fields round-trip`() {
        val exported = export().getJSONArray("items")
        assertEquals(2, exported.length())

        val milk = exported.getJSONObject(0)
        assertEquals("Leche", milk.getString("name"))
        assertEquals("2 L", milk.getString("quantity"))
        assertEquals(1L, milk.getLong("categoryId"))
        assertEquals("HIGH", milk.getString("urgency"))
        assertEquals(7, milk.getInt("recurrenceDays"))
        assertEquals(false, milk.getBoolean("done"))

        val pill = exported.getJSONObject(1)
        assertEquals("línea 1\nlínea 2", pill.getString("note"))
        assertEquals(true, pill.getBoolean("done"))
        assertEquals(123L, pill.getLong("lastBoughtAt"))
    }

    @Test
    fun `missing optional values are explicit nulls, not absent keys`() {
        val milk = export().getJSONArray("items").getJSONObject(0)
        for (key in listOf("lastBoughtAt", "nextDueAt")) {
            assertTrue("$key should be present", milk.has(key))
            assertTrue("$key should be null", milk.isNull(key))
        }
        val pill = export().getJSONArray("items").getJSONObject(1)
        assertTrue(pill.isNull("recurrenceDays"))
    }

    @Test
    fun `empty database exports empty arrays`() {
        val json = JSONObject(backupJson(emptyList(), emptyList(), exportedAt = 0L))
        assertEquals(0, json.getJSONArray("categories").length())
        assertEquals(0, json.getJSONArray("items").length())
    }
}
