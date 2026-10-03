package com.glowcare.app.ui.progress

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glowcare.app.AppViewModel
import com.glowcare.app.R
import com.glowcare.app.data.Category
import com.glowcare.app.ui.components.SectionTitle
import java.time.LocalDate

private data class Stat(val emoji: String, val value: String, val label: String)

@Composable
fun ProgressScreen(vm: AppViewModel) {
    val state = vm.state
    val stats = listOf(
        Stat("🌷", state.completedJourneys.size.toString(), stringResource(R.string.programs_completed)),
        Stat("✨", state.completedTipIds.size.toString(), stringResource(R.string.tips_completed)),
        Stat("🌸", state.exploredCategories.size.toString(), stringResource(R.string.categories_explored))
    )

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(stringResource(R.string.self_care_journey), style = MaterialTheme.typography.headlineMedium) }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().animateContentSize()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("🔥 ${state.streak}",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.current_streak),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                stats.forEach { s ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(s.emoji)
                            Spacer(Modifier.height(4.dp))
                            Text(s.value, style = MaterialTheme.typography.headlineSmall)
                            Text(s.label, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item { SectionTitle(stringResource(R.string.this_week)) }
        item { WeekCard(vm) }

        item { SectionTitle(stringResource(R.string.completed_programs)) }
        if (state.completedJourneys.isEmpty()) {
            item {
                Text(stringResource(R.string.finish_journey_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            state.completedJourneys.forEach { j ->
                item {
                    val cat = Category.byId(j.categoryId)
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = cat.accent.copy(alpha = 0.12f))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("${cat.emoji} ${cat.title} — ${j.length} days",
                                style = MaterialTheme.typography.titleMedium)
                            val start = LocalDate.ofEpochDay(j.startDateEpochDay)
                            Text(
                                stringResource(R.string.started_fmt,
                                    start.month.name.lowercase().replaceFirstChar { it.uppercase() },
                                    start.dayOfMonth, start.year),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 7-day activity strip: today fills in once a tip has been completed. */
@Composable
private fun WeekCard(vm: AppViewModel) {
    val state = vm.state
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val completedToday = state.activeJourneys.any { j ->
        TipForDay.isCompletedToday(j.categoryId, j, state)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                days.forEach { day ->
                    val label = day.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .padding(2.dp)
                                .height(28.dp)
                                .fillMaxWidth(0.10f)
                                .background(
                                    if (day == today && completedToday) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            val streakText = if (state.streak > 0) "🔥 ${state.streak} ${stringResource(R.string.day_streak)}! " else ""
            Text(streakText + stringResource(R.string.fill_your_week),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private object TipForDay {
    fun isCompletedToday(categoryId: String, journey: com.glowcare.app.data.Journey,
                         state: com.glowcare.app.data.UserState): Boolean {
        val cat = Category.byId(categoryId)
        val tip = com.glowcare.app.data.TipLibrary.tipForDay(cat, journey.currentDay) ?: return false
        return tip.id in journey.completedTipIds
    }
}
