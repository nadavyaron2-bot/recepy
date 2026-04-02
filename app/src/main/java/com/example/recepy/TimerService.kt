package com.example.recepy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

class TimerService : Service() {

    private val channelId = "RecipeTimerChannel"
    private val notificationId = 1

    private var timerJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    // WindowManager לבועה הצפה
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var bubbleTimeText: TextView? = null

    private var currentTitle = ""

    override fun onCreate() {
        super.onCreate()
        currentTitle = getString(R.string.default_timer_title)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_TIMER" -> {
                val seconds = intent.getIntExtra("SECONDS", 0)
                currentTitle = intent.getStringExtra("TITLE") ?: getString(R.string.default_timer_title)
                startTimer(seconds)
            }
            "STOP_TIMER" -> {
                stopEverything()
            }
            "STOP_ALARM" -> {
                stopAlarmAndRemoveBubble()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Int) {
        timerJob?.cancel()
        RecipeTimerManager.isRunning.value = true
        RecipeTimerManager.timeRemaining.value = seconds

        val notification = buildNotification(seconds)

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(notificationId, notification)
        }

        if (Settings.canDrawOverlays(this)) {
            showFloatingBubble()
        }

        timerJob = CoroutineScope(Dispatchers.Main).launch {
            for (i in seconds downTo 1) {
                RecipeTimerManager.timeRemaining.value = i
                updateNotification(i)
                updateBubbleTime(i)
                delay(1000)
            }

            // הזמן נגמר
            RecipeTimerManager.timeRemaining.value = 0
            RecipeTimerManager.isRunning.value = false
            onTimerFinished()
        }
    }

    private fun updateBubbleTime(secondsRemaining: Int) {
        val minutes = secondsRemaining / 60
        val seconds = secondsRemaining % 60
        val timeText = getString(R.string.timer_format, minutes, seconds)
        bubbleTimeText?.text = timeText
    }

    private fun onTimerFinished() {
        playAlarm()
        showTimeUpNotification()

        val timeZero = "00:00"
        bubbleTimeText?.text = timeZero
        bubbleTimeText?.setTextColor(Color.RED)

        if (Settings.canDrawOverlays(this) && floatingView == null) {
            showFloatingBubble()
            bubbleTimeText?.text = timeZero
            bubbleTimeText?.setTextColor(Color.RED)
        }
    }

    private fun playAlarm() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            // Error playing alarm
        }
    }

    private fun stopAlarmAndRemoveBubble() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopEverything() {
        timerJob?.cancel()
        RecipeTimerManager.timeRemaining.value = 0
        RecipeTimerManager.isRunning.value = false
        stopAlarmAndRemoveBubble()
    }

    // פונקציה לפתיחת האפליקציה (כשלוחצים על הבועה)
    private fun openApp() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (launchIntent != null) {
            startActivity(launchIntent)
        }
    }

    private fun showFloatingBubble() {
        if (floatingView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val context = this

        // עיצוב מוקטן וקומפקטי
        val bubbleBackground = GradientDrawable().apply {
            setColor("#FAFAFA".toColorInt())
            cornerRadius = 35f // רדיוס קטן יותר
            setStroke(2, "#E0E0E0".toColorInt())
        }

        val view = object : LinearLayout(context) {
            override fun performClick(): Boolean {
                if (super.performClick()) return true
                openApp()
                return true
            }
        }.apply {
            orientation = LinearLayout.VERTICAL
            background = bubbleBackground
            elevation = 8f // הצללה קטנה יותר
            setPadding(24, 16, 24, 24) // ריווח פנימי (Padding) מוקטן

            val topRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                // ה-X האדום קטן יותר
                val closeButton = TextView(context).apply {
                    val closeIcon = "✖"
                    text = closeIcon
                    textSize = 14f
                    setTextColor("#D32F2F".toColorInt())
                    setPadding(12, 0, 16, 0)
                    setOnClickListener { stopEverything() }
                }

                // כותרת מוקטנת
                val titleText = TextView(context).apply {
                    text = currentTitle
                    textSize = 12f
                    setTextColor(Color.DKGRAY)
                    gravity = Gravity.START
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }

                addView(closeButton)
                addView(titleText)
            }

            // טקסט הזמן מוקטן
            bubbleTimeText = TextView(context).apply {
                val timeZero = "00:00"
                text = timeZero
                textSize = 24f
                setTextColor("#1976D2".toColorInt())
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 0)
            }

            addView(topRow)
            addView(bubbleTimeText)
        }

        floatingView = view

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 200
        }

        // לוגיקה לגרירה מול לחיצה (Click)
        var initialY = 0
        var initialX = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = abs(event.rawX - initialTouchX)
                    val dy = abs(event.rawY - initialTouchY)
                    // אם האצבע זזה יותר מ-10 פיקסלים, זו גרירה ולא לחיצה
                    if (dx > 10 || dy > 10) {
                        isDragging = true
                    }
                    if (isDragging) {
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // המשתמש פשוט לחץ! פותחים את האפליקציה חזרה למתכון
                        v.performClick()
                    }
                    v.performClick()
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(floatingView, layoutParams)
    }

    private fun getOpenAppPendingIntent(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        } ?: return null

        return PendingIntent.getActivity(
            this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun buildNotification(secondsRemaining: Int): Notification {
        val minutes = secondsRemaining / 60
        val seconds = secondsRemaining % 60
        val timeStr = getString(R.string.timer_format, minutes, seconds)

        val stopIntent = Intent(this, TimerService::class.java).apply { action = "STOP_TIMER" }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.timer_active_desc) + ": $currentTitle")
            .setContentText(getString(R.string.time_left) + ": $timeStr")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(getOpenAppPendingIntent())
            .addAction(android.R.drawable.ic_menu_view, getString(R.string.open_recipe), getOpenAppPendingIntent())
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.cancel_timer_desc), stopPendingIntent)
            .build()
    }

    private fun updateNotification(secondsRemaining: Int) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, buildNotification(secondsRemaining))
    }

    private fun showTimeUpNotification() {
        val stopIntent = Intent(this, TimerService::class.java).apply { action = "STOP_ALARM" }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.time_up_title))
            .setContentText(getString(R.string.time_up_message, currentTitle))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(getOpenAppPendingIntent())
            .addAction(android.R.drawable.ic_menu_view, getString(R.string.open_recipe), getOpenAppPendingIntent())
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.stop_alarm), stopPendingIntent)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Recipe Timer",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopEverything()
    }
}