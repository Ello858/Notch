package com.notchdroid.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.notchdroid.util.PermissionHelper
import com.notchdroid.util.Prefs

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Prefs.isOnboardingComplete(context)) return
        if (!PermissionHelper.allPermissionsGranted(context)) {
            Log.w(TAG, "Permissions not valid after boot, skipping start")
            return
        }
        NotchOverlayService.start(context)
    }
}
