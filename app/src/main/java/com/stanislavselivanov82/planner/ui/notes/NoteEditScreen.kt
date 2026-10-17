package com.stanislavselivanov82.planner.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stanislavselivanov82.planner.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(onBack: () -> Unit, viewModel: NoteEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel) { viewModel.closed.collect { onBack() } }
    DisposableEffect(owner, viewModel) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) viewModel.flush() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    BackHandler { viewModel.close() }
    val enabled = !state.loading && !state.closing
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.note_edit)) }, navigationIcon = {
        TextButton(enabled = enabled, onClick = viewModel::close) { Text(stringResource(R.string.back)) }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading) CircularProgressIndicator()
            Text(stringResource(when { state.error -> R.string.notes_error; state.saving -> R.string.note_saving
                state.dirty -> R.string.note_unsaved; else -> R.string.note_saved }),
                color = if (state.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.error) TextButton(onClick = viewModel::flush) { Text(stringResource(R.string.note_retry)) }
            OutlinedTextField(state.draft.title, { viewModel.edit(state.draft.copy(title = it)) }, enabled = enabled,
                label = { Text(stringResource(R.string.note_title)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.draft.content, { viewModel.edit(state.draft.copy(content = it)) }, enabled = enabled,
                label = { Text(stringResource(R.string.note_content)) }, modifier = Modifier.fillMaxWidth(), minLines = 10)
            NoteColorChips(state.draft.color, stringResource(R.string.note_no_color),
                { viewModel.edit(state.draft.copy(color = it)) }, enabled)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(state.draft.pinned, { viewModel.edit(state.draft.copy(pinned = it)) }, enabled = enabled)
                Text(stringResource(R.string.note_pin))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(enabled = enabled, onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete)) }
                Button(enabled = enabled, onClick = viewModel::close) { Text(stringResource(R.string.note_done)) }
            }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text(stringResource(R.string.note_delete_confirm)) },
        confirmButton = { TextButton(enabled = enabled, onClick = viewModel::delete) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } })
}
