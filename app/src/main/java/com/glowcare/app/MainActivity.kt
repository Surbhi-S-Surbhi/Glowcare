package com.glowcare.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glowcare.app.R
import com.glowcare.app.navigation.AppNavHost
import com.glowcare.app.ui.premium.PaywallScreen
import com.glowcare.app.ui.onboarding.OnboardingScreen
import com.glowcare.app.ui.theme.GlowCareTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlowCareTheme {
                val vm: AppViewModel = viewModel()
                AppRoot(vm)
            }
        }
    }
}

@Composable
private fun AppRoot(vm: AppViewModel) {
    // First-launch paywall: the subscription screen comes before Home, and
    // returns whenever the verified Play subscription/trial lapses.
    Crossfade(
        
targetState = when {
    !vm.profile.chosen -> "signin"
    !vm.profile.isSignedIn -> "signin"
    !vm.billingReady -> "loading"
    !vm.state.onboarded -> "onboarding"
    !vm.premium && !vm.debugFreeTier -> "paywall"
    else -> "app"
}
,
        label = "root"
    ) { stage ->
        when (stage) {
            "loading" -> Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌸", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.checking_subscription),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            "onboarding" -> OnboardingScreen(vm) { }

            "paywall" -> PaywallScreen(vm) { vm.markOnboarded() }
            else -> AppNavHost(vm)
        }
    }
}
