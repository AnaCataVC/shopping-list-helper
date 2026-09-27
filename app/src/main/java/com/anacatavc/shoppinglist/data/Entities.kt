package com.anacatavc.shoppinglist.data

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anacatavc.shoppinglist.R

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
) {
    val label: String get() = "$emoji $name"
}

/** Persisted by constant name: renaming a constant requires a data migration. */
enum class Urgency(@StringRes val label: Int) {
    HIGH(R.string.urgency_high),
    MEDIUM(R.string.urgency_medium),
    LOW(R.string.urgency_low),
}

@Entity(
    tableName = "items",
    foreignKeys = [
        // RESTRICT: a category can only be deleted once its items were moved elsewhere.
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("categoryId")],
)
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: String = "",
    val note: String = "",
    val categoryId: Long,
    val urgency: Urgency = Urgency.MEDIUM,
    /** Null for one-off purchases; otherwise the item comes back this many days after being bought. */
    val recurrenceDays: Int? = null,
    val done: Boolean = false,
    val lastBoughtAt: Long? = null,
    /** For recurring items: the item stays hidden from shopping lists until this instant. */
    val nextDueAt: Long? = null,
) {
    fun isDue(now: Long): Boolean = !done && (nextDueAt == null || nextDueAt <= now)

    /** One-off items are closed; recurring items are rescheduled instead of closed. */
    fun bought(now: Long): Item =
        if (recurrenceDays == null) copy(done = true, lastBoughtAt = now)
        else copy(lastBoughtAt = now, nextDueAt = now + recurrenceDays * DAY_MS)

    /**
     * Sets the recurrence and keeps [nextDueAt] consistent with it: turning recurrence off makes the
     * item due right away, and changing the period reschedules it from the last purchase.
     */
    fun withRecurrence(days: Int?): Item = when {
        days == null -> copy(recurrenceDays = null, nextDueAt = null)
        days == recurrenceDays || lastBoughtAt == null -> copy(recurrenceDays = days)
        else -> copy(recurrenceDays = days, nextDueAt = lastBoughtAt + days * DAY_MS)
    }

    companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
