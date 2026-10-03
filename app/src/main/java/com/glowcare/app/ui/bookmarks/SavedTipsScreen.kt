package com.glowcare.app.ui.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glowcare.app.AppViewModel
import com.glowcare.app.R
import com.glowcare.app.data.TipLibrary
import com.glowcare.app.ui.components.EmptyState
import com.glowcare.app.ui.components.TipCard

@Composable
fun SavedTipsScreen(vm: AppViewModel, onBack: () -> Unit, onOpenTip: (String) -> Unit) {
    val state = vm.state
    val saved = state.savedTipIds.mapNotNull { TipLibrary.tipById(it) }.map { TipLibrary.localized(it) }

    if (saved.isEmpty()) {
        androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
            Text(stringResource(R.string.my_saved_tips), style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
            EmptyState("♡", stringResource(R.string.no_saved_tips),
                stringResource(R.string.no_saved_tips_sub))
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(stringResource(R.string.my_saved_tips), style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            if (!vm.premium) {
                Text(stringResource(R.string.saved_count_free, state.savedTipIds.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(saved, key = { it.id }) { tip ->
            TipCard(
                tip = tip,
                completed = tip.id in state.completedTipIds,
                saved = true,
                onOpen = { onOpenTip(tip.id) },
                onComplete = { vm.completeTip(tip) },
                onSave = { vm.toggleSave(tip.id) }
            )
        }
    }
}
