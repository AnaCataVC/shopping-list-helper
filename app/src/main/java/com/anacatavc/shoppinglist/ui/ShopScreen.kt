package com.anacatavc.shoppinglist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anacatavc.shoppinglist.R
import com.anacatavc.shoppinglist.data.Category
import com.anacatavc.shoppinglist.data.Item
import com.anacatavc.shoppinglist.data.ShoppingDao

/** "Voy a comprar a X": the due items of one category, bought one by one or all at once. */
@Composable
fun ShopScreen(items: List<Item>, categories: List<Category>, dao: ShoppingDao, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val now by rememberNow()
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val dueCount = items.filter { it.isDue(now) }.groupingBy { it.categoryId }.eachCount()

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.shop_question), Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
        CategoryChips(categories, categoryId, onSelect = { categoryId = it }, label = { "${it.label} (${dueCount[it.id] ?: 0})" })

        val selected = categoryId ?: return@Column
        val shoppingList = items.filter { it.categoryId == selected && it.isDue(now) }.sortedBy { it.urgency }
        if (shoppingList.isEmpty()) {
            Text(stringResource(R.string.shop_empty), Modifier.padding(top = 24.dp))
            return@Column
        }
        LazyColumn(Modifier.weight(1f)) {
            items(shoppingList, key = { it.id }) { item ->
                val buy = { scope.launchWrite(context) { dao.upsertItem(item.bought(System.currentTimeMillis())) } }
                ItemRow(item, now, onClick = { buy() }, leading = { Checkbox(checked = false, onCheckedChange = { buy() }) })
            }
        }
        Button(
            onClick = {
                scope.launchWrite(context) {
                    val boughtAt = System.currentTimeMillis()
                    dao.upsertItems(shoppingList.map { it.bought(boughtAt) })
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        ) { Text(stringResource(R.string.shop_mark_all)) }
    }
}
