package com.stanislavselivanov82.planner.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stanislavselivanov82.planner.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onBack: () -> Unit, viewModel: CategoriesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableLongStateOf(0L) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CategoryEvent.Saved -> {
                    editing = false
                    snackbar.showSnackbar(context.getString(R.string.category_saved))
                }
                is CategoryEvent.Deleted -> {
                    editing = false
                    snackbar.showSnackbar(context.getString(R.string.category_deleted, event.name, event.count))
                }
                CategoryEvent.DeleteRejected -> snackbar.showSnackbar(context.getString(R.string.category_last))
                CategoryEvent.Error -> snackbar.showSnackbar(context.getString(R.string.category_error))
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.categories_title)) }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            })
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.failed -> Box(Modifier.fillMaxSize().padding(padding).padding(18.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.category_error))
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.rows, key = { it.category.id }) { row ->
                    Card(onClick = { editId = row.category.id; editing = true }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(24.dp).background(categoryColor(row.category.colorHex), CircleShape))
                            Column(Modifier.weight(1f)) {
                                Text(row.category.name, style = MaterialTheme.typography.titleMedium)
                                Text(pluralStringResource(R.plurals.category_task_count, row.taskCount, row.taskCount),
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            if (row.category.isSystem) Text(
                                stringResource(R.string.category_system),
                                modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                item {
                    Button(onClick = { editId = 0; editing = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.category_add))
                    }
                }
            }
        }
    }
    val row = state.rows.firstOrNull { it.category.id == editId }
    if (editing && !state.loading && (editId == 0L || row != null)) {
        CategoryEditSheet(
            row = row, canDelete = state.rows.size > 1, busy = busy,
            onDismiss = { editing = false },
            onSave = { name, color -> viewModel.save(editId, name, color) },
            onDelete = { viewModel.delete(editId) },
        )
    }
}
