package com.glowcare.app.ui.search

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glowcare.app.R
import com.glowcare.app.data.TipLibrary
import com.glowcare.app.ui.components.EmptyState
import com.glowcare.app.ui.components.TipCard

@Composable
fun SearchScreen(vm: com.glowcare.app.AppViewModel, onBack: () -> Unit, onOpenTip: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = TipLibrary.search(query).map { TipLibrary.localized(it) }
    val suggestions = TipLibrary.searchSuggestions()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (query.length < 2) {
                item {
                    Text(stringResource(R.string.try_searching), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    suggestions.forEach { s ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .padding(vertical = 3.dp)
                                .fillMaxWidth(),
                            onClick = { query = s }
                        ) {
                            Text(s, Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.search_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (results.isEmpty()) {
                item {
                    EmptyState("🔍", stringResource(R.string.no_tips_found), stringResource(R.string.no_tips_found_sub))
                }
            } else {
                item {
                    Text(if (results.size == 1) stringResource(R.string.tip_found)
                        else stringResource(R.string.tips_found, results.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(results, key = { it.id }) { tip ->
                    TipCard(
                        tip = tip,
                        completed = tip.id in vm.state.completedTipIds,
                        saved = tip.id in vm.state.savedTipIds,
                        onOpen = { onOpenTip(tip.id) },
                        onComplete = { vm.completeTip(tip) },
                        onSave = { vm.toggleSave(tip.id) }
                    )
                }
            }
        }
    }
}
