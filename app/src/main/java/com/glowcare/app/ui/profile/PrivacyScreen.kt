package com.glowcare.app.ui.profile

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.glowcare.app.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.glowcare.app.ui.components.SectionTitle

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Text(stringResource(R.string.privacy), style = MaterialTheme.typography.headlineMedium)
            }
        }
        item {
            Text(stringResource(R.string.privacy_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            PrivacyCard(
                Icons.Filled.CameraAlt,
                stringResource(R.string.privacy_camera_title),
                stringResource(R.string.privacy_camera_body)
            )
        }
        item {
            PrivacyCard(
                Icons.Filled.Storage,
                stringResource(R.string.privacy_photos_title),
                stringResource(R.string.privacy_photos_body)
            )
        }
        item {
            PrivacyCard(
                Icons.Filled.HealthAndSafety,
                stringResource(R.string.privacy_minimal_title),
                stringResource(R.string.privacy_minimal_body)
            )
        }
        item {
            PrivacyCard(
                Icons.Filled.Delete,
                stringResource(R.string.privacy_control_title),
                stringResource(R.string.privacy_control_body)
            )
        }
        item {
            SectionTitle(stringResource(R.string.privacy_permissions_title))
            Text(
                stringResource(R.string.privacy_permissions_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SectionTitle(stringResource(R.string.privacy_account_title))
            Text(stringResource(R.string.privacy_account_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            SectionTitle(stringResource(R.string.privacy_billing_title))
            Text(stringResource(R.string.privacy_billing_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PrivacyCard(icon: ImageVector, title: String, body: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(18.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
