package com.glowcare.app.ui.tip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glowcare.app.AppViewModel
import com.glowcare.app.R
import com.glowcare.app.data.TipLibrary

@Composable
fun TipDetailScreen(tipId: String, vm: AppViewModel, onBack: () -> Unit) {
    val tip = TipLibrary.localized(TipLibrary.tipById(tipId) ?: run { onBack(); return })
    val state = vm.state
    val completed = tip.id in state.completedTipIds
    val saved = tip.id in state.savedTipIds

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(tip.category.emoji)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(tip.category.titleRes), style = MaterialTheme.typography.titleMedium,
                color = tip.category.accent, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.toggleSave(tip.id) }) {
                Icon(if (saved) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                    if (saved) "Remove bookmark" else "Save tip",
                    tint = if (saved) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text(tip.title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, null, Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.min_read, tip.readMinutes), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = tip.category.accent.copy(alpha = 0.10f))
        ) {
            Text(tip.short, Modifier.padding(18.dp), style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(20.dp))

        Text(stringResource(R.string.why_it_matters), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(tip.detail, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        Text(stringResource(R.string.do_today), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Card(shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))) {
            Text(tip.action, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(20.dp))

        Text(stringResource(R.string.common_mistake), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Card(shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(10.dp))
                Text(tip.mistake, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        tip.safety?.let {
            Spacer(Modifier.height(16.dp))
            Card(shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Text(
                    stringResource(R.string.see_professional, it),
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        if (completed) {
            Surface(shape = RoundedCornerShape(16.dp),
                color = tip.category.accent.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = tip.category.accent,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.completed_banner), style = MaterialTheme.typography.labelLarge)
                }
            }
        } else {
            Button(
                onClick = { vm.completeTip(tip) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) { Text(stringResource(R.string.mark_complete)) }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { vm.toggleSave(tip.id) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) { Text(if (saved) stringResource(R.string.remove_saved) else stringResource(R.string.save_tip)) }
        Spacer(Modifier.height(40.dp))
    }
}

private val contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 100.dp)
