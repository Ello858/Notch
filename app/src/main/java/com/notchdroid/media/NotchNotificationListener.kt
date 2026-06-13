package com.notchdroid.media

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.notchdroid.NotchDroidApp

class NotchNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "onListenerConnected — notification listener bound successfully")
        val app = application as? NotchDroidApp
        app?.mediaSessionListener?.onNotificationListenerConnected()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "onListenerDisconnected — notification listener unbound")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Media session updates are handled via MediaSessionManager listener
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No-op
    }

    companion object {
        private const val TAG = "NotchDroid"
    }
}
