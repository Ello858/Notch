package com.notchdroid.util

import android.content.Context
import android.content.SharedPreferences

enum class NotchPosition {
    TOP_CENTER,
    TOP_LEFT,
    TOP_RIGHT
}

enum class ActivityModule {
    MUSIC,
    TIMER,
    BATTERY
}

object Prefs {
    private const val NAME = "notchdroid_prefs"
    private const val KEY_ONBOARDING = "onboarding_complete"
    private const val KEY_POSITION = "notch_position"
    private const val KEY_MANUAL_NOTCH = "use_manual_notch"
    private const val KEY_MANUAL_X = "manual_notch_x"
    private const val KEY_MANUAL_Y = "manual_notch_y"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isOnboardingComplete(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDING, false)

    fun setOnboardingComplete(context: Context, complete: Boolean) {
        prefs(context).edit().putBoolean(KEY_ONBOARDING, complete).apply()
    }

    fun getNotchPosition(context: Context): NotchPosition {
        val value = prefs(context).getString(KEY_POSITION, NotchPosition.TOP_CENTER.name)
        return try {
            NotchPosition.valueOf(value ?: NotchPosition.TOP_CENTER.name)
        } catch (_: IllegalArgumentException) {
            NotchPosition.TOP_CENTER
        }
    }

    fun setNotchPosition(context: Context, position: NotchPosition) {
        prefs(context).edit().putString(KEY_POSITION, position.name).apply()
    }

    fun useManualNotchPosition(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MANUAL_NOTCH, false)

    fun setUseManualNotchPosition(context: Context, use: Boolean) {
        prefs(context).edit().putBoolean(KEY_MANUAL_NOTCH, use).apply()
    }

    fun getManualNotchX(context: Context): Int =
        prefs(context).getInt(KEY_MANUAL_X, -1)

    fun getManualNotchY(context: Context): Int =
        prefs(context).getInt(KEY_MANUAL_Y, -1)

    fun setManualNotchOffset(context: Context, x: Int, y: Int) {
        prefs(context).edit()
            .putInt(KEY_MANUAL_X, x)
            .putInt(KEY_MANUAL_Y, y)
            .putBoolean(KEY_MANUAL_NOTCH, true)
            .apply()
    }
}
