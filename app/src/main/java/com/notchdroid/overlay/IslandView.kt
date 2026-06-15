package com.notchdroid.overlay

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.RadioGroup
import com.notchdroid.R
import com.notchdroid.databinding.OverlayIslandBinding
import com.notchdroid.media.MediaModule
import com.notchdroid.media.MediaSessionListener
import com.notchdroid.util.ActivityModule
import com.notchdroid.util.CutoutHelper
import com.notchdroid.util.NotchPosition
import com.notchdroid.util.Prefs

class IslandView(
    context: Context,
    private val windowManager: WindowManager,
    private val layoutParams: WindowManager.LayoutParams,
    private val onRequestMediaState: () -> MediaSessionListener.MediaState?,
    private val onReposition: (IslandView) -> Unit,
    private val onExpandStateChanged: (expanded: Boolean) -> Unit = {}
) : FrameLayout(context) {

    companion object {
        private const val TAG = "NotchDroid"
    }

    private val binding: OverlayIslandBinding
    private val animator: IslandAnimator
    private val mediaModule: MediaModule
    private var currentModule = ActivityModule.MUSIC
    private var settingsPopup: PopupWindow? = null

    init {
        binding = OverlayIslandBinding.inflate(LayoutInflater.from(context), this, true)

        animator = IslandAnimator(
            context,
            binding.islandRoot,
            binding.moduleContainer,
            onLayoutChanged = { width, height ->
                val oldWidth = layoutParams.width
                val oldHeight = layoutParams.height
                if (oldWidth > 0 && oldHeight > 0) {
                    val centerX = layoutParams.x + oldWidth / 2
                    val centerY = layoutParams.y + oldHeight / 2
                    val screenWidth = CutoutHelper.getScreenMetrics(context).widthPixels
                    layoutParams.x = (centerX - width / 2).coerceIn(0, (screenWidth - width).coerceAtLeast(0))
                    layoutParams.y = (centerY - height / 2).coerceAtLeast(0)
                }
                layoutParams.width = width
                layoutParams.height = height
                Log.d(TAG, "IslandView layout changed: ${width}x${height} at (${layoutParams.x}, ${layoutParams.y})")
                try {
                    windowManager.updateViewLayout(this@IslandView, layoutParams)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update overlay layout", e)
                }
            },
            object : IslandAnimator.Callbacks {
                override fun onExpanded() {
                    Log.d(TAG, "IslandView onExpanded — refreshing media and binding UI")
                    val state = onRequestMediaState()
                    if (state != null) {
                        mediaModule.updateState(state)
                    }
                    mediaModule.onExpanded()
                    onExpandStateChanged(true)
                }

                override fun onCollapsed() {
                    mediaModule.onCollapsed()
                    onExpandStateChanged(false)
                }

                override fun onVisibilityChanged(visible: Boolean) {
                    Log.d(TAG, "IslandView visibility: $visible")
                }
            }
        )

        mediaModule = MediaModule(binding.moduleContainer, animator) {
            animator.onUserTouch()
        }

        GestureHandler(context, binding.islandRoot, object : GestureHandler.Callbacks {
            override fun onSingleTap() {
                animator.onUserTouch()
                if (currentModule == ActivityModule.MUSIC) {
                    if (animator.isExpanded()) {
                        animator.collapse()
                    } else {
                        animator.expand()
                    }
                }
            }

            override fun onDoubleTap() {
                cycleModule()
            }

            override fun onLongPress() {
                showSettingsPopup()
            }

            override fun onSwipeDown() {
                animator.collapse()
            }

            override fun onAnyTouch() {
                animator.onUserTouch()
            }
        }).attach()

        binding.islandRoot.alpha = 0f
    }

    fun updateMediaState(state: MediaSessionListener.MediaState) {
        if (currentModule == ActivityModule.MUSIC) {
            mediaModule.updateState(state)
        }
    }

    fun collapseIfExpanded() {
        if (animator.isExpanded()) {
            animator.collapse()
        }
    }

    fun isExpanded(): Boolean = animator.isExpanded()

    fun reposition() {
        val placement = CutoutHelper.getPlacement(
            context,
            Prefs.getNotchPosition(context)
        )
        layoutParams.x = placement.x
        layoutParams.y = placement.y
        layoutParams.gravity = Gravity.TOP or Gravity.START
        try {
            windowManager.updateViewLayout(this, layoutParams)
        } catch (_: Exception) { }
        onReposition(this)
    }

    private fun cycleModule() {
        currentModule = when (currentModule) {
            ActivityModule.MUSIC -> ActivityModule.TIMER
            ActivityModule.TIMER -> ActivityModule.BATTERY
            ActivityModule.BATTERY -> ActivityModule.MUSIC
        }
        if (currentModule == ActivityModule.MUSIC && animator.isExpanded()) {
            mediaModule.onExpanded()
        }
    }

    private fun showSettingsPopup() {
        settingsPopup?.dismiss()
        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_settings, null)
        val group = popupView.findViewById<RadioGroup>(R.id.popupPositionGroup)

        when (Prefs.getNotchPosition(context)) {
            NotchPosition.TOP_CENTER -> group.check(R.id.popupPosCenter)
            NotchPosition.TOP_LEFT -> group.check(R.id.popupPosLeft)
            NotchPosition.TOP_RIGHT -> group.check(R.id.popupPosRight)
        }

        group.setOnCheckedChangeListener { _, checkedId ->
            val position = when (checkedId) {
                R.id.popupPosLeft -> NotchPosition.TOP_LEFT
                R.id.popupPosRight -> NotchPosition.TOP_RIGHT
                else -> NotchPosition.TOP_CENTER
            }
            Prefs.setNotchPosition(context, position)
            reposition()
            settingsPopup?.dismiss()
        }

        settingsPopup = PopupWindow(
            popupView,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        )
        settingsPopup?.showAsDropDown(binding.islandRoot, 0, 20, Gravity.CENTER_HORIZONTAL)
    }

    fun destroy() {
        settingsPopup?.dismiss()
        animator.destroy()
        mediaModule.destroy()
    }
}
