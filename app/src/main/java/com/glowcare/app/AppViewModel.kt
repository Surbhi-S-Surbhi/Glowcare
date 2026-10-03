package com.glowcare.app

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glowcare.app.auth.AuthManager
import com.glowcare.app.auth.UserProfile
import com.glowcare.app.billing.BillingManager
import com.glowcare.app.data.AppRepository
import com.glowcare.app.data.Category
import com.glowcare.app.data.Journey
import com.glowcare.app.data.Tip
import com.glowcare.app.data.TipLibrary
import com.glowcare.app.data.UserState
import com.glowcare.app.notifications.ReminderScheduler
import kotlinx.coroutines.launch

/**
 * Single source of truth for user state. Premium entitlement comes ONLY from
 * verified Google Play purchase state (BillingManager) — never a stored flag.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppRepository(app)
    private val context = app.applicationContext

    val billing = BillingManager(context)

    var state by mutableStateOf(UserState())
        private set

    var profile by mutableStateOf(UserProfile())
        private set

    /** Live premium entitlement: verified Play subscription OR authenticated owner. */
    var premium by mutableStateOf(false)
        private set

    val isOwner: Boolean get() = profile.isOwner

    /**
     * True once Billing connected OR a short timeout elapsed, so the gate
     * never flashes the paywall at a subscriber before Play answers.
     */
    var billingReady by mutableStateOf(false)
        private set

    init {
        billing.connect()
        viewModelScope.launch {
            var alarmAnchored = false
            repo.state.collect { s ->
                state = s
                if (!alarmAnchored) {
                    alarmAnchored = true
                    if (s.onboarded && s.reminderEnabled) {
                        ReminderScheduler.schedule(context, s.reminderHour, s.reminderMinute)
                    }
                }
            }
        }
        viewModelScope.launch {
            AuthManager.profile.collect {
                profile = it
                // Owner (or sign-out) changes premium immediately.
                premium = billing.entitlement.value != BillingManager.Entitlement.FREE || it.isOwner
                if (it.isOwner) billingReady = true
            }
        }
        viewModelScope.launch {
            billing.entitlement.collect { ent ->
                premium = ent != BillingManager.Entitlement.FREE || isOwner
                if (premium) billingReady = true
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(4_000)
            billingReady = true
        }
    }

    /** Marks first-launch onboarding complete (Premium is verified by Play). */
    fun markOnboarded() {
        viewModelScope.launch {
            repo.update { it.copy(onboarded = true) }
        }
    }

    /**
     * Debug builds only: lets QA enter the FREE tier when Play products are
     * unavailable. Never grants premium; release builds ignore it entirely.
     */
    var debugFreeTier by mutableStateOf(false)
        private set

    fun enableDebugFreeTier() {
        debugFreeTier = true
    }

    fun trialDaysRemaining(): Int? = billing.trialDaysRemaining()

    fun completeOnboarding(interests: List<String>, reminderEnabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            repo.update { s ->
                s.copy(
                    onboarded = true,
                    interests = interests,
                    reminderEnabled = reminderEnabled,
                    reminderHour = hour,
                    reminderMinute = minute
                )
            }
            if (reminderEnabled) ReminderScheduler.schedule(context, hour, minute)
            else ReminderScheduler.cancel(context)
        }
    }

    /** Starts a journey; enforces free-tier limits. Returns false when blocked. */
    fun startJourney(category: Category, length: Int, onBlocked: () -> Unit) {
        val alreadyActive = state.activeJourneys.any { it.categoryId == category.id }
        if (!premium) {
            if (state.activeJourneys.isNotEmpty() && !alreadyActive) { onBlocked(); return }
            if (length != 7) { onBlocked(); return }
        }
        viewModelScope.launch {
            repo.update { s ->
                val cleaned = s.journeys.filter {
                    !(it.categoryId == category.id && !it.isFinished)
                }
                s.copy(
                    journeys = cleaned + Journey(
                        categoryId = category.id,
                        length = length,
                        startDateEpochDay = Journey.todayEpochDay
                    ),
                    exploredCategories = s.exploredCategories + category.id
                )
            }
        }
    }

    fun abandonJourney(categoryId: String) {
        viewModelScope.launch {
            repo.update { s ->
                s.copy(journeys = s.journeys.filter { !(it.categoryId == categoryId && !it.isFinished) })
            }
        }
    }

    fun completeTip(tip: Tip) {
        viewModelScope.launch {
            repo.update { s ->
                val withTip = s.withTipCompleted(tip.id)
                val journeys = withTip.journeys.map { j ->
                    if (j.categoryId == tip.category.id &&
                        TipLibrary.tipForDay(tip.category, j.currentDay)?.id == tip.id
                    ) j.copy(completedTipIds = j.completedTipIds + tip.id)
                    else j
                }
                withTip.copy(journeys = journeys)
            }
        }
    }

    fun toggleSave(tipId: String) {
        viewModelScope.launch {
            repo.update { s ->
                val saved = if (tipId in s.savedTipIds) s.savedTipIds - tipId
                else if (premium || s.savedTipIds.size < 10) s.savedTipIds + tipId
                else s.savedTipIds
                s.copy(savedTipIds = saved)
            }
        }
    }

    fun markExplored(category: Category) {
        if (category.id in state.exploredCategories) return
        viewModelScope.launch {
            repo.update { it.copy(exploredCategories = it.exploredCategories + category.id) }
        }
    }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            repo.update { it.copy(reminderEnabled = enabled, reminderHour = hour, reminderMinute = minute) }
            if (enabled) ReminderScheduler.schedule(context, hour, minute)
            else ReminderScheduler.cancel(context)
        }
    }

    fun setInterests(interests: List<String>) {
        viewModelScope.launch { repo.update { it.copy(interests = interests) } }
    }

    fun resetAll() {
        viewModelScope.launch {
            repo.wipeAll()
            ReminderScheduler.cancel(context)
        }
    }

    // ---- auth ----

    fun signInWithGoogle(activityContext: android.content.Context, onResult: (Result<UserProfile>) -> Unit) {
        viewModelScope.launch {
            val result = AuthManager.signInWithGoogle(activityContext)
            onResult(result)
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch { AuthManager.continueAsGuest() }
    }

    fun signOut() {
        viewModelScope.launch { AuthManager.signOut() }
    }

    // ---- billing ----

    fun launchSubscription(activity: Activity, productId: String) {
        billing.launchSubscription(activity, productId)
    }

    fun restorePurchases() {
        billing.refresh()
    }

    // ---- derived helpers ----

    fun todayTips(): List<Tip> =
        state.activeJourneys.mapNotNull { j ->
            TipLibrary.tipForDay(Category.byId(j.categoryId), j.currentDay)
        }

    fun isPremiumBlockedFor(length: Int): Boolean = !premium && length != 7

    fun isFilterLocked(index: Int): Boolean {
        val filters = com.glowcare.app.ui.camera.FilterEngine.FILTERS
        val f = filters.getOrNull(index) ?: filters.first()
        return com.glowcare.app.ui.camera.FilterEngine.isFilterLocked(f, premium)
    }
}
