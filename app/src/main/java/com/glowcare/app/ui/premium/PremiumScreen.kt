package com.glowcare.app.ui.premium

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glowcare.app.AppViewModel
import com.glowcare.app.R
import com.glowcare.app.billing.BillingManager

/**
 * Premium paywall. Plans, trial and purchases run through Google Play Billing —
 * this screen presents the configured catalog (₹20/month, ₹199/year, 3-day
 * free trial) and launches the real Play subscription sheet.
 */
@Composable
fun PremiumScreen(vm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var yearly by remember { mutableStateOf(true) }
    val plans by vm.billing.plans.collectAsState()
    val billingAvailable by vm.billing.billingAvailable.collectAsState()
    val premium = vm.premium

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("✨", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.unlock_title),
            style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.premium_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))

        val perks = listOf(
            R.string.benefit_journeys, R.string.benefit_library, R.string.benefit_tools,
            R.string.benefit_filters, R.string.benefit_effects, R.string.benefit_adjust,
            R.string.benefit_routines, R.string.benefit_progress, R.string.benefit_adfree,
            R.string.benefit_bilingual
        )
        perks.forEach { perk ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(5.dp))
                Text(stringResource(perk), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(20.dp))

        val monthly = plans.firstOrNull { it.productId == BillingManager.MONTHLY_PRODUCT_ID }
        val yearlyPlan = plans.firstOrNull { it.productId == BillingManager.YEARLY_PRODUCT_ID }

        PlanCard(
            title = stringResource(R.string.plan_monthly),
            price = monthly?.formattedPrice ?: stringResource(R.string.plan_monthly_price),
            note = "",
            selected = !yearly,
            badge = null,
            onClick = { yearly = false }
        )
        Spacer(Modifier.height(10.dp))
        PlanCard(
            title = stringResource(R.string.plan_yearly),
            price = yearlyPlan?.formattedPrice ?: stringResource(R.string.plan_yearly_price),
            note = stringResource(R.string.yearly_value_note),
            selected = yearly,
            badge = stringResource(R.string.best_value),
            onClick = { yearly = true }
        )

        Spacer(Modifier.height(20.dp))

        if (premium) {
            Text(stringResource(R.string.premium_member),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
        } else {
            Button(
                onClick = {
                    val productId = if (yearly) BillingManager.YEARLY_PRODUCT_ID
                                    else BillingManager.MONTHLY_PRODUCT_ID
                    if (context is android.app.Activity) {
                        vm.launchSubscription(context, productId)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.start_trial)) }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.trial_then_price),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { vm.restorePurchases() },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.restore_purchases)) }

        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.continue_free)) }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.trial_disclosure),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        if (!billingAvailable) {
            Text(stringResource(R.string.billing_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        Text(stringResource(R.string.premium_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun PlanCard(
    title: String, price: String, note: String,
    selected: Boolean, badge: String?, onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.5.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(18.dp)) {
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(badge, Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(6.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (selected) "●" else "○", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(6.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(price, style = MaterialTheme.typography.headlineSmall)
            if (note.isNotBlank()) {
                Text(note, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
