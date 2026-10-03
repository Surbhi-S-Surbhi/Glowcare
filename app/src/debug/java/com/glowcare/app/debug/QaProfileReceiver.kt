package com.glowcare.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.glowcare.app.auth.AuthManager
import com.glowcare.app.auth.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** QA-only: simulates an authenticated profile for testing entitlement paths. */
class QaProfileReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val email = intent.getStringExtra("email") ?: return
        val name = intent.getStringExtra("name") ?: "QA User"
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AuthManager.debugSetProfileForQa(
                    UserProfile(displayName = name, email = email, isGuest = false, chosen = true)
                )
            } finally {
                pending.finish()
            }
        }
    }
}
