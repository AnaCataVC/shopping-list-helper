package com.anacatavc.shoppinglist.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemTest {
    private val now = 1_800_000_000_000L
    private val base = Item(id = 1, name = "Leche", categoryId = 1)

    @Test
    fun `new item is due`() {
        assertTrue(base.isDue(now))
    }

    @Test
    fun `done item is never due`() {
        assertFalse(base.copy(done = true).isDue(now))
    }

    @Test
    fun `item scheduled in the future is not due`() {
        assertFalse(base.copy(nextDueAt = now + 1).isDue(now))
    }

    @Test
    fun `item becomes due exactly at nextDueAt`() {
        assertTrue(base.copy(nextDueAt = now).isDue(now))
        assertTrue(base.copy(nextDueAt = now - 1).isDue(now))
    }

    @Test
    fun `buying a one-off item closes it`() {
        val bought = base.bought(now)
        assertTrue(bought.done)
        assertEquals(now, bought.lastBoughtAt)
        assertNull(bought.nextDueAt)
        assertFalse(bought.isDue(now + 365 * Item.DAY_MS))
    }

    @Test
    fun `buying a recurring item reschedules it instead of closing it`() {
        val bought = base.copy(recurrenceDays = 7).bought(now)
        assertFalse(bought.done)
        assertEquals(now, bought.lastBoughtAt)
        assertEquals(now + 7 * Item.DAY_MS, bought.nextDueAt)
    }

    @Test
    fun `recurring item is hidden until its period elapses, then due again`() {
        val bought = base.copy(recurrenceDays = 7).bought(now)
        assertFalse(bought.isDue(now + 7 * Item.DAY_MS - 1))
        assertTrue(bought.isDue(now + 7 * Item.DAY_MS))
    }

    @Test
    fun `buying a recurring item again restarts the period from the new purchase`() {
        val later = now + 10 * Item.DAY_MS
        val twice = base.copy(recurrenceDays = 7).bought(now).bought(later)
        assertEquals(later + 7 * Item.DAY_MS, twice.nextDueAt)
    }

    @Test
    fun `long recurrence does not overflow`() {
        val bought = base.copy(recurrenceDays = 3650).bought(now)
        assertEquals(now + 3650L * Item.DAY_MS, bought.nextDueAt)
    }

    @Test
    fun `turning recurrence off makes a scheduled item due right away`() {
        val edited = base.copy(recurrenceDays = 7).bought(now).withRecurrence(null)
        assertNull(edited.recurrenceDays)
        assertNull(edited.nextDueAt)
        assertTrue(edited.isDue(now))
    }

    @Test
    fun `changing the period reschedules from the last purchase`() {
        val edited = base.copy(recurrenceDays = 30).bought(now).withRecurrence(7)
        assertEquals(now + 7 * Item.DAY_MS, edited.nextDueAt)
    }

    @Test
    fun `keeping the same period keeps the schedule`() {
        val bought = base.copy(recurrenceDays = 7).bought(now)
        assertEquals(bought, bought.withRecurrence(7))
    }

    @Test
    fun `adding recurrence to a never-bought item keeps it due`() {
        val edited = base.withRecurrence(7)
        assertEquals(7, edited.recurrenceDays)
        assertTrue(edited.isDue(now))
    }

    @Test
    fun `urgency order is high, medium, low`() {
        assertEquals(listOf(Urgency.HIGH, Urgency.MEDIUM, Urgency.LOW), Urgency.entries.sorted())
    }
}
