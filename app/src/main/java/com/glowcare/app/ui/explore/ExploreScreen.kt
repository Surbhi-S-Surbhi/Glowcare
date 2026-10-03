package com.glowcare.app.ui.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glowcare.app.AppViewModel
import com.glowcare.app.R
import com.glowcare.app.data.Category
import com.glowcare.app.ui.components.CategoryCard
import com.glowcare.app.ui.components.SectionTitle

@Composable
fun ExploreScreen(vm: AppViewModel, onOpenCategory: (String) -> Unit, onOpenSearch: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val categories = Category.entries.filter {
        query.isBlank() || it.title.contains(query, ignoreCase = true) ||
            it.subtitle.contains(query, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(stringResource(R.string.nav_explore), style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, "Search all tips")
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionTitle(stringResource(R.string.all_categories)) }
            items(categories, key = { it.id }) { c ->
                CategoryCard(c, onClick = {
                    vm.markExplored(c)
                    onOpenCategory(c.id)
                })
            }
        }
    }
}
