# GlowCare — Real Google Sign-In Setup (one-time)

The app uses Android **Credential Manager** with the official `googleid`
library: tapping "Continue with Google" opens the **real Google account
chooser** listing the Google accounts on the phone. No passwords, no fake
selectors, no typed-email owner access.

To make Google accept the request, Google must recognize two things:
**this app's package + signing SHA-1** (Android OAuth client) and a
**Web OAuth Client ID** (server client used for the ID token).

## Step 1 — Google Cloud Console (https://console.cloud.google.com)

1. Create (or pick) a project, e.g. `GlowCare`.
2. **APIs & Services → OAuth consent screen** → External → fill app name
   `GlowCare`, support email, developer email → Save. (Testing mode is fine;
   add test user emails if it stays in Testing.)
3. **APIs & Services → Credentials → Create credentials → OAuth client ID**:
   - Type **Web application** → Create. Copy the **Client ID**
     (`1234...apps.googleusercontent.com`). This is the value this app needs.
   - Create a second client: Type **Android** →
     Package name: `com.glowcare.app`
     SHA-1 certificate fingerprint (debug keystore of this machine):

     ```
     72:47:61:B6:4A:2D:E5:09:84:CC:31:AE:EF:C8:EC:0F:9E:CA:5F:E3
     ```

     (Get any machine's debug SHA-1 with:
     `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`
     For the Play release build, also add the SHA-1 of your release keystore /
     Play App Signing key.)

## Step 2 — Put the Web Client ID into the build

Edit **`GlowCare/local.properties`** (git-ignored):

```
GOOGLE_WEB_CLIENT_ID=1234567890-abcdefg.apps.googleusercontent.com
```

Alternatives (same priority order the build uses):
1. `-PgoogleWebClientId=<id>` gradle property
2. `GOOGLE_WEB_CLIENT_ID=<id>` environment variable
3. `local.properties` (recommended)

Then rebuild — the ID is baked into `BuildConfig.GOOGLE_WEB_CLIENT_ID`.
The app contains **no placeholder**: with no ID configured, tapping
"Continue with Google" shows an honest configuration error and unlocks
nothing.

## Step 3 — Device requirements

- Google Play services installed and up to date.
- At least one Google account on the device (Android Settings → Accounts).
  If none, the app shows "No Google accounts found on this device…".
- The APK must be signed with the keystore whose SHA-1 you registered
  (debug builds automatically use `~/.android/debug.keystore`).

## How the OWNER account works (security)

- After **successful real Google authentication** only, GlowCare reads the
  email from the verified Google credential.
- If it is exactly `surbhirohilla49@gmail.com` → owner unlock: no billing,
  no trial, full premium, Profile shows "GlowCare Owner /
  Premium Access — Lifetime". The session is persisted so re-subscribing is
  never needed.
- Any other account → no bypass; the selected Monthly/Yearly plan goes to
  real Google Play Billing (3-day trial when the Play offer exists).
- There is **no email text field** anywhere; typing an email can never grant
  access. The debug-only QA hook (broadcast receiver) exists solely in
  `src/debug` and is absent from release APKs.

## Common errors

| Message | Cause / fix |
|---|---|
| `ApiException: 10` / DEVELOPER_ERROR | SHA-1 or package not registered in the Android OAuth client (Step 1.3b), or wrong Web Client ID |
| "No Google accounts found…" | Add a Google account on the device |
| "not configured yet" | `GOOGLE_WEB_CLIENT_ID` empty in local.properties — fill it and rebuild |
