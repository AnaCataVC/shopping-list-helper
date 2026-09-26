package com.anacatavc.shoppinglist.ui

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anacatavc.shoppinglist.R
import com.anacatavc.shoppinglist.data.Category
import com.anacatavc.shoppinglist.data.Item
import com.anacatavc.shoppinglist.data.ShoppingDao
import kotlinx.coroutines.launch

/** Everything not yet bought, grouped by category and sorted by urgency. */
@Composable
fun PendingScreen(items: List<Item>, categories: List<Category>, dao: ShoppingDao, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val now = System.currentTimeMillis()
    // null = closed, Item(id = 0) = new item.
    var editing by remember { mutableStateOf<Item?>(null) }
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
                        "${category.emoji} ${category.name}",
                        Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                val sorted = pendingByCategory.getValue(category.id).sortedWith(compareBy({ !it.isDue(now) }, { it.urgency }))
                items(sorted, key = { it.id }) { item -> ItemRow(item, now, onClick = { editing = item }) }
            }
        }
        FloatingActionButton(
            onClick = { categories.firstOrNull()?.let { editing = Item(name = "", categoryId = it.id) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Default.Add, stringResource(R.string.add_item)) }
    }

    editing?.let { item ->
        ItemDialog(
            initial = item,
            categories = categories,
            onDismiss = { editing = null },
            onSave = { scope.launch { dao.upsertItem(it) }; editing = null },
            onDelete = { scope.launch { dao.deleteItem(item) }; editing = null },
        )
    }
}
