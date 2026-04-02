package com.example.recepy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

object RecipeTimerManager {
    val timeRemaining = MutableStateFlow(0)
    val isRunning = MutableStateFlow(false)
    private var timerJob: Job? = null

    private var appContext: Context? = null

    fun startTimer(context: Context, seconds: Int, title: String) {
        appContext = context.applicationContext
        timerJob?.cancel()
        timeRemaining.value = seconds
        isRunning.value = true

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel("timer_channel", "טיימר מתכונים", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        // במקביל מפעילים את ה-Service שלנו שיצייר את הבועה וישמור על התהליך חי
        val intent = Intent(context, TimerService::class.java).apply {
            action = "START_TIMER"
            putExtra("SECONDS", seconds)
            putExtra("TITLE", title)
        }
        context.startForegroundService(intent)

        timerJob = CoroutineScope(Dispatchers.IO).launch {
            while (timeRemaining.value > 0 && isRunning.value) {
                delay(1000)
                timeRemaining.value -= 1
            }
            if (isRunning.value) {
                isRunning.value = false
                try {
                    val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
                    ringtone.play()

                    val notification = NotificationCompat.Builder(context, "timer_channel")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("הטיימר הסתיים!")
                        .setContentText("הזמן של $title עבר.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build()
                    notificationManager.notify(1001, notification)

                    delay(8000)
                    ringtone.stop()
                } catch (_: Exception) {
                    // Error playing ringtone or showing notification
                }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        isRunning.value = false
        timeRemaining.value = 0

        appContext?.let { context ->
            val intent = Intent(context, TimerService::class.java).apply {
                action = "STOP_TIMER"
            }
            context.startService(intent)
        }
    }
}