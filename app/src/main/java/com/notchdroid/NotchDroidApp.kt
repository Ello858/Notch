package com.notchdroid

import android.app.Application
import com.notchdroid.media.MediaSessionListener

class NotchDroidApp : Application() {
    var mediaSessionListener: MediaSessionListener? = null
}
