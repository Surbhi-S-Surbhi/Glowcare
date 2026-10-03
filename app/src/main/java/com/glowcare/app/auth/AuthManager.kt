package com.glowcare.app.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "glowcare_auth")

data class UserProfile(
    val displayName: String? = null,
    val email: String? = null,
    val profilePhotoUri: String? = null,
    val isGuest: Boolean = true,
    val chosen: Boolean = false          // true once user signed in or chose guest
) {
    val firstName: String? get() = displayName?.trim()?.split(" ")?.firstOrNull()
    val isSignedIn: Boolean get() = displayName != null || email != null

    /** True only for the authenticated OWNER account (never for guests). */
    val isOwner: Boolean get() = !isGuest && isSignedIn && email == AuthManager.OWNER_EMAIL
}

/**
 * Google Sign-In via Android Credential Manager (real Google authentication —
 * no passwords are ever seen or stored by GlowCare).
 *
 * The Web OAuth Client ID is supplied at BUILD time (never hardcoded here):
 * put `GOOGLE_WEB_CLIENT_ID=<id>.apps.googleusercontent.com` in
 * GlowCare/local.properties (or -PgoogleWebClientId= / env GOOGLE_WEB_CLIENT_ID)
 * and rebuild. One-time Cloud Console setup, incl. this device's SHA-1:
 * see README_GOOGLE_SIGNIN.md in the project root.
 */
object AuthManager {

    /** Injected from local.properties / gradle property at build time. */
    val GOOGLE_WEB_CLIENT_ID: String = com.glowcare.app.BuildConfig.GOOGLE_WEB_CLIENT_ID

    /**
     * GlowCare OWNER account. Premium is granted to this account ONLY after
     * successful Google authentication verifies this exact email — never from
     * any typed input (there is no email text field in the app).
     */
    const val OWNER_EMAIL: String = "surbhirohilla49@gmail.com"

    private val NAME = stringPreferencesKey("display_name")
    private val EMAIL = stringPreferencesKey("email")
    private val PHOTO = stringPreferencesKey("photo_uri")
    private val GUEST = androidx.datastore.preferences.core.booleanPreferencesKey("is_guest")
    private val CHOSEN = androidx.datastore.preferences.core.booleanPreferencesKey("chosen")

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val profile: Flow<UserProfile>
        get() = appContext.authDataStore.data.map { p ->
        UserProfile(
            displayName = p[NAME],
            email = p[EMAIL],
            profilePhotoUri = p[PHOTO],
            isGuest = p[GUEST] ?: true,
            chosen = p[CHOSEN] ?: false
        )
    }

    val isConfigured: Boolean get() = GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /**
     * Launches the REAL Google account chooser via Credential Manager.
     * setFilterByAuthorizedAccounts(false) lists every Google account on the
     * device (not just previously-used ones); no auto-select so the chooser
     * sheet always appears for an explicit choice.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<UserProfile> {
        if (!isConfigured) {
            return Result.failure(IllegalStateException(
                "Google Sign-In is not configured yet: add " +
                "GOOGLE_WEB_CLIENT_ID=<your Web OAuth client ID> to GlowCare/local.properties " +
                "and rebuild. Full setup steps: README_GOOGLE_SIGNIN.md"
            ))
        }
        return runCatching {
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(GOOGLE_WEB_CLIENT_ID)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                val profile = UserProfile(
                    displayName = google.displayName,
                    email = credential.data.getString(
                        "com.google.android.libraries.identity.googleid.BUNDLE_KEY_EMAIL"
                    ),
                    profilePhotoUri = google.profilePictureUri?.toString(),
                    isGuest = false,
                    chosen = true
                )
                persist(profile)
                profile
            } else {
                throw IllegalStateException("Unsupported credential type")
            }
        }
    }

    suspend fun continueAsGuest() {
        persist(UserProfile(displayName = null, email = null, isGuest = true, chosen = true))
    }

    suspend fun signOut() {
        appContext.authDataStore.edit { it.clear() }
        if (isConfigured) {
            runCatching {
                CredentialManager.create(appContext)
                    .clearCredentialState(ClearCredentialStateRequest())
            }
        }
    }

    /**
     * DEBUG/QA ONLY: injects the same UserProfile that a successful real
     * Google sign-in produces, so the owner-entitlement path can be tested
     * end-to-end before OAuth credentials are configured. Strictly no-op in
     * release builds; production can only reach here via real authentication.
     */
    suspend fun debugSetProfileForQa(profile: UserProfile) {
        if (!com.glowcare.app.BuildConfig.DEBUG) return
        persist(profile)
    }

    private suspend fun persist(profile: UserProfile) {
        appContext.authDataStore.edit { p ->
            profile.displayName?.let { p[NAME] = it }
            profile.email?.let { p[EMAIL] = it }
            profile.profilePhotoUri?.let { p[PHOTO] = it }
            p[GUEST] = profile.isGuest
            p[CHOSEN] = profile.chosen
        }
    }
}
