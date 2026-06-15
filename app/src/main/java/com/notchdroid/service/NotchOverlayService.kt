package com.notchdroid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.notchdroid.NotchDroidApp
import com.notchdroid.R
import com.notchdroid.media.MediaSessionListener
import com.notchdroid.overlay.IslandView
import com.notchdroid.ui.OnboardingActivity
import com.notchdroid.util.CutoutHelper
import com.notchdroid.util.PermissionHelper
import com.notchdroid.util.Prefs

class NotchOverlayService : Service() {

    companion object {
        private const val TAG = "NotchOverlay"
        private const val CHANNEL_ID = "notchdroid_overlay"
        private const val NOTIFICATION_ID = 1
        const val ACTION_REPOSITION = "com.notchdroid.REPOSITION"

        fun start(context: android.content.Context) {
            val intent = Intent(context, NotchOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun reposition(context: android.content.Context) {
            val intent = Intent(context, NotchOverlayService::class.java).apply {
                action = ACTION_REPOSITION
            }
            context.startService(intent)
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, NotchOverlayService::class.java))
        }
    }

    private var windowManager: WindowManager? = null
    private var islandView: IslandView? = null
    private var scrimView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var mediaSessionListener: MediaSessionListener? = null

    override fun onCreate() {
        super.onCreate()
        if (!PermissionHelper.canDrawOverlays(this)) {
            Log.e(TAG, "Overlay permission missing, stopping")
            stopSelf()
            return
        }
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        setupOverlay()
        setupMediaListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REPOSITION) {
            islandView?.reposition()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mediaSessionListener?.stop()
        (application as? NotchDroidApp)?.mediaSessionListener = null
        removeScrim()
        islandView?.destroy()
        islandView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) { }
        }
        islandView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val placement = CutoutHelper.getPlacement(this, Prefs.getNotchPosition(this))

        val params = WindowManager.LayoutParams(
            placement.collapsedWidthPx,
            placement.collapsedHeightPx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = placement.x
        params.y = placement.y

        layoutParams = params
        islandView = IslandView(
            this,
            windowManager!!,
            params,
            onRequestMediaState = { mediaSessionListener?.getCurrentState() },
            onReposition = { },
            onExpandStateChanged = { expanded ->
                if (expanded) addScrim() else removeScrim()
            }
        )
        windowManager?.addView(islandView, params)
    }

    private fun addScrim() {
        if (scrimView != null) return
        val wm = windowManager ?: return
        val view = View(this)
        view.setOnClickListener { islandView?.collapseIfExpanded() }
        view.isClickable = true
        view.isFocusable = false

        val scrimParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        scrimParams.gravity = Gravity.TOP or Gravity.START

        try {
            wm.addView(view, scrimParams)
            scrimView = view
            islandView?.let { iv ->
                try {
                    wm.removeView(iv)
                    wm.addView(iv, layoutParams)
                } catch (_: Exception) { }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add scrim view", e)
        }
    }

    private fun removeScrim() {
        val wm = windowManager ?: return
        scrimView?.let { view ->
            try {
                wm.removeView(view)
            } catch (_: Exception) { }
        }
        scrimView = null
    }

    private fun setupMediaListener() {
        val hasAccess = PermissionHelper.hasNotificationAccess(this)
        android.util.Log.d("NotchDroid", "setupMediaListener: notificationAccess=$hasAccess")
        val listener = MediaSessionListener(this, object : MediaSessionListener.MediaStateCallback {
            override fun onMediaStateChanged(state: MediaSessionListener.MediaState) {
                islandView?.post { islandView?.updateMediaState(state) }
            }
        })
        mediaSessionListener = listener
        (application as NotchDroidApp).mediaSessionListener = listener
        listener.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.service_notification_text)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, OnboardingActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }
}
