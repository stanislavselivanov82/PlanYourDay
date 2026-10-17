package com.stanislavselivanov82.planner.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.stanislavselivanov82.planner.R

internal val CategoryPalette = listOf(
    "#ff5c5c", "#ff8a3d", "#ffb84d", "#ffd84d", "#a3e055", "#4dd68a",
    "#4dd6c7", "#5db6ff", "#6c8cff", "#b48cff", "#ff6fd8", "#9aa0ac",
)

internal fun categoryColor(hex: String): Color = Color(android.graphics.Color.parseColor(hex))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryEditSheet(
    row: CategoryRow?,
    canDelete: Boolean,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
) {
    val id = row?.category?.id ?: 0L
    var name by rememberSaveable(id) { mutableStateOf(row?.category?.name.orEmpty()) }
    var color by rememberSaveable(id) { mutableStateOf(row?.category?.colorHex ?: CategoryPalette.first()) }
    var confirmDelete by rememberSaveable(id) { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(if (row == null) R.string.category_add else R.string.category_edit),
                style = MaterialTheme.typography.headlineSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(32.dp).background(categoryColor(color), CircleShape))
                Column {
                    Text(name.ifBlank { stringResource(R.string.category_preview) }, style = MaterialTheme.typography.titleMedium)
                    val count = row?.taskCount ?: 0
                    Text(pluralStringResource(R.plurals.category_task_count, count, count))
                }
            }
            OutlinedTextField(
                value = name, onValueChange = { name = it }, enabled = !busy,
                label = { Text(stringResource(R.string.category_name)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.category_color), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CategoryPalette.forEach { hex ->
                    val chosen = hex.equals(color, ignoreCase = true)
                    val label = stringResource(R.string.category_color_value, hex)
                    Box(
                        Modifier.size(48.dp).semantics { contentDescription = label; selected = chosen }
                            .clickable(enabled = !busy, role = Role.RadioButton) { color = hex }
                            .padding(5.dp)
                            .border(if (chosen) 3.dp else 0.dp,
                                if (chosen) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                            .padding(4.dp).background(categoryColor(hex), CircleShape),
                    )
                }
            }
            if (row != null) {
                TextButton(enabled = canDelete && !busy, onClick = { confirmDelete = true }) {
                    Text(stringResource(R.string.category_delete), color = if (canDelete && !busy)
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!canDelete) Text(stringResource(R.string.category_last), style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Button(enabled = name.isNotBlank() && !busy, onClick = { onSave(name.trim(), color) }) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
    if (confirmDelete && row != null) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirmDelete = false },
            title = { Text(stringResource(R.string.category_delete_confirm, row.category.name)) },
            text = { Text(stringResource(R.string.category_delete_explanation, row.taskCount)) },
            confirmButton = {
                TextButton(enabled = !busy && canDelete, onClick = onDelete) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
