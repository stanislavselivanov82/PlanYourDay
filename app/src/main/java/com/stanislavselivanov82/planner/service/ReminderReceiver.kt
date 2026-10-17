package com.stanislavselivanov82.planner.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.stanislavselivanov82.planner.MainActivity
import com.stanislavselivanov82.planner.R
import com.stanislavselivanov82.planner.domain.model.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*
import java.security.MessageDigest
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("taskId", -1)
        if (id < 1) return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { withTimeout(8500) {
                scheduler.deliver(id, intent.getLongExtra("updatedAt", -1), intent.getLongExtra("trigger", -1)) { task, prefs ->
                    show(context, task, prefs)
                }
            } } catch (error: Exception) { Log.e("ReminderReceiver", "Unable to deliver task reminder", error) }
            finally { result.finish() }
        }
    }
    private fun show(context: Context, task: Task, prefs: PlannerSettings) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val isAlarm = task.reminderType == ReminderType.ALARM
        val sound = prefs.reminderSoundUri?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(if (isAlarm) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION)
        // Android channels have immutable sound settings; a sound-specific channel respects subsequent changes.
        val digest = MessageDigest.getInstance("SHA-256").digest(sound.toString().toByteArray()).take(6).joinToString("") { "%02x".format(it) }
        val channelId = (if (isAlarm) "alarms_" else "reminders_") + digest
        val channel = NotificationChannel(channelId, context.getString(if (isAlarm) R.string.channel_alarms else R.string.channel_reminders), NotificationManager.IMPORTANCE_HIGH)
        channel.setSound(sound, AudioAttributes.Builder().setUsage(if (isAlarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION).build())
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val open = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("planyourday://day/${task.date}"))
            .putExtra("openDay", task.date).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val time = LocalTime.of(task.startMinutes / 60, task.startMinutes % 60).format(DateTimeFormatter.ofPattern("HH:mm"))
        val text = context.getString(R.string.reminder_notification_text, time, task.title)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(task.title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text + task.description?.let { "\n$it" }.orEmpty()))
            .setContentIntent(pending).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .build()
        try { manager.notify("task:${task.id}", 1, notification) } catch (error: SecurityException) { Log.w("ReminderReceiver", "Notification permission revoked", error) }
    }
}
