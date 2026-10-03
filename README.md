# GlowCare 🌸

**Your daily beauty & self-care companion — small habits, better care, every day.**

A production-quality Android app (Kotlin + Jetpack Compose) combining a daily self-care
coach, 7/14/30-day journeys per category, a full beauty education library, progress
tracking, daily reminders, and an on-device beauty camera with filters and photo editing.

## Feature map

| Area | Where |
|---|---|
| Content engine (112 tips, 14 categories) | `app/src/main/java/com/glowcare/app/data/TipLibrary.kt` |
| Persistence (journeys, streaks, bookmarks, settings) | `data/AppRepository.kt` (DataStore) |
| Onboarding (4 steps) | `ui/onboarding/OnboardingScreen.kt` |
| Home dashboard, Explore, Category, Tip detail, Journey | `ui/home`, `ui/explore`, `ui/category`, `ui/tip`, `ui/journey` |
| Progress & streaks | `ui/progress/ProgressScreen.kt` |
| Profile, reminders, privacy, reset | `ui/profile` |
| Premium paywall (freemium, simulated locally) | `ui/premium/PremiumScreen.kt` |
| Search & saved tips | `ui/search`, `ui/bookmarks` |
| Beauty camera (CameraX) | `ui/camera/CameraScreen.kt` |
| Filters + beauty + editor (all on-device) | `ui/camera/FilterEngine.kt`, `ui/camera/EditorScreen.kt` |
| Daily reminder notifications | `notifications/` (AlarmManager) |

## Free vs Premium (enforced in `AppViewModel`)

- **Free:** 1 active journey, 7-day programs only, 10 saved tips, Natural filter set.
- **Premium (simulated for demo — wire Google Play Billing for release):** unlimited
  journeys, 14/30-day programs, full library, all filters.

## Privacy

- Camera permission is requested only when opening the Camera tab; denying it leaves
  every other feature working (gallery import offered instead).
- All photo processing (filters, beauty, blur) runs locally; photos never leave the
  device unless the user explicitly taps Share.
- Gallery access uses Android's photo picker (no broad storage permission).
- Everything can be deleted from Profile → Reset all data.

## Build & run

```bash
./gradlew assembleDebug
# install on a connected device/emulator:
adb install app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17+ and an Android SDK with platform 35 (`local.properties` or
`ANDROID_HOME` pointing at it).

## Content guidelines baked in

All tips are factual, non-diagnostic, non-sexualized, avoid body-shaming and
unrealistic claims, and recommend qualified professionals (dermatologist, dentist,
doctor) wherever appropriate. Add new tips in `TipLibrary.kt` — no UI changes needed.
