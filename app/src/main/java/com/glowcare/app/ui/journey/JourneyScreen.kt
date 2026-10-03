package com.glowcare.app.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glowcare.app.AppViewModel
import com.glowcare.app.R
import com.glowcare.app.data.Category
import com.glowcare.app.data.Journey
import com.glowcare.app.data.TipLibrary
import com.glowcare.app.ui.components.GlowProgressBar
import com.glowcare.app.ui.components.SectionTitle
import com.glowcare.app.ui.components.TipCard

/** Day-by-day view of one active program. */
@Composable
fun JourneyScreen(categoryId: String, vm: AppViewModel, onBack: () -> Unit, onOpenTip: (String) -> Unit) {
    val category = Category.byId(categoryId)
    val journey = vm.state.activeJourneys.firstOrNull { it.categoryId == categoryId }
        ?: run {
            // Finished or missing — show finished view
            val done = vm.state.journeys.firstOrNull { it.categoryId == categoryId }
            FinishedJourney(category, done?.length ?: 7, onBack)
            return
        }
    val todayTipRaw = TipLibrary.tipForDay(category, journey.currentDay)
    val todayTip = todayTipRaw?.let { TipLibrary.localized(it) }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Column(Modifier.weight(1f)) {
                    Text("${category.title} Journey", style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.journey_day_x_of_y, journey.currentDay, journey.length),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            GlowProgressBar(
                journey.completedTipIds.size.toFloat() / journey.length, category.accent)
        }

        todayTip?.let { tip ->
            item {
                TipCard(
                    tip = tip,
                    dayLabel = "DAY ${journey.currentDay} OF ${journey.length}",
                    completed = tip.id in vm.state.completedTipIds,
                    saved = tip.id in vm.state.savedTipIds,
                    onOpen = { onOpenTip(tip.id) },
                    onComplete = { vm.completeTip(tip) },
                    onSave = { vm.toggleSave(tip.id) }
                )
            }
        }

        item { SectionTitle(stringResource(R.string.your_program)) }
        itemsIndexed((1..journey.length).toList()) { _, day ->
            val tip = TipLibrary.tipForDay(category, day)
            val done = tip?.id in journey.completedTipIds
            val unlocked = day <= journey.currentDay
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (done) category.accent.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    val icon = when {
                        done -> Icons.Filled.CheckCircle
                        unlocked -> null
                        else -> Icons.Filled.Lock
                    }
                    if (icon != null) {
                        Icon(icon, null, tint = category.accent)
                        Spacer(Modifier.padding(4.dp))
                    } else {
                        Surface(shape = RoundedCornerShape(10.dp),
                            color = category.accent.copy(alpha = 0.15f)) {
                            Text("Day $day", Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.padding(4.dp))
                    Column {
                        Text(tip?.title ?: stringResource(R.string.coming_soon),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (unlocked) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                        if (unlocked && tip != null) {
                            Text(stringResource(R.string.tap_today_tip),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinishedJourney(category: Category, length: Int, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.journey_finished, length, category.title),
            style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.journey_finished_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        androidx.compose.material3.Button(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}
