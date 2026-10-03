package com.glowcare.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glowcare.app.R
import com.glowcare.app.data.Category
import com.glowcare.app.data.Journey
import com.glowcare.app.data.Tip

/** One consistent, theme-independent text system for all pastel category cards. */
private val CategoryCardTitleColor = Color(0xFF3B2A31)     // dark plum — highly readable
private val CategoryCardSubtitleColor = Color(0xFF6B585F)  // lighter, still clearly readable

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        modifier = modifier.padding(vertical = 12.dp)
    )
}

@Composable
fun TipCard(
    tip: Tip,
    dayLabel: String? = null,
    completed: Boolean = false,
    saved: Boolean = false,
    onOpen: () -> Unit,
    onComplete: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = tip.category.accent.copy(alpha = 0.15f)
                ) {
                    Text(tip.category.emoji, Modifier.padding(8.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    dayLabel?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Text(stringResource(tip.category.titleRes), style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onSave) {
                    Icon(
                        if (saved) Icons.Filled.Bookmark else Icons.Outlined.Bookmark,
                        contentDescription = if (saved) "Remove bookmark" else "Save tip",
                        tint = if (saved) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(tip.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(tip.short, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.clickable(onClick = onOpen)
                ) {
                    Text(stringResource(R.string.read_more), Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (completed) tip.category.accent.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onComplete)
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp),
                            tint = if (completed) tip.category.accent else Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (completed) stringResource(R.string.completed) else stringResource(R.string.mark_complete),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (completed) MaterialTheme.colorScheme.onSurface
                                    else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(category: Category, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(category.gradient))
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(category.emoji, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    // Cards always use pastel gradients in both light and dark
                    // themes, so text needs fixed dark tones — not theme colors,
                    // which turn pale in dark mode.
                    Text(stringResource(category.titleRes), style = MaterialTheme.typography.titleMedium,
                        color = CategoryCardTitleColor)
                    Text(stringResource(category.subtitleRes), style = MaterialTheme.typography.bodySmall,
                        color = CategoryCardSubtitleColor)
                }
            }
        }
    }
}

@Composable
fun JourneyCard(journey: Journey, category: Category, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(category.emoji)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.care_suffix, stringResource(category.titleRes)), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
                Icon(Icons.Filled.LocalFireDepartment, null,
                    tint = category.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("${journey.currentDay} / ${journey.length}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            GlowProgressBar(
                progress = journey.completedTipIds.size.toFloat() / journey.length,
                color = category.accent
            )
        }
    }
}

@Composable
fun GlowProgressBar(progress: Float, color: Color = MaterialTheme.colorScheme.primary) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = color,
        trackColor = color.copy(alpha = 0.15f)
    )
}

@Composable
fun PremiumCard(onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.premium_title), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.premium_member_details),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(stringResource(R.string.see_plans), Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
