package com.dpfr.app.trigger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.dpfr.app.MainActivity
import com.dpfr.app.R
import com.dpfr.app.data.Feature
import com.dpfr.app.data.FeatureSettings
import com.dpfr.app.data.TriggerStore
import com.dpfr.app.overlay.OverlayService

/**
 * Checks the foreground app once per second. When an app with a rule comes to the front,
 * its prank starts. When the app leaves, the prank is removed.
 */
class AppTriggerService : Service() {

    companion object {
        const val TRIGGER_TAG = "trigger"
        private const val CHANNEL_ID = "dpfr_trigger"
        private const val NOTIF_ID = 1002

        @Volatile
        var running: Boolean = false
            private set

        /** Starts or stops the service so it matches the switch, the permission and the rules. */
        fun sync(context: Context) {
            val settings = FeatureSettings(context)
            val shouldRun = settings.get(Feature.APP_TRIGGER) &&
                UsageAccessHelper.hasAccess(context) &&
                TriggerStore(context).list().any { it.enabled }
            val intent = Intent(context, AppTriggerService::class.java)
            if (shouldRun) {
                if (!running) {
                    runCatching { ContextCompat.startForegroundService(context, intent) }
                }
            } else if (running) {
                context.stopService(intent)
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var current: String? = null
    private var appliedSignature = ""
    private var lastQuery = 0L

    private val ticker = object : Runnable {
        override fun run() {
            poll()
            if (running) handler.postDelayed(this, 1000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        running = false
        OverlayService.removeTag(this, TRIGGER_TAG)
        super.onDestroy()
    }

    private fun poll() {
        val settings = FeatureSettings(this)
        if (!settings.get(Feature.APP_TRIGGER) || !UsageAccessHelper.hasAccess(this)) {
            stopSelf()
            return
        }
        val rules = TriggerStore(this).list().filter { it.enabled }
        if (rules.isEmpty()) {
            stopSelf()
            return
        }

        val pkg = foregroundPackage()
        val matching = if (pkg == null) emptyList() else rules.filter { it.packageName == pkg }
        val signature = (pkg ?: "") + "|" + matching.joinToString(";") { it.toJson().toString() }

        if (signature != appliedSignature) {
            OverlayService.removeTag(this, TRIGGER_TAG)
            appliedSignature = signature
            matching.forEach {
                OverlayService.add(this, it.prankKey, false, TRIGGER_TAG, it.placement)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun foregroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val begin = if (lastQuery == 0L) now - 10 * 60 * 1000L else lastQuery - 2000L
        lastQuery = now

        val events = usm.queryEvents(begin, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> current = event.packageName
                UsageEvents.Event.MOVE_TO_BACKGROUND ->
                    if (current == event.packageName) current = null
            }
        }
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (power.isInteractive) current else null
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "App trigger", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shown while Dpfr watches for chosen apps" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openPi = PendingIntent.getActivity(
            this, 4, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle("Dpfr app trigger is on")
            .setContentText("Watching the apps in your rules.")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPi)
            .build()
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (e: Exception) {
            // The system refused. The next sync from the app will try again.
        }
    }
}
