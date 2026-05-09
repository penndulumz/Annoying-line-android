package com.annoyingline.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.annoyingline.app.data.PreferenceManager
import com.annoyingline.app.service.OverlayService

class ServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.annoyingline.app.ACTION_RESTART_SERVICE") {
            val prefs = PreferenceManager(context)
            if (!prefs.isOverlayEnabled) return

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val serviceIntent = Intent(context, OverlayService::class.java)
            
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            // Using getForegroundService on O+ for robust restart
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(context, 100, serviceIntent, flags)
            } else {
                PendingIntent.getService(context, 100, serviceIntent, flags)
            }
            
            // Schedule restart for 5 seconds in the future
            val triggerTime = System.currentTimeMillis() + 5000
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    am.setWindow(AlarmManager.RTC_WAKEUP, triggerTime, 1000, pendingIntent)
                }
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }
}
