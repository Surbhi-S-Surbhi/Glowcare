package com.glowcare.app.ui.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glowcare.app.AppViewModel
import com.glowcare.app.R
import com.glowcare.app.data.Category
import com.glowcare.app.data.LanguagePrefs
import kotlinx.coroutines.launch

/** 6-step onboarding: welcome → language → interests → sign-in → plans → reminder. */
@Composable
fun OnboardingScreen(vm: AppViewModel, onDone: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusOptions = listOf(
        Category.HAIR, Category.SKIN, Category.BODY, Category.NAILS, Category.ORAL,
        Category.FEET, Category.GROOMING, Category.MENSTRUAL, Category.SLEEP,
        Category.FITNESS, Category.NUTRITION, Category.SELFCARE
    )
    var selected by rememberSaveable { mutableStateOf(ArrayList<String>()) }
    var language by rememberSaveable { mutableStateOf(LanguagePrefs.current()) }
    var signInError by remember { mutableStateOf<String?>(null) }
    var reminderOn by rememberSaveable { mutableStateOf(true) }
    var hour by rememberSaveable { mutableIntStateOf(9) }
    var minute by rememberSaveable { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                )
            )
            .padding(24.dp)
    ) {
        Box(Modifier.weight(1f)) {
            Crossfade(targetState = step, label = "onboarding") { s ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (s) {
                        0 -> WelcomeStep()
                        1 -> LanguageStep(language) { language = it }
                        2 -> InterestsStep(focusOptions, selected.toSet()) { id ->
                            selected = if (selected.contains(id)) ArrayList(selected.filter { it != id })
                            else ArrayList(selected + id)
                        }
                        3 -> LoginStep(vm, signInError, { signInError = it }, onNext = { step = 4 })
                        4 -> PlansStep()
                        5 -> ReminderStep(reminderOn, { reminderOn = it }, hour, minute) { h, m -> hour = h; minute = m }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            if (step > 0 && step != 4) {
                OutlinedButton(onClick = { step-- }) { Text(stringResource(R.string.back)) }
            } else Spacer(Modifier.width(1.dp))
            Row {
                repeat(6) { i ->
                    Box(
                        Modifier
                            .padding(3.dp)
                            .size(if (i == step) 10.dp else 8.dp, 8.dp)
                            .background(
                                if (i == step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                CircleShape
                            )
                    )
                }
            }
            if (step != 3) {
                Button(
                    onClick = {
                        when (step) {
                            1 -> {
                                scope.launch { LanguagePrefs.set(language) }
                                step++
                            }
                            5 -> {
                                vm.completeOnboarding(selected.toList(), reminderOn, hour, minute)
                                onDone()
                            }
                            else -> step++
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        stringResource(
                            when (step) {
                                0 -> R.string.onboarding_get_started
                                5 -> R.string.start_my_journey
                                else -> R.string.next
                            }
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.ArrowForward, null, Modifier.size(16.dp))
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Spacer(Modifier.height(80.dp))
    Box(
        Modifier
            .size(110.dp)
            .background(
                Brush.linearGradient(listOf(Color(0xFFF9DEE3), Color(0xFFEBDDF3))),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) { Text("🌸", style = MaterialTheme.typography.displaySmall) }
    Spacer(Modifier.height(24.dp))
    Text("GlowCare", style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.onboarding_headline),
        style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.onboarding_sub),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

@Composable
private fun LanguageStep(current: String, pick: (String) -> Unit) {
    Spacer(Modifier.height(48.dp))
    Text(stringResource(R.string.onboarding_language_title),
        style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.onboarding_language_sub),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
    listOf("en" to stringResource(R.string.language_english),
           "hi" to stringResource(R.string.language_hindi)).forEach { (code, label) ->
        val on = current == code
        Row(
            Modifier
                .fillMaxWidth()
                .border(
                    1.5.dp,
                    if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(18.dp)
                )
                .background(
                    if (on) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)
                )
                .clickable { pick(code) }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (on) "✓" else "○", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun InterestsStep(options: List<Category>, selected: Set<String>, toggle: (String) -> Unit) {
    Spacer(Modifier.height(40.dp))
    Text(stringResource(R.string.onboarding_interests_title),
        style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.onboarding_interests_sub),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { c ->
                    val isOn = c.id in selected
                    Column(
                        Modifier
                            .weight(1f)
                            .border(
                                1.5.dp,
                                if (isOn) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(16.dp)
                            )
                            .background(
                                if (isOn) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { toggle(c.id) }
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(c.emoji)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(c.titleRes), style = MaterialTheme.typography.titleSmall)
                        if (isOn) {
                            Spacer(Modifier.height(4.dp))
                            Icon(Icons.Filled.Check, null, Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LoginStep(
    vm: AppViewModel, lastError: String?,
    onError: (String?) -> Unit, onNext: () -> Unit
) {
    val context = LocalContext.current
    Spacer(Modifier.height(48.dp))
    Text(stringResource(R.string.onboarding_login_title),
        style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.onboarding_login_sub),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(32.dp))

    Button(
        onClick = {
            vm.signInWithGoogle(context) { result ->
                onError(result.exceptionOrNull()?.message)
            }
        },
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp)) {
            Text("G", color = Color(0xFF4285F4), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.continue_with_google),
                color = Color(0xFF202124), style = MaterialTheme.typography.titleSmall)
        }
    }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = { vm.continueAsGuest() },
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) { Text(stringResource(R.string.continue_as_guest)) }

    // Advance as soon as the user has signed in or explicitly chosen guest.
    LaunchedEffect(vm.profile.chosen) {
        if (vm.profile.chosen) { onError(null); onNext() }
    }

    lastError?.let {
        Spacer(Modifier.height(16.dp))
        Text(it, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PlansStep() {
    Spacer(Modifier.height(48.dp))
    Text(stringResource(R.string.onboarding_plans_title),
        style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(20.dp))
    InfoCard(stringResource(R.string.onboarding_plans_free))
    Spacer(Modifier.height(12.dp))
    InfoCard(stringResource(R.string.onboarding_plans_premium), highlight = true)
}

@Composable
private fun InfoCard(text: String, highlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReminderStep(
    on: Boolean, setOn: (Boolean) -> Unit,
    hour: Int, minute: Int, setTime: (Int, Int) -> Unit
) {
    Spacer(Modifier.height(48.dp))
    Text(stringResource(R.string.onboarding_reminder_title),
        style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.onboarding_reminder_quote),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(28.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.daily_notification), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.onboarding_reminder_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = on, onCheckedChange = setOn)
    }
    if (on) {
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.onboarding_reminder_at), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            TimeSpinner(0..23, hour) { h -> setTime(h, minute) }
            Text(":", style = MaterialTheme.typography.headlineMedium)
            TimeSpinner(0..59, minute) { m -> setTime(hour, m) }
        }
    }
}

@Composable
private fun TimeSpinner(range: IntRange, current: Int, onPick: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.KeyboardArrowUp, "increase",
            Modifier
                .size(32.dp)
                .clickable { onPick(if (current == range.last) range.first else current + 1) },
            tint = MaterialTheme.colorScheme.primary)
        Text(String.format("%02d", current), style = MaterialTheme.typography.headlineMedium)
        Icon(Icons.Filled.KeyboardArrowDown, "decrease",
            Modifier
                .size(32.dp)
                .clickable { onPick(if (current == range.first) range.last else current - 1) },
            tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ReminderTimePicker(hour: Int, minute: Int, onPick: (Int, Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        TimeSpinner(0..23, hour) { h -> onPick(h, minute) }
        Text(":", style = MaterialTheme.typography.headlineMedium)
        TimeSpinner(0..59, minute) { m -> onPick(hour, m) }
    }
}
