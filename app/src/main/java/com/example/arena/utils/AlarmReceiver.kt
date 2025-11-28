package com.example.arena.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("TITLE") ?: "ARENA ALERT"
        val message = intent.getStringExtra("MESSAGE") ?: "Your deadline is approaching!"

        NotificationHelper.createNotificationChannel(context)
        NotificationHelper.showNotification(context, title, message)
    }
}