package com.anacatavc.shoppinglist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.anacatavc.shoppinglist.R
import com.anacatavc.shoppinglist.data.Category
import com.anacatavc.shoppinglist.data.Item
import com.anacatavc.shoppinglist.data.Urgency

/** Create (id = 0) or edit an item. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemDialog(
    initial: Item,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Item) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var quantity by rememberSaveable { mutableStateOf(initial.quantity) }
    var note by rememberSaveable { mutableStateOf(initial.note) }
    var categoryId by rememberSaveable { mutableStateOf(initial.categoryId) }
    var urgency by rememberSaveable { mutableStateOf(initial.urgency) }
    var recurrence by rememberSaveable { mutableStateOf(initial.recurrenceDays?.toString() ?: "") }
    val recurrenceDays = recurrence.toIntOrNull()?.takeIf { it > 0 }
    val recurrenceValid = recurrence.isBlank() || recurrenceDays != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id == 0L) R.string.item_new else R.string.item_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.item_name)) }, singleLine = true)
                OutlinedTextField(quantity, { quantity = it }, label = { Text(stringResource(R.string.item_quantity)) }, singleLine = true)
                Text(stringResource(R.string.item_category), style = MaterialTheme.typography.labelLarge)
                CategoryChips(categories, categoryId, onSelect = { categoryId = it })
                Text(stringResource(R.string.item_urgency), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Urgency.entries.forEach {
                        FilterChip(selected = urgency == it, onClick = { urgency = it }, label = { Text(stringResource(it.label)) })
                    }
                }
                OutlinedTextField(
                    recurrence,
                    { recurrence = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.item_recurrence)) },
                    singleLine = true,
                    isError = !recurrenceValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.item_note)) })
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && recurrenceValid,
                onClick = {
                    onSave(
                        initial.copy(
                            name = name.trim(),
                            quantity = quantity.trim(),
                            note = note.trim(),
                            categoryId = categoryId,
                            urgency = urgency,
                        ).withRecurrence(recurrenceDays),
                    )
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            if (initial.id != 0L) {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
