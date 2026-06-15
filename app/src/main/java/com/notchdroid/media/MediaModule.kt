package com.notchdroid.media

import android.graphics.Bitmap
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.notchdroid.R
import com.notchdroid.overlay.IslandAnimator

class MediaModule(
    private val container: FrameLayout,
    private val animator: IslandAnimator,
    private val onInteraction: () -> Unit
) {
    companion object {
        private const val TAG = "NotchDroid"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            if (isExpanded && currentState?.isPlaying == true) {
                handler.postDelayed(this, 500L)
            }
        }
    }

    private var musicView: View? = null
    private var albumArt: ImageView? = null
    private var songTitle: TextView? = null
    private var artistName: TextView? = null
    private var btnPrevious: ImageButton? = null
    private var btnPlayPause: ImageButton? = null
    private var btnNext: ImageButton? = null
    private var progressBar: ProgressBar? = null
    private var currentTime: TextView? = null
    private var totalTime: TextView? = null
    private var pausedLabel: TextView? = null
    private var noMediaLabel: TextView? = null
    private var textLayout: View? = null
    private var controlsLayout: View? = null

    private var currentState: MediaSessionListener.MediaState? = null
    private var isExpanded = false
    private var wasPlaying = false

    fun inflate() {
        if (musicView != null) return
        Log.d(TAG, "MediaModule.inflate — inflating module_music.xml")
        musicView = LayoutInflater.from(container.context)
            .inflate(R.layout.module_music, container, false)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        container.addView(musicView, params)

        albumArt = musicView?.findViewById(R.id.albumArt)
        songTitle = musicView?.findViewById(R.id.songTitle)
        artistName = musicView?.findViewById(R.id.artistName)
        btnPrevious = musicView?.findViewById(R.id.btnPrevious)
        btnPlayPause = musicView?.findViewById(R.id.btnPlayPause)
        btnNext = musicView?.findViewById(R.id.btnNext)
        progressBar = musicView?.findViewById(R.id.progressBar)
        currentTime = musicView?.findViewById(R.id.currentTime)
        totalTime = musicView?.findViewById(R.id.totalTime)
        pausedLabel = musicView?.findViewById(R.id.pausedLabel)
        noMediaLabel = musicView?.findViewById(R.id.noMediaLabel)
        textLayout = musicView?.findViewById(R.id.textLayout)
        controlsLayout = musicView?.findViewById(R.id.controlsLayout)

        btnPrevious?.setOnClickListener {
            onInteraction()
            currentState?.controller?.transportControls?.skipToPrevious()
        }
        btnPlayPause?.setOnClickListener {
            onInteraction()
            val controller = currentState?.controller ?: return@setOnClickListener
            val state = controller.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        }
        btnNext?.setOnClickListener {
            onInteraction()
            currentState?.controller?.transportControls?.skipToNext()
        }
        Log.d(TAG, "MediaModule.inflate — views bound: art=${albumArt != null} title=${songTitle != null}")
    }

    fun updateState(state: MediaSessionListener.MediaState) {
        currentState = state
        val hasMedia = state.controller != null
        Log.d(TAG, "MediaModule.updateState: hasMedia=$hasMedia expanded=$isExpanded title='${state.title}'")

        if (hasMedia && state.isPlaying) {
            wasPlaying = true
        } else if (hasMedia && !state.isPlaying && wasPlaying && isExpanded) {
            showPausedBriefly()
            wasPlaying = false
        } else if (!hasMedia) {
            wasPlaying = false
            if (isExpanded) {
                animator.schedulePauseCollapse()
            }
        }

        if (isExpanded) {
            bindUi(state)
        }
    }

    fun onExpanded() {
        isExpanded = true
        Log.d(TAG, "MediaModule.onExpanded — showing music UI")
        inflate()
        musicView?.visibility = View.VISIBLE
        musicView?.alpha = 1f
        container.alpha = 1f
        container.visibility = View.VISIBLE
        bindUi(currentState ?: MediaSessionListener.MediaState(null, false, "", "", 0L, 0L))
        handler.removeCallbacks(progressRunnable)
        if (currentState?.isPlaying == true) {
            handler.post(progressRunnable)
        }
    }

    fun onCollapsed() {
        isExpanded = false
        handler.removeCallbacks(progressRunnable)
        musicView?.visibility = View.GONE
        pausedLabel?.visibility = View.GONE
    }

    private fun bindUi(state: MediaSessionListener.MediaState) {
        val hasMedia = state.controller != null
        Log.d(TAG, "MediaModule.bindUi: hasMedia=$hasMedia title='${state.title}' artist='${state.artist}'")

        if (!hasMedia) {
            noMediaLabel?.visibility = View.VISIBLE
            textLayout?.visibility = View.GONE
            controlsLayout?.visibility = View.GONE
            albumArt?.visibility = View.GONE
            pausedLabel?.visibility = View.GONE
            return
        }

        noMediaLabel?.visibility = View.GONE
        textLayout?.visibility = View.VISIBLE
        controlsLayout?.visibility = View.VISIBLE
        albumArt?.visibility = View.VISIBLE
        pausedLabel?.visibility = View.GONE

        songTitle?.text = state.title
        artistName?.text = state.artist
        songTitle?.isSelected = true
        updatePlayPauseIcon(state.isPlaying)
        updateAlbumArt(state)
        updateProgress()

        Log.d(TAG, "MediaModule.bindUi — title set='${songTitle?.text}' artist='${artistName?.text}'")
    }

    private fun showPausedBriefly() {
        pausedLabel?.visibility = View.VISIBLE
        controlsLayout?.visibility = View.GONE
        textLayout?.visibility = View.GONE
        albumArt?.visibility = View.GONE
        noMediaLabel?.visibility = View.GONE
        animator.schedulePauseCollapse()
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        btnPlayPause?.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun updateAlbumArt(state: MediaSessionListener.MediaState) {
        val metadata = state.controller?.metadata ?: return
        val bitmap: Bitmap? = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
            ?: metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        Log.d(TAG, "MediaModule.updateAlbumArt: bitmap=${bitmap != null} (${bitmap?.width}x${bitmap?.height})")
        if (bitmap != null) {
            albumArt?.setImageBitmap(bitmap)
        } else {
            albumArt?.setImageResource(R.drawable.ic_music_note)
        }
    }

    private fun updateProgress() {
        val state = currentState ?: return
        val duration = state.duration
        val position = state.controller?.playbackState?.position ?: state.position
        if (duration > 0) {
            val progress = ((position.toFloat() / duration) * 1000).toInt().coerceIn(0, 1000)
            progressBar?.progress = progress
            currentTime?.text = formatTime(position)
            totalTime?.text = formatTime(duration)
        }
    }


    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    fun destroy() {
        handler.removeCallbacks(progressRunnable)
    }
}
