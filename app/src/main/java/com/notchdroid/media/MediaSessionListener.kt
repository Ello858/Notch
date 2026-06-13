package com.notchdroid.media

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.notchdroid.util.PermissionHelper

class MediaSessionListener(
    private val context: Context,
    private val callback: MediaStateCallback
) {
    interface MediaStateCallback {
        fun onMediaStateChanged(state: MediaState)
    }

    data class MediaState(
        val controller: MediaController?,
        val isPlaying: Boolean,
        val title: String,
        val artist: String,
        val duration: Long,
        val position: Long
    )

    companion object {
        private const val TAG = "NotchDroid"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    private var activeController: MediaController? = null
    private var controllerCallback: MediaController.Callback? = null
    private var registered = false

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        Log.d(TAG, "onActiveSessionsChanged: ${controllers?.size ?: 0} session(s)")
        controllers?.forEachIndexed { index, controller ->
            val pkg = controller.packageName
            val title = controller.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
            Log.d(TAG, "  session[$index] pkg=$pkg title=$title")
        }
        handler.post { updateActiveController(controllers) }
    }

    fun start() {
        if (!PermissionHelper.hasNotificationAccess(context)) {
            Log.w(TAG, "MediaSessionListener.start — notification access NOT granted")
            return
        }
        Log.d(TAG, "MediaSessionListener.start — notification access granted, registering")
        register()
    }

    fun onNotificationListenerConnected() {
        Log.d(TAG, "MediaSessionListener.onNotificationListenerConnected — re-registering")
        handler.post {
            stopInternal()
            register()
        }
    }

    fun stop() {
        stopInternal()
    }

    private fun register() {
        if (registered) return
        try {
            val component = ComponentName(context, NotchNotificationListener::class.java)
            mediaSessionManager.addOnActiveSessionsChangedListener(
                sessionsListener,
                component,
                handler
            )
            registered = true
            Log.d(TAG, "MediaSessionListener registered with component=$component")
            val controllers = mediaSessionManager.getActiveSessions(component)
            Log.d(TAG, "getActiveSessions returned ${controllers.size} controller(s)")
            updateActiveController(controllers)
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to register media session listener", e)
        }
    }

    private fun stopInternal() {
        if (registered) {
            try {
                mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsListener)
            } catch (_: Exception) { }
            registered = false
        }
        detachControllerCallback()
        activeController = null
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        detachControllerCallback()
        activeController = controllers?.firstOrNull { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING } ?: controllers?.firstOrNull()
        if (activeController != null) {
            Log.d(TAG, "Active controller: pkg=${activeController!!.packageName}")
            attachControllerCallback(activeController!!)
        } else {
            Log.d(TAG, "No active media controller")
        }
        notifyState()
    }

    private fun attachControllerCallback(controller: MediaController) {
        controllerCallback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
                Log.d(TAG, "onPlaybackStateChanged: state=${state?.state}")
                notifyState()
            }

            override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
                Log.d(TAG, "onMetadataChanged: title=$title")
                notifyState()
            }
        }
        controller.registerCallback(controllerCallback!!, handler)
    }

    private fun detachControllerCallback() {
        controllerCallback?.let { cb ->
            try {
                activeController?.unregisterCallback(cb)
            } catch (_: Exception) { }
        }
        controllerCallback = null
    }

    fun getCurrentState(): MediaState = buildState(activeController)

    private fun notifyState() {
        val state = buildState(activeController)
        Log.d(TAG, "notifyState: playing=${state.isPlaying} title='${state.title}' artist='${state.artist}'")
        callback.onMediaStateChanged(state)
    }

    private fun buildState(controller: MediaController?): MediaState {
        if (controller == null) {
            return MediaState(null, false, "", "", 0L, 0L)
        }
        val metadata = controller.metadata
        val playbackState = controller.playbackState
        val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(android.media.MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: context.getString(com.notchdroid.R.string.unknown_title)
        val artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: context.getString(com.notchdroid.R.string.unknown_artist)
        val duration = metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = playbackState?.position ?: 0L
        val isPlaying = playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
        return MediaState(controller, isPlaying, title, artist, duration, position)
    }
}
