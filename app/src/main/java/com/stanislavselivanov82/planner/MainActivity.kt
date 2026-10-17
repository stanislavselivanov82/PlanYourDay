package com.stanislavselivanov82.planner

import android.os.Bundle
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.lifecycleScope
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stanislavselivanov82.planner.domain.model.AppTheme
import com.stanislavselivanov82.planner.ui.navigation.PlannerNavHost
import com.stanislavselivanov82.planner.ui.settings.SettingsViewModel
import com.stanislavselivanov82.planner.ui.theme.PlanYourDayTheme
import com.stanislavselivanov82.planner.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @javax.inject.Inject lateinit var scheduler: com.stanislavselivanov82.planner.service.ReminderScheduler
    private val openDay = androidx.compose.runtime.mutableStateOf<Long?>(null)
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasExtra("openDay")) openDay.value = intent.getLongExtra("openDay", 0)
    }
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try { scheduler.rescheduleAll() }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                android.util.Log.e("MainActivity", "Unable to restore task reminders", error)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent.hasExtra("openDay")) openDay.value = intent.getLongExtra("openDay", 0)
        setContent {
            val viewModel: SettingsViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val theme = when (settings.theme) {
                AppTheme.DARK -> ThemeMode.DARK
                AppTheme.LIGHT -> ThemeMode.LIGHT
                AppTheme.SYSTEM -> ThemeMode.SYSTEM
            }
            PlanYourDayTheme(mode = theme) {
                PlannerNavHost(openDay = openDay.value, onDayConsumed = { openDay.value = null })
                com.stanislavselivanov82.planner.ui.components.ReminderPermissions(requestOnEntry = true)
            }
        }
    }
}
