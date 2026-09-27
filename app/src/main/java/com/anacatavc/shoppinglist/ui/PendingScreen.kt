package com.anacatavc.shoppinglist.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anacatavc.shoppinglist.R
import com.anacatavc.shoppinglist.data.Category
import com.anacatavc.shoppinglist.data.Item
import com.anacatavc.shoppinglist.data.ShoppingDao

private const val NEW_ITEM_ID = 0L

/** Everything not yet bought, grouped by category and sorted by urgency. */
@Composable
fun PendingScreen(items: List<Item>, categories: List<Category>, dao: ShoppingDao, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val now by rememberNow()
    // Only the id is saved so the dialog survives rotation; null = closed, NEW_ITEM_ID = new item.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val pendingByCategory = items.filter { !it.done }.groupBy { it.categoryId }

    Box(modifier.fillMaxSize()) {
        if (pendingByCategory.isEmpty()) {
            Text(
                stringResource(R.string.pending_empty),
                Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            categories.filter { it.id in pendingByCategory }.forEach { category ->
                item(key = "header-${category.id}") {
                    Text(
                        category.label,
                        Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                val sorted = pendingByCategory.getValue(category.id).sortedWith(compareBy({ !it.isDue(now) }, { it.urgency }))
                items(sorted, key = { it.id }) { item -> ItemRow(item, now, onClick = { editingId = item.id }) }
            }
        }
        FloatingActionButton(
            onClick = {
                if (categories.isEmpty()) Toast.makeText(context, R.string.add_item_needs_category, Toast.LENGTH_LONG).show()
                else editingId = NEW_ITEM_ID
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Default.Add, stringResource(R.string.add_item)) }
    }

    val editing = when (val id = editingId) {
        null -> null
        NEW_ITEM_ID -> categories.firstOrNull()?.let { Item(name = "", categoryId = it.id) }
        else -> items.find { it.id == id }
    }
    editing?.let { item ->
        ItemDialog(
            initial = item,
            categories = categories,
            onDismiss = { editingId = null },
            onSave = { scope.launchWrite(context) { dao.upsertItem(it) }; editingId = null },
            onDelete = { scope.launchWrite(context) { dao.deleteItem(item) }; editingId = null },
        )
    }
}
