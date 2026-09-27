package com.anacatavc.shoppinglist.ui

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import com.anacatavc.shoppinglist.R
import com.anacatavc.shoppinglist.data.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "ShoppingList"

/**
 * Runs a database write without crashing the app: a constraint violation (e.g. a category deleted
 * or renamed meanwhile) is reported with a toast and the write is dropped.
 */
fun CoroutineScope.launchWrite(context: Context, write: suspend () -> Unit) = launch {
    try {
        write()
    } catch (e: SQLiteException) {
        Log.w(TAG, "Database write rejected", e)
        Toast.makeText(context, R.string.save_failed, Toast.LENGTH_LONG).show()
    }
}

/** Current time, refreshed every minute so due items appear without waiting for another recomposition. */
@Composable
fun rememberNow(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        delay(60_000)
        value = System.currentTimeMillis()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    label: (Category) -> String = { it.label },
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        categories.forEach {
            FilterChip(selected = selectedId == it.id, onClick = { onSelect(it.id) }, label = { Text(label(it)) })
        }
    }
}
