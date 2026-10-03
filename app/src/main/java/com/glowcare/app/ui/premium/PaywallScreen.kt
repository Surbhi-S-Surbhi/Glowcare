package com.glowcare.app.ui.premium

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.glowcare.app.BuildConfig
import com.glowcare.app.R
import com.glowcare.app.billing.BillingManager
import com.glowcare.app.data.LanguagePrefs
import kotlinx.coroutines.launch

/**
 * FIRST-LAUNCH PAYWALL. Shown before Home until an active Google Play
 * subscription/trial is verified. Bilingual (EN/हिं), no dark patterns:
 * real prices, real trial via Play Billing, honest disclosures.
 */
@Composable
fun PaywallScreen(vm: AppViewModel, onSubscribed: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var yearly by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var devTaps by remember { mutableIntStateOf(0) }
    val plans by vm.billing.plans.collectAsState()
    val billingAvailable by vm.billing.billingAvailable.collectAsState()

    // Purchase results arrive from BillingManager via AppViewModel.
    LaunchedEffect(Unit) {
        vm.billing.onPurchaseResult = { success ->
            message = if (success) null
                      else context.getString(R.string.purchase_cancelled)
            if (success) onSubscribed()
        }
    }

    // OWNER: the moment Google authentication verifies the owner email,
    // unlock everything and go straight to Home — no billing, no trial.
    LaunchedEffect(vm.profile.isOwner) {
        if (vm.profile.isOwner) {
            vm.markOnboarded()
            onSubscribed()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Quick language switch (kept from onboarding; also in Profile)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguagePill("English", LanguagePrefs.current() == "en") {
                    scope.launch { LanguagePrefs.set("en") }
                }
                LanguagePill("हिन्दी", LanguagePrefs.current() == "hi") {
                    scope.launch { LanguagePrefs.set("hi") }
                }
            }

            Spacer(Modifier.height(20.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .clickable(enabled = BuildConfig.DEBUG) {
                        // Debug builds only: enter the free tier for testing when
                        // Play Billing/products are unavailable. Release never shows this.
                        devTaps++
                        if (devTaps >= 5) {
                            android.widget.Toast.makeText(
                                context, context.getString(R.string.dev_bypass_toast),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            vm.markOnboarded()
                            vm.enableDebugFreeTier()
                            onSubscribed()
                        }
                    }
            ) {
                Text("🌸", Modifier.padding(18.dp), style = MaterialTheme.typography.displaySmall)
            }

            Spacer(Modifier.height(14.dp))
            Text(
    stringResource(R.string.paywall_welcome),
    style = MaterialTheme.typography.headlineMedium,
    color = Color.White,
    textAlign = TextAlign.Center
)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.paywall_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

            Spacer(Modifier.height(18.dp))
            BenefitRow(R.string.paywall_benefit_journeys)
            BenefitRow(R.string.paywall_benefit_tips)
            BenefitRow(R.string.paywall_benefit_categories, small = true)
            BenefitRow(R.string.paywall_benefit_tools)
            BenefitRow(R.string.paywall_benefit_adjust)
            BenefitRow(R.string.paywall_benefit_progress)
            BenefitRow(R.string.paywall_benefit_bilingual)
            BenefitRow(R.string.paywall_benefit_adfree)

            Spacer(Modifier.height(20.dp))
            val monthly = plans.firstOrNull { it.productId == BillingManager.MONTHLY_PRODUCT_ID }
            val yearlyPlan = plans.firstOrNull { it.productId == BillingManager.YEARLY_PRODUCT_ID }
            PaywallPlanCard(
                title = stringResource(R.string.plan_monthly),
                price = monthly?.formattedPrice ?: stringResource(R.string.plan_monthly_price),
                trialLine = stringResource(R.string.days_free_then_monthly),
                selected = !yearly, badge = null
            ) { yearly = false }
            Spacer(Modifier.height(10.dp))
            PaywallPlanCard(
                title = stringResource(R.string.plan_yearly),
                price = yearlyPlan?.formattedPrice ?: stringResource(R.string.plan_yearly_price),
                trialLine = stringResource(R.string.days_free_then_yearly),
                selected = yearly, badge = stringResource(R.string.best_value)
            ) { yearly = true }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    val productId = if (yearly) BillingManager.YEARLY_PRODUCT_ID
                                    else BillingManager.MONTHLY_PRODUCT_ID
                    if (context is android.app.Activity) {
                        vm.launchSubscription(context, productId)
                    }
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) { Text(stringResource(R.string.start_trial), style = MaterialTheme.typography.titleMedium) }

            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.cancel_anytime_play),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(14.dp))
            // Google sign-in: REAL Credential Manager flow. After real Google
            // authentication: owner → unlocked by the LaunchedEffect above;
            // everyone else → routed straight into Google Play Billing for the
            // plan selected above (required flow, step 8).
            OutlinedButton(
                onClick = {
                    vm.signInWithGoogle(context) { result ->
                        val signedIn = result.getOrNull()
                        if (signedIn != null) {
                            if (signedIn.isOwner) {
                                // OWNER: unlock happens via vm.profile collector —
                                // no billing, no trial, straight to Home.
                                message = context.getString(R.string.signed_in_as, signedIn.email ?: "")
                            } else if (context is android.app.Activity) {
                                message = context.getString(R.string.signed_in_as, signedIn.email ?: "")
                                vm.launchSubscription(
                                    context,
                                    if (yearly) BillingManager.YEARLY_PRODUCT_ID
                                    else BillingManager.MONTHLY_PRODUCT_ID
                                )
                            }
                        } else {
                            message = friendlySignInError(context, result.exceptionOrNull())
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("G", color = Color(0xFF4285F4), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.padding(4.dp))
                Text(stringResource(R.string.continue_with_google))
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { vm.restorePurchases() }) {
                Text(stringResource(R.string.restore_purchases))
            }

            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            if (!billingAvailable) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.billing_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            Text(stringResource(R.string.trial_disclosure),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun LanguagePill(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(label, Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun BenefitRow(res: Int, small: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 10.dp))
        Text(stringResource(res),
            style = if (small) MaterialTheme.typography.bodySmall
                    else MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PaywallPlanCard(
    title: String, price: String, trialLine: String,
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
        Column(Modifier.padding(16.dp)) {
            if (badge != null) {
                Surface(shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary) {
                    Text(badge, Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(6.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (selected) "●" else "○", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(6.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(price, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text(trialLine, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Opens the Google Play subscriptions management page for GlowCare. */
fun openManageSubscription(context: android.content.Context) {
    val uri = Uri.parse(
        "https://play.google.com/store/account/subscriptions" +
        "?package=com.glowcare.app&sku=${BillingManager.MONTHLY_PRODUCT_ID}"
    )
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

/**
 * Maps real Credential Manager / Google API failures to honest, actionable
 * messages (bilingual via resources). No error is swallowed silently.
 */
fun friendlySignInError(context: android.content.Context, error: Throwable?): String {
    val msg = error?.message.orEmpty().trim()
    return when {
        error is androidx.credentials.exceptions.NoCredentialException ->
            context.getString(R.string.signin_no_accounts)
        // ApiException: 10 / DEVELOPER_ERROR — OAuth client or SHA-1 mismatch
        msg.startsWith("10:") || msg.contains("DEVELOPER_ERROR") ->
            context.getString(R.string.signin_not_registered)
        error is IllegalStateException && msg.contains("not configured") -> msg
        else -> context.getString(R.string.signin_failed, msg.ifEmpty { error?.javaClass?.simpleName ?: "" })
    }
}
