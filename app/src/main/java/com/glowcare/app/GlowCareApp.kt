package com.glowcare.app

import android.app.Application
import com.glowcare.app.auth.AuthManager
import com.glowcare.app.data.LanguagePrefs
import com.glowcare.app.notifications.ensureChannel
import com.glowcare.app.ui.camera.GlowCareAppContextHolder

class GlowCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
        GlowCareAppContextHolder.appContext = this
        AuthManager.init(this)
        LanguagePrefs.init(this)
    }
}
