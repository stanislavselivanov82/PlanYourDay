package com.stanislavselivanov82.planner.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stanislavselivanov82.planner.R
import com.stanislavselivanov82.planner.domain.model.NoteColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun noteColor(tag: String?): Color = when(tag) {
    "red" -> Color(0xFFFF5C5C); "orange" -> Color(0xFFFF8A3D); "yellow" -> Color(0xFFFFD84D)
    "green" -> Color(0xFF4DD68A); "blue" -> Color(0xFF5DB6FF); "purple" -> Color(0xFFB48CFF)
    else -> Color(0xFF9AA0AC)
}
@Composable internal fun colorName(tag: String): String = stringResource(when(tag) {
    "red" -> R.string.note_red; "orange" -> R.string.note_orange; "yellow" -> R.string.note_yellow
    "green" -> R.string.note_green; "blue" -> R.string.note_blue; else -> R.string.note_purple
})
@Composable
internal fun NoteColorChips(selected: String?, noneLabel: String, onSelect: (String?) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected == null, { onSelect(null) }, enabled = enabled, label = { Text(noneLabel) })
        NoteColors.tags.forEach { tag ->
            FilterChip(selected == tag, { onSelect(tag) }, enabled = enabled,
                leadingIcon = { Box(Modifier.size(12.dp).background(noteColor(tag), CircleShape)) }, label = { Text(colorName(tag)) })
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(onOpenNote: (Long) -> Unit, viewModel: NotesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val error = stringResource(R.string.notes_error)
    LaunchedEffect(viewModel) { viewModel.errors.collect { snackbar.showSnackbar(error) } }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.notes_count, state.total)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { FloatingActionButton(onClick = { onOpenNote(0) }) { Icon(Icons.Default.Add, stringResource(R.string.note_new)) } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { OutlinedTextField(state.query, viewModel::search, modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.notes_search)) }) }
            item { NoteColorChips(state.color, stringResource(R.string.notes_all), viewModel::filter) }
            if (state.loading) item { CircularProgressIndicator() }
            if (state.failed) item { Text(error, color = MaterialTheme.colorScheme.error) }
            if (!state.loading && !state.failed && state.notes.isEmpty()) item { Text(stringResource(R.string.notes_empty)) }
            items(state.notes, key = { it.id }) { note ->
                Card(onClick = { onOpenNote(note.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(4.dp).fillMaxHeight().background(noteColor(note.colorTag)))
                        Column(Modifier.weight(1f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(note.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.note_untitled), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text(note.content, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                            Text(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("ru"))
                                .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(note.updatedAt)), style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { viewModel.pin(note) }) { Text(stringResource(if (note.isPinned) R.string.note_unpin else R.string.note_pin)) }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
