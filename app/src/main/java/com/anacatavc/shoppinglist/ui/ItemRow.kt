package com.anacatavc.shoppinglist.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anacatavc.shoppinglist.R
import com.anacatavc.shoppinglist.data.Item
import com.anacatavc.shoppinglist.data.Urgency
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("dd/MM")

fun Urgency.color(): Color = when (this) {
    Urgency.HIGH -> Color(0xFFD9425B)
    Urgency.MEDIUM -> Color(0xFFE8A13A)
    Urgency.LOW -> Color(0xFF5FB58A)
}

private fun Item.subtitle(context: Context, now: Long): String = buildList {
    if (quantity.isNotBlank()) add(quantity)
    if (recurrenceDays != null) add(context.getString(R.string.every_n_days, recurrenceDays))
    if (!isDue(now) && nextDueAt != null) {
        val day = dayFormat.format(Instant.ofEpochMilli(nextDueAt).atZone(ZoneId.systemDefault()))
        add(context.getString(R.string.comes_back_on, day))
    }
    if (note.isNotBlank()) add(note)
}.joinToString(" · ")

/** Recurring items that are not due yet are shown faded. */
@Composable
fun ItemRow(
    item: Item,
    now: Long,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    val subtitle = item.subtitle(LocalContext.current, now)
    ListItem(
        modifier = Modifier.clickable(onClick = onClick).alpha(if (item.isDue(now)) 1f else 0.5f),
        leadingContent = leading ?: { Box(Modifier.size(12.dp).background(item.urgency.color(), CircleShape)) },
        headlineContent = { Text(item.name) },
        supportingContent = if (subtitle.isEmpty()) null else { { Text(subtitle) } },
    )
}
