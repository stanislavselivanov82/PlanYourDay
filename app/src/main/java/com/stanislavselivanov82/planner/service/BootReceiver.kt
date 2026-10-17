package com.stanislavselivanov82.planner.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED")) return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { withTimeout(8500) { scheduler.rescheduleAll() } }
            catch (error: Exception) { Log.e("BootReceiver", "Unable to restore reminders", error) }
            finally { result.finish() }
        }
    }
}
