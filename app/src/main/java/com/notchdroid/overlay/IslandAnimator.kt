package com.notchdroid.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.notchdroid.R
import com.notchdroid.util.CutoutHelper

class IslandAnimator(
    private val context: Context,
    private val root: ConstraintLayout,
    private val moduleContainer: View,
    private val onLayoutChanged: (width: Int, height: Int) -> Unit,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onExpanded()
        fun onCollapsed()
        fun onVisibilityChanged(visible: Boolean)
    }

    companion object {
        private const val EXPAND_DURATION = 300L
        private const val PAUSE_COLLAPSE_MS = 2000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isExpanded = false
    private var userTouched = false

    private val pauseCollapseRunnable = Runnable { collapse() }

    private val collapsedWidth = CutoutHelper.dpToPx(context, 126f)
    private val collapsedHeight = CutoutHelper.dpToPx(context, 37f)
    private val expandedWidth = CutoutHelper.getExpandedWidthPx(context)
    private val expandedHeight = CutoutHelper.getExpandedHeightPx(context)

    fun expand() {
        if (isExpanded) {
            return
        }
        isExpanded = true

        val transition = ChangeBounds()
        transition.duration = EXPAND_DURATION
        transition.interpolator = OvershootInterpolator(1.2f)
        TransitionManager.beginDelayedTransition(root, transition)

        root.setBackgroundResource(R.drawable.island_background_expanded)
        onLayoutChanged(expandedWidth, expandedHeight)

        moduleContainer.visibility = View.VISIBLE
        moduleContainer.alpha = 1f

        val scaleAnim = ValueAnimator.ofFloat(0.95f, 1f)
        scaleAnim.duration = EXPAND_DURATION
        scaleAnim.interpolator = OvershootInterpolator(1.2f)
        scaleAnim.addUpdateListener { anim ->
            val scale = anim.animatedValue as Float
            root.scaleX = scale
            root.scaleY = scale
        }
        scaleAnim.start()

        setVisible(true)
        callbacks.onExpanded()
    }

    fun collapse() {
        if (!isExpanded) return
        isExpanded = false
        handler.removeCallbacks(pauseCollapseRunnable)

        val transition = ChangeBounds()
        transition.duration = EXPAND_DURATION
        transition.interpolator = OvershootInterpolator(1.2f)
        TransitionManager.beginDelayedTransition(root, transition)

        root.setBackgroundResource(R.drawable.island_background_collapsed)
        onLayoutChanged(collapsedWidth, collapsedHeight)
        root.scaleX = 1f
        root.scaleY = 1f

        moduleContainer.animate().alpha(0f).setDuration(200L).withEndAction {
            moduleContainer.visibility = View.GONE
        }.start()

        callbacks.onCollapsed()
        userTouched = false
        updateIdleVisibility()
    }

    fun onUserTouch() {
        userTouched = true
        setVisible(true)
    }

    fun schedulePauseCollapse() {
        handler.removeCallbacks(pauseCollapseRunnable)
        handler.postDelayed(pauseCollapseRunnable, PAUSE_COLLAPSE_MS)
    }

    fun isExpanded(): Boolean = isExpanded

    private fun updateIdleVisibility() {
        val shouldShow = userTouched || isExpanded
        setVisible(shouldShow)
    }

    private fun setVisible(visible: Boolean) {
        root.animate().alpha(if (visible) 1f else 0f).setDuration(200L).start()
        callbacks.onVisibilityChanged(visible)
    }

    fun destroy() {
        handler.removeCallbacks(pauseCollapseRunnable)
    }
}
