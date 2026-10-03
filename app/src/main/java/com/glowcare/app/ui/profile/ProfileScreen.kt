package com.glowcare.app.ui.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glowcare.app.AppViewModel
import com.glowcare.app.R
import com.glowcare.app.data.LanguagePrefs
import com.glowcare.app.ui.components.SectionTitle
import com.glowcare.app.ui.onboarding.ReminderTimePicker
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    vm: AppViewModel,
    onOpenPremium: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val state = vm.state
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showReset by remember { mutableStateOf(false) }
    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= 33)
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notifGranted = it }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(stringResource(R.string.nav_profile), style = MaterialTheme.typography.headlineMedium) }

        // ---- Account ----
        item {
            Card(shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(18.dp)) {
                    val p = vm.profile
                    if (p.isSignedIn) {
                        Text(p.displayName ?: p.email ?: "",
                            style = MaterialTheme.typography.titleMedium)
                        p.email?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { vm.signOut() }) {
                            Text(stringResource(R.string.sign_out))
                        }
                    } else {
                        Text(stringResource(R.string.guest_user),
                            style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.onboarding_login_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { vm.signInWithGoogle(context) { } }) {
                            Text(stringResource(R.string.continue_with_google))
                        }
                    }
                }
            }
        }

        // ---- Subscription status (verified by Google Play) ----
        item {
            Card(shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (vm.premium) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(18.dp)) {
                    val trialDays = if (vm.isOwner) null else vm.trialDaysRemaining()
                    Text(
                        when {
                            vm.isOwner -> stringResource(R.string.owner_title)
                            vm.premium && trialDays != null && trialDays > 0 ->
                                stringResource(R.string.trial_days_remaining, trialDays)
                            vm.premium -> stringResource(R.string.premium_active)
                            else -> stringResource(R.string.free_plan)
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            vm.isOwner -> stringResource(R.string.owner_access)
                            vm.premium -> stringResource(R.string.premium_member_details)
                            else -> stringResource(R.string.free_plan_details)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { com.glowcare.app.ui.premium.openManageSubscription(context) }) {
                        Text(stringResource(R.string.manage_subscription))
                    }
                }
            }
        }

        // ---- Language ----
        item { SectionTitle(stringResource(R.string.language)) }
        item {
            Card(shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(16.dp)) {
                    listOf(
                        "en" to stringResource(R.string.language_english),
                        "hi" to stringResource(R.string.language_hindi)
                    ).forEach { (code, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Text(
                                if (LanguagePrefs.current() == code) "●" else "○",
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.padding(6.dp))
                            Text(label, style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier
                                    .weight(1f)
                                    .let { m ->
                                        m.then(
                                            Modifier.padding(vertical = 4.dp)
                                        )
                                    },
                                color = MaterialTheme.colorScheme.onSurface)
                            TextButton(onClick = { scope.launch { LanguagePrefs.set(code) } }) {
                                Text(if (LanguagePrefs.current() == code) stringResource(R.string.done)
                                     else stringResource(R.string.next))
                            }
                        }
                    }
                }
            }
        }

        // ---- Reminder ----
        item { SectionTitle(stringResource(R.string.daily_reminder)) }
        item {
            Card(shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.notifications), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.notifications_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = state.reminderEnabled,
                            onCheckedChange = { on ->
                                if (on && Build.VERSION.SDK_INT >= 33 && !notifGranted) {
                                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                vm.setReminder(on, state.reminderHour, state.reminderMinute)
                            }
                        )
                    }
                    if (state.reminderEnabled) {
                        Spacer(Modifier.height(12.dp))
                        ReminderTimePicker(
                            hour = state.reminderHour,
                            minute = state.reminderMinute
                        ) { h, m -> vm.setReminder(true, h, m) }
                    }
                }
            }
        }

        // ---- More ----
        item { SectionTitle(stringResource(R.string.more)) }
        item {
            SettingsRow(stringResource(R.string.my_saved_tips), onOpenSaved)
            SettingsRow("🔒 " + stringResource(R.string.privacy), onOpenPrivacy)
        }

        // ---- Danger zone ----
        item { SectionTitle(stringResource(R.string.danger_zone)) }
        item {
            OutlinedButton(
                onClick = { showReset = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.reset_all_data)) }
        }
        item {
            Text(stringResource(R.string.version_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(stringResource(R.string.reset_confirm_title)) },
            text = { Text(stringResource(R.string.reset_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { showReset = false; vm.resetAll() }) {
                    Text(stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsRow(label: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Text(label, Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall)
    }
}
