package com.glowcare.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "glowcare")

/**
 * Persistence layer. All user progress, preferences and premium state survive restarts;
 * tip content is bundled so everything works fully offline.
 */
class AppRepository(private val context: Context) {

    private object Keys {
        val onboarded = booleanPreferencesKey("onboarded")
        val interests = stringSetPreferencesKey("interests")
        val journeys = stringSetPreferencesKey("journeys") // "catId:length:startDay:doneIds(csv)"
        val completedTips = stringSetPreferencesKey("completed_tips")
        val savedTips = stringSetPreferencesKey("saved_tips")
        val streak = intPreferencesKey("streak")
        val lastCompletionDay = longPreferencesKey("last_completion_day")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val explored = stringSetPreferencesKey("explored_categories")
    }

    val state: Flow<UserState> = context.dataStore.data.map { p ->
        UserState(
            onboarded = p[Keys.onboarded] ?: false,
            interests = (p[Keys.interests] ?: emptySet()).toList(),
            journeys = (p[Keys.journeys] ?: emptySet()).mapNotNull { JourneySerializer.decode(it) },
            completedTipIds = p[Keys.completedTips] ?: emptySet(),
            savedTipIds = p[Keys.savedTips] ?: emptySet(),
            streak = p[Keys.streak] ?: 0,
            lastCompletionDay = p[Keys.lastCompletionDay],
            reminderEnabled = p[Keys.reminderEnabled] ?: true,
            reminderHour = p[Keys.reminderHour] ?: 9,
            reminderMinute = p[Keys.reminderMinute] ?: 0,
            exploredCategories = p[Keys.explored] ?: emptySet()
        )
    }

    suspend fun setOnboarded(interests: List<String>) = context.dataStore.edit {
        it[Keys.onboarded] = true
        it[Keys.interests] = interests.toSet()
    }

    suspend fun update(transform: (UserState) -> UserState) = context.dataStore.edit { p ->
        val current = UserState(
            onboarded = p[Keys.onboarded] ?: false,
            interests = (p[Keys.interests] ?: emptySet()).toList(),
            journeys = (p[Keys.journeys] ?: emptySet()).mapNotNull { JourneySerializer.decode(it) },
            completedTipIds = p[Keys.completedTips] ?: emptySet(),
            savedTipIds = p[Keys.savedTips] ?: emptySet(),
            streak = p[Keys.streak] ?: 0,
            lastCompletionDay = p[Keys.lastCompletionDay],
            reminderEnabled = p[Keys.reminderEnabled] ?: true,
            reminderHour = p[Keys.reminderHour] ?: 9,
            reminderMinute = p[Keys.reminderMinute] ?: 0,
            exploredCategories = p[Keys.explored] ?: emptySet()
        )
        val next = transform(current)
        p[Keys.onboarded] = next.onboarded
        p[Keys.interests] = next.interests.toSet()
        p[Keys.journeys] = next.journeys.map { JourneySerializer.encode(it) }.toSet()
        p[Keys.completedTips] = next.completedTipIds
        p[Keys.savedTips] = next.savedTipIds
        p[Keys.streak] = next.streak
        next.lastCompletionDay?.let { p[Keys.lastCompletionDay] = it }
        p[Keys.reminderEnabled] = next.reminderEnabled
        p[Keys.reminderHour] = next.reminderHour
        p[Keys.reminderMinute] = next.reminderMinute
        p[Keys.explored] = next.exploredCategories
    }

    suspend fun wipeAll() = context.dataStore.edit { it.clear() }
}

private object JourneySerializer {
    fun encode(j: Journey): String =
        listOf(j.categoryId, j.length, j.startDateEpochDay, j.completedTipIds.joinToString("~"))
            .joinToString("|")

    fun decode(s: String): Journey? {
        val parts = s.split("|")
        if (parts.size != 4) return null
        return Journey(
            categoryId = parts[0],
            length = parts[1].toIntOrNull() ?: return null,
            startDateEpochDay = parts[2].toLongOrNull() ?: return null,
            completedTipIds = parts[3].split("~").filter { it.isNotBlank() }.toSet()
        )
    }
}
