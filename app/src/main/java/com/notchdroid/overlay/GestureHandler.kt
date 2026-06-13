package com.notchdroid.overlay

import android.content.Context
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import com.notchdroid.util.ActivityModule

class GestureHandler(
    context: Context,
    private val target: View,
    private val callbacks: Callbacks
) {
    interface Callbacks {
        fun onSingleTap()
        fun onDoubleTap()
        fun onLongPress()
        fun onSwipeDown()
        fun onAnyTouch()
    }

    private var velocityTracker: VelocityTracker? = null
    private var swipeHandled = false

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                target.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                callbacks.onSingleTap()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                target.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                callbacks.onDoubleTap()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                target.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                callbacks.onLongPress()
            }
        }
    )

    fun attach() {
    target.setOnTouchListener { _, event ->
        android.util.Log.d("NotchDroid", "Touch event: ${event.actionMasked}")
        callbacks.onAnyTouch()
            velocityTracker?.addMovement(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    swipeHandled = false
                    velocityTracker = VelocityTracker.obtain()
                    velocityTracker?.addMovement(event)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!swipeHandled && event.historySize > 0) {
                        val dy = event.y - event.getHistoricalY(0)
                        if (dy > 30) {
                            velocityTracker?.computeCurrentVelocity(1000)
                            val vy = velocityTracker?.yVelocity ?: 0f
                            if (vy > 800f) {
                                swipeHandled = true
                                callbacks.onSwipeDown()
                                return@setOnTouchListener true
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    velocityTracker?.recycle()
                    velocityTracker = null
                }
            }

            gestureDetector.onTouchEvent(event)
            true
        }
    }
}
