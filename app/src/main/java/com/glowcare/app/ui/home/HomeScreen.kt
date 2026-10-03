package com.glowcare.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glowcare.app.AppViewModel
import com.glowcare.app.R
import com.glowcare.app.data.Category
import com.glowcare.app.data.TipLibrary
import com.glowcare.app.ui.components.CategoryCard
import com.glowcare.app.ui.components.EmptyState
import com.glowcare.app.ui.components.JourneyCard
import com.glowcare.app.ui.components.PremiumCard
import com.glowcare.app.ui.components.SectionTitle
import com.glowcare.app.ui.components.TipCard
import java.time.LocalTime

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpenTip: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenJourney: (String) -> Unit
) {
    val state = vm.state
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> stringResource(R.string.greeting_morning)
        in 12..17 -> stringResource(R.string.greeting_afternoon)
        else -> stringResource(R.string.greeting_evening)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth()) {
                Text(greeting, style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Filled.Search, "Search tips")
                }
            }
        }

        val today = vm.todayTips().map { TipLibrary.localized(it) }
        if (today.isEmpty()) {
            item {
                Surface(shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.start_todays_tip), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.start_todays_tip_sub),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item { SectionTitle(stringResource(R.string.todays_tip)) }
            items(today, key = { "today_" + it.id }) { tip ->
                val journey = state.activeJourneys.firstOrNull { it.categoryId == tip.category.id }
                TipCard(
                    tip = tip,
                    dayLabel = journey?.let { stringResource(R.string.day_x_of_y, it.currentDay, it.length) },
                    completed = tip.id in state.completedTipIds,
                    saved = tip.id in state.savedTipIds,
                    onOpen = { onOpenTip(tip.id) },
                    onComplete = { vm.completeTip(tip) },
                    onSave = { vm.toggleSave(tip.id) }
                )
            }
        }

        val journeys = state.activeJourneys
        if (journeys.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.active_programs)) }
            items(journeys, key = { "journey_" + it.categoryId + "_" + it.startDateEpochDay }) { j ->
                JourneyCard(j, Category.byId(j.categoryId), onClick = { onOpenJourney(j.categoryId) })
            }
        }

        if (!vm.premium) {
            item { PremiumCard(onOpenPremium) }
        }

        item { SectionTitle(stringResource(R.string.explore_more)) }
        val explore = Category.entries.filter { it.id !in journeys.map { j -> j.categoryId } }
        items(explore, key = { "explore_" + it.id }) { c ->
            CategoryCard(c, onClick = { onOpenCategory(c.id) })
        }

        if (state.journeys.isEmpty() && today.isEmpty()) {
            item { EmptyState("🌸", "Nothing here yet", "Start your first journey from Explore.") }
        }
    }
}
