package com.notchdroid.util

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

data class IslandPlacement(
    val x: Int,
    val y: Int,
    val collapsedWidthPx: Int,
    val collapsedHeightPx: Int
)

object CutoutHelper {

    fun getPlacement(context: Context, position: NotchPosition): IslandPlacement {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        val collapsedWidthPx = dpToPx(context, 126f)
        val collapsedHeightPx = dpToPx(context, 37f)

        if (Prefs.useManualNotchPosition(context)) {
            val manualX = Prefs.getManualNotchX(context)
            val manualY = Prefs.getManualNotchY(context)
            if (manualX >= 0 && manualY >= 0) {
                return IslandPlacement(
                    x = manualX.coerceIn(0, metrics.widthPixels - collapsedWidthPx),
                    y = manualY.coerceAtLeast(0),
                    collapsedWidthPx = collapsedWidthPx,
                    collapsedHeightPx = collapsedHeightPx
                )
            }
        }

        val statusBarHeight = getStatusBarHeight(context)
        val cutoutCenter = getCutoutCenter(context, metrics, statusBarHeight)

        val y = if (cutoutCenter != null) {
            cutoutCenter.y - collapsedHeightPx / 2
        } else {
            statusBarHeight
        }

        val x = when (position) {
            NotchPosition.TOP_CENTER -> {
                val centerX = cutoutCenter?.x ?: (metrics.widthPixels / 2)
                centerX - collapsedWidthPx / 2
            }
            NotchPosition.TOP_LEFT -> dpToPx(context, 16f)
            NotchPosition.TOP_RIGHT -> metrics.widthPixels - collapsedWidthPx - dpToPx(context, 16f)
        }

        return IslandPlacement(
            x = x.coerceIn(0, metrics.widthPixels - collapsedWidthPx),
            y = y.coerceAtLeast(0),
            collapsedWidthPx = collapsedWidthPx,
            collapsedHeightPx = collapsedHeightPx
        )
    }

    fun getScreenMetrics(context: Context): DisplayMetrics {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return metrics
    }

    private fun getCutoutCenter(context: Context, metrics: DisplayMetrics, statusBarHeight: Int): Point? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val insets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            wm.currentWindowMetrics.windowInsets
        } else {
            null
        }

        val cutout = insets?.displayCutout
            ?: return Point(metrics.widthPixels / 2, statusBarHeight + dpToPx(context, 8f))

        val boundingRects = cutout.boundingRects
        if (boundingRects.isEmpty()) return null

        val topRect = boundingRects.minByOrNull { it.top } ?: return null
        return Point(topRect.centerX(), topRect.centerY())
    }

    private fun getStatusBarHeight(context: Context): Int {
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) context.resources.getDimensionPixelSize(resId) else dpToPx(context, 24f)
    }

    fun dpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }

    fun getExpandedWidthPx(context: Context): Int {
        val screenWidth = getScreenMetrics(context).widthPixels
        val maxWidth = dpToPx(context, 400f)
        return (screenWidth * 0.90f).toInt().coerceAtMost(maxWidth)
    }
    fun getExpandedHeightPx(context: Context): Int = dpToPx(context, 60f)
}
