package com.xinxu.mood

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import com.xinxu.mood.data.Settings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = (context.applicationContext as MoodApplication).settings.flow.first()
                if (settings.reminder && intent.action != Intent.ACTION_BOOT_COMPLETED && (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)) {
                    val manager = context.getSystemService(NotificationManager::class.java)
                    manager.createNotificationChannel(NotificationChannel("daily", "每日关怀", NotificationManager.IMPORTANCE_DEFAULT))
                    val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    manager.notify(1, Notification.Builder(context, "daily").setSmallIcon(com.xinxu.mood.R.drawable.ic_mood).setContentTitle("留一点时间给自己").setContentText("今天的你，感觉怎么样？").setContentIntent(open).setAutoCancel(true).build())
                }
                schedule(context, settings)
            } finally { pending.finish() }
        }
    }
    companion object {
        fun schedule(context: Context, settings: Settings) {
            val alarm = context.getSystemService(AlarmManager::class.java)
            val pending = PendingIntent.getBroadcast(context, 2, Intent(context, ReminderReceiver::class.java).setAction("com.xinxu.mood.REMIND"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            alarm.cancel(pending)
            if (settings.reminder) {
                val now = ZonedDateTime.now(); var next = now.withHour(settings.hour).withMinute(settings.minute).withSecond(0).withNano(0)
                if (!next.isAfter(now)) next = next.plusDays(1)
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), pending)
            }
        }
    }
}
