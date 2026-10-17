package com.stanislavselivanov82.planner.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.stanislavselivanov82.planner.data.local.db.PlannerDatabase
import com.stanislavselivanov82.planner.data.local.db.entity.toDomain
import com.stanislavselivanov82.planner.domain.model.*
import com.stanislavselivanov82.planner.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PlannerDatabase,
    private val settings: SettingsRepository,
) {
    private val alarm get() = context.getSystemService(AlarmManager::class.java)
    private val mutex = Mutex()
    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()
    private fun intent(id: Long) = Intent(context, ReminderReceiver::class.java)
        .setAction("com.stanislavselivanov82.planner.REMIND_TASK")
        .setData(Uri.parse("planyourday://task-reminder/$id"))

    private fun cancel(id: Long) {
        val pending = PendingIntent.getBroadcast(context, 0, intent(id), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pending != null) { alarm.cancel(pending); pending.cancel() }
        NotificationManagerCompat.from(context).cancel("task:$id", 1)
    }
    private fun schedule(task: Task, prefs: PlannerSettings) {
        cancel(task.id)
        if (!task.hasActiveReminder()) return
        val trigger = reminderTimeMillis(task, prefs.zoneId)
        if (trigger <= System.currentTimeMillis() || !canScheduleExact()) return
        val pending = PendingIntent.getBroadcast(context, 0,
            intent(task.id).putExtra("taskId", task.id).putExtra("updatedAt", task.updatedAt).putExtra("trigger", trigger),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try { alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending) }
        catch (error: SecurityException) { pending.cancel(); Log.w("ReminderScheduler", "Exact alarm access was revoked", error) }
    }
    suspend fun refresh(id: Long) = mutex.withLock {
        val task = database.taskDao().getById(id)?.toDomain()
        if (task == null) cancel(id) else schedule(task, settings.settings.first())
    }
    suspend fun rescheduleAll() = mutex.withLock {
        val prefs = settings.settings.first()
        database.taskDao().getActiveReminders().forEach { schedule(it.toDomain(), prefs) }
    }
    /** Prevent an already-queued broadcast from notifying an edited/completed/deleted task. */
    suspend fun deliver(id: Long, expectedUpdatedAt: Long, expectedTrigger: Long, notify: (Task, PlannerSettings) -> Unit) = mutex.withLock {
        val task = database.taskDao().getById(id)?.toDomain() ?: return@withLock
        val prefs = settings.settings.first()
        if (!task.hasActiveReminder() || task.updatedAt != expectedUpdatedAt || reminderTimeMillis(task, prefs.zoneId) != expectedTrigger) return@withLock
        if (expectedTrigger > System.currentTimeMillis() + 1000) return@withLock
        notify(task, prefs)
    }
}
