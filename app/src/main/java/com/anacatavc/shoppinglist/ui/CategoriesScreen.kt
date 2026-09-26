package com.anacatavc.shoppinglist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anacatavc.shoppinglist.R
import com.anacatavc.shoppinglist.data.Category
import com.anacatavc.shoppinglist.data.ShoppingDao
import kotlinx.coroutines.launch

/** Pending deletion of a category that still has [itemCount] items attached. */
private data class DeleteRequest(val category: Category, val itemCount: Int)

@Composable
fun CategoriesScreen(categories: List<Category>, dao: ShoppingDao, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Category?>(null) }
    var deleting by remember { mutableStateOf<DeleteRequest?>(null) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(categories, key = { it.id }) { category ->
                ListItem(
                    modifier = Modifier.clickable { editing = category },
                    leadingContent = { Text(category.emoji, style = MaterialTheme.typography.headlineSmall) },
                    headlineContent = { Text(category.name) },
                    trailingContent = {
                        IconButton(onClick = {
                            scope.launch { deleting = DeleteRequest(category, dao.countItemsIn(category.id)) }
                        }) { Icon(Icons.Default.Delete, stringResource(R.string.delete_category_cd, category.name)) }
                    },
                )
            }
        }
        FloatingActionButton(
            onClick = { editing = Category(name = "", emoji = "🛍️") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Default.Add, stringResource(R.string.add_category)) }
    }

    editing?.let { category ->
        CategoryDialog(
            initial = category,
            takenNames = categories.filter { it.id != category.id }.map { it.name.lowercase() }.toSet(),
            onDismiss = { editing = null },
            onSave = { scope.launch { dao.upsertCategory(it) }; editing = null },
        )
    }

    deleting?.let { request ->
        DeleteCategoryDialog(
            request = request,
            otherCategories = categories.filter { it.id != request.category.id },
            onDismiss = { deleting = null },
            onConfirm = { targetId ->
                scope.launch {
                    if (targetId == null) dao.deleteCategoryById(request.category.id)
                    else dao.moveItemsAndDeleteCategory(request.category.id, targetId)
                }
                deleting = null
            },
        )
    }
}

@Composable
private fun CategoryDialog(initial: Category, takenNames: Set<String>, onDismiss: () -> Unit, onSave: (Category) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var emoji by rememberSaveable { mutableStateOf(initial.emoji) }
    val duplicate = name.trim().lowercase() in takenNames

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id == 0L) R.string.category_new else R.string.category_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    emoji,
                    { emoji = it.take(8) },
                    label = { Text(stringResource(R.string.category_emoji)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    name, { name = it },
                    label = { Text(stringResource(R.string.category_name)) },
                    singleLine = true,
                    isError = duplicate,
                    supportingText = if (duplicate) { { Text(stringResource(R.string.category_duplicate)) } } else null,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && !duplicate,
                onClick = { onSave(initial.copy(name = name.trim(), emoji = emoji.trim().ifEmpty { "📦" })) },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * An empty category is deleted after a plain confirmation. A category with items can only be
 * deleted by first choosing where its items go.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeleteCategoryDialog(
    request: DeleteRequest,
    otherCategories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (targetId: Long?) -> Unit,
) {
    var targetId by remember { mutableStateOf<Long?>(null) }
    val hasItems = request.itemCount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_delete_title, "${request.category.emoji} ${request.category.name}")) },
        text = {
            when {
                !hasItems -> Text(stringResource(R.string.category_delete_empty))
                otherCategories.isEmpty() -> Text(pluralStringResource(R.plurals.category_delete_no_target, request.itemCount, request.itemCount))
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(pluralStringResource(R.plurals.category_delete_move, request.itemCount, request.itemCount))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        otherCategories.forEach {
                            FilterChip(
                                selected = targetId == it.id,
                                onClick = { targetId = it.id },
                                label = { Text("${it.emoji} ${it.name}") },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !hasItems || targetId != null,
                onClick = { onConfirm(if (hasItems) targetId else null) },
            ) { Text(stringResource(if (hasItems) R.string.category_move_and_delete else R.string.delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
