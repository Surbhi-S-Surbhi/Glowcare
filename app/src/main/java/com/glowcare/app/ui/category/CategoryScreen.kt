package com.glowcare.app.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.glowcare.app.AppViewModel
import com.glowcare.app.R
import com.glowcare.app.data.Category
import com.glowcare.app.data.TipLibrary
import com.glowcare.app.ui.components.JourneyCard
import com.glowcare.app.ui.components.SectionTitle
import com.glowcare.app.ui.components.TipCard

/** Category detail: learn (tip list) + start a 7/14/30-day journey. */
@Composable
fun CategoryScreen(
    categoryId: String,
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenTip: (String) -> Unit,
    onOpenPremium: () -> Unit,
    onOpenJourney: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val category = Category.byId(categoryId)
    val tips = TipLibrary.tipsFor(category).map { TipLibrary.localized(it) }
    val state = vm.state
    val journey = state.journeys.firstOrNull { it.categoryId == categoryId && !it.isFinished }
    var showLengthDialog by remember { mutableStateOf(false) }
    var blockedMessage by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    Text(category.emoji, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(stringResource(category.titleRes), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(category.subtitleRes), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (journey != null) {
                item {
                    SectionTitle(stringResource(R.string.journey_in_progress))
                    JourneyCard(journey, category, onClick = { onOpenJourney(categoryId) })
                }
            }

            item { SectionTitle(stringResource(R.string.start_journey)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(7, 14, 30).forEach { days ->
                        Button(
                            onClick = {
                                if (!vm.premium && days != 7) {
                                    blockedMessage = context.getString(R.string.free_seven_day)
                                } else {
                                    vm.startJourney(category, days, onBlocked = {
                                        blockedMessage = context.getString(R.string.free_one_journey)
                                    })
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (days == 7 || vm.premium)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("${days}D", style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }

            item { SectionTitle(stringResource(R.string.learn)) }
            items(tips, key = { it.id }) { tip ->
                TipCard(
                    tip = tip,
                    completed = tip.id in state.completedTipIds,
                    saved = tip.id in state.savedTipIds,
                    onOpen = { onOpenTip(tip.id) },
                    onComplete = { vm.completeTip(tip) },
                    onSave = { vm.toggleSave(tip.id) }
                )
            }
        }
    }

    if (blockedMessage != null) {
        AlertDialog(
            onDismissRequest = { blockedMessage = null },
            title = { Text(stringResource(R.string.upgrade_title)) },
            text = { Text(blockedMessage!!) },
            confirmButton = {
                TextButton(onClick = { blockedMessage = null; onOpenPremium() }) { Text(stringResource(R.string.see_plans)) }
            },
            dismissButton = {
                TextButton(onClick = { blockedMessage = null }) { Text(stringResource(R.string.maybe_later)) }
            }
        )
    }
}
