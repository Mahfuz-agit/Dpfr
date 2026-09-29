package com.dpfr.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.TileService
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.dpfr.app.MainActivity
import com.dpfr.app.R
import com.dpfr.app.data.CustomShape
import com.dpfr.app.data.Feature
import com.dpfr.app.data.FeatureSettings
import com.dpfr.app.data.ShapeStore
import com.dpfr.app.tile.DpfrTileService
import com.dpfr.app.util.Haptics
import com.dpfr.app.util.ShakeDetector

/**
 * Owns every overlay window. One window per prank item.
 * Live items ignore touches so the phone stays usable. Edit items can be dragged and pinched.
 */
class OverlayService : Service() {

    companion object {
        const val ACTION_ADD = "com.dpfr.app.action.ADD"
        const val ACTION_REMOVE_TAG = "com.dpfr.app.action.REMOVE_TAG"
        const val ACTION_STOP_ALL = "com.dpfr.app.action.STOP_ALL"
        const val ACTION_LOCK_ALL = "com.dpfr.app.action.LOCK_ALL"

        const val EXTRA_KEY = "key"
        const val EXTRA_EDIT = "edit"
        const val EXTRA_TAG = "tag"
        const val EXTRA_PLACEMENT = "placement"

        private const val CHANNEL_ID = "dpfr_overlay"
        private const val NOTIF_ID = 1001
        private const val MAX_ITEMS = 12

        /**
         * Android 12+ blocks touches that pass through an overlay whose window alpha is above 0.8.
         * Staying just below keeps the phone usable under a live prank.
         */
        private const val LIVE_ALPHA = 0.79f

        @Volatile
        var running: Boolean = false
            private set

        fun add(
            context: Context,
            key: String,
            edit: Boolean = false,
            tag: String? = null,
            placement: FloatArray? = null
        ) {
            val intent = Intent(context, OverlayService::class.java)
                .setAction(ACTION_ADD)
                .putExtra(EXTRA_KEY, key)
                .putExtra(EXTRA_EDIT, edit)
            if (tag != null) intent.putExtra(EXTRA_TAG, tag)
            if (placement != null) intent.putExtra(EXTRA_PLACEMENT, placement)
            ContextCompat.startForegroundService(context, intent)
        }

        fun removeTag(context: Context, tag: String) {
            if (!running) return
            val intent = Intent(context, OverlayService::class.java)
                .setAction(ACTION_REMOVE_TAG)
                .putExtra(EXTRA_TAG, tag)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stopAll(context: Context) {
            if (!running) return
            val intent = Intent(context, OverlayService::class.java).setAction(ACTION_STOP_ALL)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }
    }

    private class Entry(
        val id: Int,
        val ref: PrankRef,
        val tag: String?,
        val host: ItemHost,
        val lp: WindowManager.LayoutParams,
        var editing: Boolean,
        var stopper: Runnable? = null
    )

    private lateinit var wm: WindowManager
    private lateinit var settings: FeatureSettings
    private val handler = Handler(Looper.getMainLooper())
    private val entries = LinkedHashMap<Int, Entry>()
    private var nextId = 1
    private var selectedId = -1
    private var editBar: EditBar? = null
    private var shake: ShakeDetector? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        settings = FeatureSettings(this)
        running = true
        createChannel()
        refreshTile()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        when (intent?.action) {
            ACTION_ADD -> addFromIntent(intent)
            ACTION_REMOVE_TAG -> removeByTag(intent.getStringExtra(EXTRA_TAG))
            ACTION_LOCK_ALL -> lockAll()
            ACTION_STOP_ALL -> {
                removeAll()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        if (entries.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeAll()
        editBar?.hide()
        editBar = null
        shake?.stop()
        shake = null
        handler.removeCallbacksAndMessages(null)
        running = false
        refreshTile()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- adding

    private fun addFromIntent(intent: Intent) {
        if (!Settings.canDrawOverlays(this)) return
        val key = intent.getStringExtra(EXTRA_KEY) ?: return
        val ref = PrankRef.parse(key) ?: return
        if (!settings.isPrankEnabled(ref)) return
        if (entries.size >= MAX_ITEMS) return

        val edit = intent.getBooleanExtra(EXTRA_EDIT, false)
        val tag = intent.getStringExtra(EXTRA_TAG)
        val placement = intent.getFloatArrayExtra(EXTRA_PLACEMENT)

        var shape: CustomShape? = null
        if (ref.type == PrankType.CUSTOM) {
            shape = ShapeStore(this).get(ref.shapeId ?: return) ?: return
        }

        val (sw, sh) = screenSize()
        val density = resources.displayMetrics.density
        val base = PrankDefaults.rect(ref.type, sw, sh, density, shape?.aspect ?: 1f)
        val rect: Rect = when {
            placement != null && placement.size >= 3 ->
                PrankDefaults.applyPlacement(base, placement[0], placement[1], placement[2], sw, sh)
            else -> settings.lastPlacement(key)?.let { PrankDefaults.fromFractions(it, sw, sh) } ?: base
        }
        createEntry(ref, tag, edit, rect, shape)
    }

    private fun flagsFor(edit: Boolean): Int {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (!edit) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return flags
    }

    private fun createEntry(ref: PrankRef, tag: String?, edit: Boolean, rect: Rect, shape: CustomShape?) {
        val view = PrankViewFactory.create(this, ref, shape)
        val host = ItemHost(this, view)
        val lp = WindowManager.LayoutParams(
            rect.width().coerceAtLeast(1),
            rect.height().coerceAtLeast(1),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flagsFor(edit),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = rect.left
            y = rect.top
            alpha = if (edit) 1f else LIVE_ALPHA
            if (Build.VERSION.SDK_INT >= 30) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            wm.addView(host, lp)
        } catch (e: Exception) {
            return
        }

        val entry = Entry(nextId++, ref, tag, host, lp, edit)
        entries[entry.id] = entry

        if (edit) {
            host.editing = true
            val (sw, sh) = screenSize()
            val min = (48 * resources.displayMetrics.density).toInt()
            host.setOnTouchListener(
                DragResizeTouchHandler(host, wm, lp, min, maxOf(sw, sh) * 3) { select(entry.id) }
            )
            select(entry.id)
        } else if (tag == null) {
            scheduleAutoStop(entry)
        }

        if (settings.get(Feature.HAPTICS)) Haptics.tick(this)
        updateChrome()
    }

    // ------------------------------------------------------------- selection

    private fun select(id: Int) {
        selectedId = id
        entries.values.forEach { it.host.picked = it.id == id }
        updateChrome()
    }

    private fun selectedEntry(): Entry? = entries[selectedId]?.takeIf { it.editing }

    private fun adjustSelected(delta: Float) {
        val entry = selectedEntry() ?: return
        val adjustable = entry.host.prank as? Adjustable ?: return
        adjustable.intensity = (adjustable.intensity + delta).coerceIn(0.05f, 1f)
    }

    private fun deleteSelected() {
        val entry = selectedEntry() ?: return
        removeEntry(entry.id)
    }

    // -------------------------------------------------------------- locking

    private fun lock(entry: Entry) {
        if (!entry.editing) return
        entry.editing = false
        entry.host.editing = false
        entry.host.picked = false
        entry.host.setOnTouchListener(null)
        entry.lp.flags = flagsFor(false)
        entry.lp.alpha = LIVE_ALPHA
        runCatching { wm.updateViewLayout(entry.host, entry.lp) }
        savePlacement(entry)
        val adjustable = entry.host.prank as? Adjustable
        if (adjustable != null && entry.ref.type == PrankType.DIMMER) {
            settings.dimmerIntensity = adjustable.intensity
        }
        if (entry.tag == null) scheduleAutoStop(entry)
    }

    private fun lockAll() {
        entries.values.toList().forEach { lock(it) }
        updateChrome()
    }

    private fun savePlacement(entry: Entry) {
        val (sw, sh) = screenSize()
        if (sw <= 0 || sh <= 0) return
        settings.setLastPlacement(
            entry.ref.key,
            floatArrayOf(
                entry.lp.x / sw.toFloat(),
                entry.lp.y / sh.toFloat(),
                entry.lp.width / sw.toFloat(),
                entry.lp.height / sh.toFloat()
            )
        )
    }

    // ------------------------------------------------------------- removing

    private fun scheduleAutoStop(entry: Entry) {
        if (!settings.get(Feature.AUTO_STOP)) return
        val seconds = settings.autoStopSeconds
        if (seconds <= 0) return
        val stopper = Runnable { removeEntry(entry.id) }
        entry.stopper = stopper
        handler.postDelayed(stopper, seconds * 1000L)
    }

    private fun removeEntry(id: Int) {
        val entry = entries.remove(id) ?: return
        entry.stopper?.let { handler.removeCallbacks(it) }
        runCatching { wm.removeView(entry.host) }
        if (selectedId == id) {
            selectedId = entries.values.lastOrNull { it.editing }?.id ?: -1
            entries.values.forEach { it.host.picked = it.id == selectedId }
        }
        updateChrome()
        if (entries.isEmpty()) scheduleStopIfEmpty()
    }

    private fun removeByTag(tag: String?) {
        if (tag == null) return
        entries.values.filter { it.tag == tag }.map { it.id }.forEach { removeEntry(it) }
    }

    private fun removeAll() {
        entries.values.toList().forEach { entry ->
            entry.stopper?.let { handler.removeCallbacks(it) }
            runCatching { wm.removeView(entry.host) }
        }
        entries.clear()
        selectedId = -1
        updateChrome()
    }

    private fun scheduleStopIfEmpty() {
        handler.postDelayed({
            if (entries.isEmpty()) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }, 400L)
    }

    // --------------------------------------------------------------- chrome

    private fun updateChrome() {
        val anyEditing = entries.values.any { it.editing }
        if (anyEditing) {
            if (editBar == null) {
                editBar = EditBar(
                    this, wm,
                    onLess = { adjustSelected(-0.1f) },
                    onMore = { adjustSelected(0.1f) },
                    onDelete = { deleteSelected() },
                    onDone = { lockAll() }
                ).also { it.show() }
            }
            editBar?.setAdjustVisible(selectedEntry()?.host?.prank is Adjustable)
        } else {
            editBar?.hide()
            editBar = null
        }

        if (entries.isNotEmpty() && settings.get(Feature.SHAKE_STOP)) {
            if (shake == null) {
                shake = ShakeDetector(this) { handler.post { stopEverything() } }.also { it.start() }
            }
        } else {
            shake?.stop()
            shake = null
        }

        runCatching {
            getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification())
        }
    }

    private fun stopEverything() {
        removeAll()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // --------------------------------------------------------- notification

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Active pranks", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shown while a prank is on the screen" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP_ALL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val lockPi = PendingIntent.getService(
            this, 2,
            Intent(this, OverlayService::class.java).setAction(ACTION_LOCK_ALL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openPi = PendingIntent.getActivity(
            this, 3, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle("Dpfr is running")
            .setContentText("${entries.size} active. Tap Stop all to remove everything.")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPi)
            .addAction(0, "Stop all", stopPi)
            .addAction(0, "Lock all", lockPi)
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
            // The system refused to promote the service. Nothing else to do.
        }
    }

    // -------------------------------------------------------------- helpers

    private fun refreshTile() {
        runCatching {
            TileService.requestListeningState(this, ComponentName(this, DpfrTileService::class.java))
        }
    }

    private fun screenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= 30) {
            val bounds = wm.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            dm.widthPixels to dm.heightPixels
        }
    }
}
