package com.anacatavc.shoppinglist.ui

import android.icu.text.BreakIterator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anacatavc.shoppinglist.R
import com.anacatavc.shoppinglist.data.Category
import com.anacatavc.shoppinglist.data.Item
import com.anacatavc.shoppinglist.data.ShoppingDao

private const val NEW_CATEGORY_ID = 0L
private const val MAX_EMOJI_GRAPHEMES = 2

@Composable
fun CategoriesScreen(categories: List<Category>, items: List<Item>, dao: ShoppingDao, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Only ids are saved so the dialogs survive rotation; NEW_CATEGORY_ID = new category.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(categories, key = { it.id }) { category ->
                ListItem(
                    modifier = Modifier.clickable { editingId = category.id },
                    leadingContent = { Text(category.emoji, style = MaterialTheme.typography.headlineSmall) },
                    headlineContent = { Text(category.name) },
                    trailingContent = {
                        IconButton(onClick = { deletingId = category.id }) { Icon(Icons.Default.Delete, stringResource(R.string.delete_category_cd, category.name)) }
                    },
                )
            }
        }
        FloatingActionButton(
            onClick = { editingId = NEW_CATEGORY_ID },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Default.Add, stringResource(R.string.add_category)) }
    }

    val editing = when (val id = editingId) {
        null -> null
        NEW_CATEGORY_ID -> Category(name = "", emoji = "🛍️")
        else -> categories.find { it.id == id }
    }
    editing?.let { category ->
        CategoryDialog(
            initial = category,
            takenNames = categories.filter { it.id != category.id }.map { it.name.lowercase() }.toSet(),
            onDismiss = { editingId = null },
            onSave = { scope.launchWrite(context) { dao.upsertCategory(it) }; editingId = null },
        )
    }

    categories.find { it.id == deletingId }?.let { category ->
        DeleteCategoryDialog(
            category = category,
            // Counted live, so an item added meanwhile switches the dialog to "move items first".
            itemCount = items.count { it.categoryId == category.id },
            otherCategories = categories.filter { it.id != category.id },
            onDismiss = { deletingId = null },
            onConfirm = { targetId ->
                // An empty category is deleted directly; RESTRICT rejects it if an item slipped in.
                scope.launchWrite(context) {
                    if (targetId == null) dao.deleteCategoryById(category.id)
                    else dao.moveItemsAndDeleteCategory(category.id, targetId)
                }
                deletingId = null
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
                    { emoji = it.takeGraphemes(MAX_EMOJI_GRAPHEMES) },
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

/** Truncates by user-perceived character, so multi-codepoint emoji are never split. */
private fun String.takeGraphemes(max: Int): String {
    val boundaries = BreakIterator.getCharacterInstance().also { it.setText(this) }
    val end = boundaries.next(max)
    return if (end == BreakIterator.DONE) this else substring(0, end)
}

/**
 * An empty category is deleted after a plain confirmation. A category with items can only be
 * deleted by first choosing where its items go.
 */
@Composable
private fun DeleteCategoryDialog(
    category: Category,
    itemCount: Int,
    otherCategories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (targetId: Long?) -> Unit,
) {
    var targetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val hasItems = itemCount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_delete_title, category.label)) },
        text = {
            when {
                !hasItems -> Text(stringResource(R.string.category_delete_empty))
                otherCategories.isEmpty() -> Text(pluralStringResource(R.plurals.category_delete_no_target, itemCount, itemCount))
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(pluralStringResource(R.plurals.category_delete_move, itemCount, itemCount))
                    CategoryChips(otherCategories, targetId, onSelect = { targetId = it })
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
