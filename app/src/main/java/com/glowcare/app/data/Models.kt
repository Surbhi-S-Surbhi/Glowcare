package com.glowcare.app.data

import androidx.compose.ui.graphics.Color
import com.glowcare.app.R

/** All self-care categories offered by the app. */
enum class Category(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val accent: Color,
    val gradient: List<Color>,
    val titleRes: Int,
    val subtitleRes: Int
) {
    HAIR("hair", "Hair", "Wash, scalp & styling care", "💇‍♀️", Color(0xFFB07391), listOf(Color(0xFFF7DCE2), Color(0xFFF1E4EC)), R.string.cat_hair, R.string.cat_hair_sub),
    SKIN("skin", "Face & Skin", "Cleanse, moisturize, protect", "✨", Color(0xFF9A7BA6), listOf(Color(0xFFEBDDF3), Color(0xFFF6EEF9)), R.string.cat_skin, R.string.cat_skin_sub),
    ORAL("oral", "Oral Care", "Teeth, gums & fresh breath", "🪥", Color(0xFF6E93A8), listOf(Color(0xFFDCE9F0), Color(0xFFF0F6FA)), R.string.cat_oral, R.string.cat_oral_sub),
    BODY("body", "Body Hygiene", "Shower & daily freshness", "🧴", Color(0xFF8FA98A), listOf(Color(0xFFE3EEDF), Color(0xFFF2F8EF)), R.string.cat_body, R.string.cat_body_sub),
    NAILS("nails", "Hands & Nails", "Clean, trim & care", "💅", Color(0xFFC58A72), listOf(Color(0xFFF6E3D8), Color(0xFBF1EA)), R.string.cat_nails, R.string.cat_nails_sub),
    FEET("feet", "Feet", "Soft, clean & happy feet", "🦶", Color(0xFF7E8FB0), listOf(Color(0xFFE1E5F2), Color(0xFFF1F3FA)), R.string.cat_feet, R.string.cat_feet_sub),
    UNDERARM("underarm", "Underarm Care", "Gentle daily care", "🌿", Color(0xFF89A893), listOf(Color(0xFFE0EEE4), Color(0xF0F7F2)), R.string.cat_underarm, R.string.cat_underarm_sub),
    GROOMING("grooming", "Personal Grooming", "Polished everyday habits", "🪞", Color(0xFFA8849C), listOf(Color(0xFFF0E0EB), Color(0xF8EFF4)), R.string.cat_grooming, R.string.cat_grooming_sub),
    MENSTRUAL("menstrual", "Menstrual Hygiene", "Comfort & confidence", "🌸", Color(0xFFC2707F), listOf(Color(0xFFF8DCE1), Color(0xFBEDEF)), R.string.cat_menstrual, R.string.cat_menstrual_sub),
    SLEEP("sleep", "Sleep & Recovery", "Rest that restores", "🌙", Color(0xFF7B7FA8), listOf(Color(0xFFDEE0F0), Color(0xFFF0F1F9)), R.string.cat_sleep, R.string.cat_sleep_sub),
    NUTRITION("nutrition", "Nutrition & Hydration", "Feed your glow", "🥗", Color(0xFF8FA06A), listOf(Color(0xFFE8EEDA), Color(0xFFF3F7EC)), R.string.cat_nutrition, R.string.cat_nutrition_sub),
    FITNESS("fitness", "Fitness & Movement", "Move gently, feel strong", "🤸‍♀️", Color(0xFFC08A67), listOf(Color(0xFFF8E5D4), Color(0xF8F0E6)), R.string.cat_fitness, R.string.cat_fitness_sub),
    SELFCARE("selfcare", "Self-Care", "Time just for you", "💗", Color(0xFFC4707F), listOf(Color(0xFFF9DEE3), Color(0xFBEFF1)), R.string.cat_selfcare, R.string.cat_selfcare_sub),
    EVERYDAY("everyday", "Everyday Beauty", "Quick daily wins", "🌷", Color(0xFF9C7BA9), listOf(Color(0xFFEDE0F2), Color(0xF7F0F9)), R.string.cat_everyday, R.string.cat_everyday_sub);

companion object {
        fun byId(id: String): Category = entries.first { it.id == id }
    }
}

/** A single educational tip. */
data class Tip(
    val id: String,
    val category: Category,
    val title: String,
    val short: String,
    val detail: String,
    val action: String,
    val mistake: String,
    val safety: String? = null,
    val readMinutes: Int = 2
)

/** An active multi-day journey the user started. */
data class Journey(
    val categoryId: String,
    val length: Int,
    val startDateEpochDay: Long,
    val completedTipIds: Set<String> = emptySet()
) {
    val currentDay: Int
        get() = ((todayEpochDay - startDateEpochDay) + 1).coerceIn(1L, length.toLong()).toInt()

    val isFinished: Boolean get() = currentDay >= length && completedTipIds.size >= length

    companion object {
        val todayEpochDay: Long get() = java.time.LocalDate.now().toEpochDay()
    }
}

/** Everything persisted between launches. */
data class UserState(
    val onboarded: Boolean = false,
    val interests: List<String> = emptyList(),
    val journeys: List<Journey> = emptyList(),
    val completedTipIds: Set<String> = emptySet(),
    val savedTipIds: Set<String> = emptySet(),
    val streak: Int = 0,
    val lastCompletionDay: Long? = null,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val exploredCategories: Set<String> = emptySet()
) {
    val activeJourneys: List<Journey> get() = journeys.filter { !it.isFinished }
    val completedJourneys: List<Journey> get() = journeys.filter { it.isFinished }

    fun withTipCompleted(tipId: String): UserState {
        val newCompleted = completedTipIds + tipId
        val today = Journey.todayEpochDay
        val newStreak = when (lastCompletionDay) {
            today -> streak
            today - 1 -> streak + 1
            else -> 1
        }
        return copy(
            completedTipIds = newCompleted,
            streak = newStreak,
            lastCompletionDay = today
        )
    }
}
