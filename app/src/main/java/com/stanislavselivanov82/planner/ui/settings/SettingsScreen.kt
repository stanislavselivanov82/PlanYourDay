package com.stanislavselivanov82.planner.ui.settings

import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stanislavselivanov82.planner.BuildConfig
import com.stanislavselivanov82.planner.R
import com.stanislavselivanov82.planner.domain.model.AppTheme
import java.time.ZoneId
import java.util.Locale

@Composable
fun SettingsScreen(onCategories: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val errorText = stringResource(R.string.settings_error)
    val invalidSleep = stringResource(R.string.sleep_invalid)
    var sleepError by remember { mutableStateOf(false) }
    var zoneDialog by rememberSaveable { mutableStateOf(false) }
    var zoneInput by rememberSaveable { mutableStateOf("") }
    val zoneValid = zoneInput.isBlank() || zoneInput.trim() in ZoneId.getAvailableZoneIds()
    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val uri = if (Build.VERSION.SDK_INT >= 33) {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            viewModel.setReminderSound(uri?.toString())
        }
    }
    LaunchedEffect(viewModel) { viewModel.errors.collect { snackbar.showSnackbar(errorText) } }
    LaunchedEffect(sleepError) {
        if (sleepError) { snackbar.showSnackbar(invalidSleep); sleepError = false }
    }
    fun chooseTime(start: Boolean) {
        val initial = if (start) settings.sleepStart else settings.sleepEnd
        TimePickerDialog(context, { _, hours, minutes ->
            val value = hours * 60 + minutes
            val from = if (start) value else settings.sleepStart
            val to = if (start) settings.sleepEnd else value
            if (from == to) sleepError = true else viewModel.setSleep(from, to)
        }, initial / 60, initial % 60, true).show()
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.more_title), style = MaterialTheme.typography.headlineMedium)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.sleep_title), style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { chooseTime(true) }) {
                            Text(stringResource(R.string.sleep_start_value, clock(settings.sleepStart)))
                        }
                        TextButton(onClick = { chooseTime(false) }) {
                            Text(stringResource(R.string.sleep_end_value, clock(settings.sleepEnd)))
                        }
                    }
                    Text(stringResource(R.string.sleep_totals, settings.sleepMinutes / 60, settings.sleepMinutes % 60,
                        settings.wakingMinutes / 60, settings.wakingMinutes % 60))
                }
            }
            TextButton(onClick = onCategories) { Text(stringResource(R.string.categories_title)) }
            Text(stringResource(R.string.theme_title), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(AppTheme.DARK to R.string.theme_dark, AppTheme.LIGHT to R.string.theme_light,
                    AppTheme.SYSTEM to R.string.theme_system).forEach { (theme, label) ->
                    FilterChip(selected = settings.theme == theme, onClick = { viewModel.setTheme(theme) },
                        label = { Text(stringResource(label)) })
                }
            }
            HorizontalDivider()
            TextButton(onClick = {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        settings.reminderSoundUri?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                }
                if (intent.resolveActivity(context.packageManager) != null) soundPicker.launch(intent)
                else viewModel.setReminderSound(null)
            }) { Text(stringResource(R.string.reminder_sound)) }
            TextButton(onClick = { zoneInput = settings.timeZoneId.orEmpty(); zoneDialog = true }) {
                Text(stringResource(R.string.timezone_value, settings.zoneId.id))
            }
            Text(stringResource(R.string.local_data_only), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.export_later), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.import_later), color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
        }
    }
    if (zoneDialog) {
        AlertDialog(
            onDismissRequest = { zoneDialog = false },
            title = { Text(stringResource(R.string.timezone_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.timezone_hint))
                    OutlinedTextField(value = zoneInput, onValueChange = { zoneInput = it }, singleLine = true,
                        isError = !zoneValid, label = { Text(stringResource(R.string.timezone_id)) })
                }
            },
            confirmButton = {
                TextButton(enabled = zoneValid, onClick = {
                    viewModel.setTimeZone(zoneInput.trim().takeIf { it.isNotEmpty() }); zoneDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { zoneDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

private fun clock(minutes: Int): String = String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)
