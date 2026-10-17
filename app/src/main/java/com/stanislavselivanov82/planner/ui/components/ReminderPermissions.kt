package com.stanislavselivanov82.planner.ui.components

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stanislavselivanov82.planner.R

@Composable
fun ReminderPermissions(requestOnEntry: Boolean = false) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var refresh by remember { mutableIntStateOf(0) }
    var dismissed by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh++ }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    val notifications = remember(refresh) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val exact = remember(refresh) { Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() }
    fun openExact() {
        if (Build.VERSION.SDK_INT >= 31) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
            if (intent.resolveActivity(context.packageManager) != null) settingsLauncher.launch(intent)
        }
    }
    if (requestOnEntry) LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("permission_requests", 0)
        if (!prefs.getBoolean("notificationsAsked", false) && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            prefs.edit().putBoolean("notificationsAsked", true).apply()
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // Exact-alarm access is special settings access, not a runtime permission dialog.
    if ((!notifications || !exact) && !dismissed) AlertDialog(onDismissRequest = { dismissed = true },
        title = { Text(stringResource(R.string.reminder_permissions)) },
        text = { Column {
            Text(stringResource(R.string.reminder_permissions_hint))
            if (!notifications) TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else settingsLauncher.launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }) { Text(stringResource(R.string.allow_notifications)) }
            if (!exact) TextButton(onClick = { openExact() }) { Text(stringResource(R.string.allow_exact)) }
        } },
        confirmButton = { TextButton(onClick = { dismissed = true }) { Text(stringResource(R.string.reminder_later)) } })
}
