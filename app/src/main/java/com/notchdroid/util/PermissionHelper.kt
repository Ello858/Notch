package com.notchdroid.util

import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object PermissionHelper {

    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun hasNotificationAccess(context: Context): Boolean {
        val enabled = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabled.contains(context.packageName)
    }

    fun allPermissionsGranted(context: Context): Boolean =
        canDrawOverlays(context) && hasNotificationAccess(context)
}
