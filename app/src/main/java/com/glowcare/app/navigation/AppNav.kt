package com.glowcare.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.glowcare.app.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.glowcare.app.AppViewModel
import com.glowcare.app.ui.bookmarks.SavedTipsScreen
import com.glowcare.app.ui.camera.CameraScreen
import com.glowcare.app.ui.camera.EditorScreen
import com.glowcare.app.ui.camera.VideoPreviewScreen
import com.glowcare.app.ui.category.CategoryScreen
import com.glowcare.app.ui.explore.ExploreScreen
import com.glowcare.app.ui.home.HomeScreen
import com.glowcare.app.ui.journey.JourneyScreen
import com.glowcare.app.ui.premium.PremiumScreen
import com.glowcare.app.ui.profile.PrivacyScreen
import com.glowcare.app.ui.profile.ProfileScreen
import com.glowcare.app.ui.progress.ProgressScreen
import com.glowcare.app.ui.search.SearchScreen
import com.glowcare.app.ui.tip.TipDetailScreen

object Routes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val CAMERA = "camera"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val PREMIUM = "premium"
    const val SEARCH = "search"
    const val SAVED = "saved"
    const val PRIVACY = "privacy"
    const val CATEGORY = "category/{id}"
    const val TIP = "tip/{id}"
    const val JOURNEY = "journey/{id}"
    const val EDITOR = "editor?uri={uri}&fromCamera={fromCamera}"
    const val VIDEO_PREVIEW = "videoPreview?uri={uri}"

    fun category(id: String) = "category/$id"
    fun tip(id: String) = "tip/$id"
    fun journey(id: String) = "journey/$id"
    fun editor(uri: String, fromCamera: Boolean) =
        "editor?uri=${android.net.Uri.encode(uri)}&fromCamera=$fromCamera"
}

private data class Tab(val route: String, val label: String,
                       val icon: ImageVector, val activeIcon: ImageVector,
                       val labelRes: Int)

private val TABS = listOf(
    Tab(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home, R.string.nav_home),
    Tab(Routes.EXPLORE, "Explore", Icons.Outlined.Explore, Icons.Filled.Explore, R.string.nav_explore),
    Tab(Routes.CAMERA, "Camera", Icons.Outlined.CameraAlt, Icons.Filled.CameraAlt, R.string.nav_camera),
    Tab(Routes.PROGRESS, "Progress", Icons.Outlined.Insights, Icons.Filled.Insights, R.string.nav_progress),
    Tab(Routes.PROFILE, "Profile", Icons.Outlined.Person, Icons.Filled.Person, R.string.nav_profile)
)

@Composable
fun AppNavHost(vm: AppViewModel) {
    val nav: NavHostController = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in TABS.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TABS.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) tab.activeIcon else tab.icon, tab.label) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(180)) }
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    vm = vm,
                    onOpenTip = { nav.navigate(Routes.tip(it)) },
                    onOpenSearch = { nav.navigate(Routes.SEARCH) },
                    onOpenPremium = { nav.navigate(Routes.PREMIUM) },
                    onOpenCategory = { nav.navigate(Routes.category(it)) },
                    onOpenJourney = { nav.navigate(Routes.journey(it)) }
                )
            }
            composable(Routes.EXPLORE) {
                ExploreScreen(vm, onOpenCategory = { nav.navigate(Routes.category(it)) },
                    onOpenSearch = { nav.navigate(Routes.SEARCH) })
            }
            composable(Routes.CAMERA) {
                CameraScreen(
                    onOpenEditor = { uri -> nav.navigate(Routes.editor(uri.toString(), true)) },
                    onOpenVideoPreview = { uri ->
                        nav.navigate("videoPreview?uri=${android.net.Uri.encode(uri.toString())}")
                    },
                    onClose = { nav.popBackStack() },
                    premium = vm.premium,
                    onUpgrade = { nav.navigate(Routes.PREMIUM) }
                )
            }
            composable(Routes.VIDEO_PREVIEW) { entry ->
                val uri = entry.arguments?.getString("uri") ?: return@composable
                VideoPreviewScreen(
                    videoUri = android.net.Uri.parse(uri),
                    onDone = { nav.popBackStack() }
                )
            }
            composable(Routes.PROGRESS) { ProgressScreen(vm) }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    vm,
                    onOpenPremium = { nav.navigate(Routes.PREMIUM) },
                    onOpenSaved = { nav.navigate(Routes.SAVED) },
                    onOpenPrivacy = { nav.navigate(Routes.PRIVACY) }
                )
            }
            composable(Routes.PREMIUM) { PremiumScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Routes.SEARCH) {
                SearchScreen(vm, onBack = { nav.popBackStack() },
                    onOpenTip = { nav.navigate(Routes.tip(it)) })
            }
            composable(Routes.SAVED) {
                SavedTipsScreen(vm, onBack = { nav.popBackStack() },
                    onOpenTip = { nav.navigate(Routes.tip(it)) })
            }
            composable(Routes.PRIVACY) { PrivacyScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.CATEGORY) { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                CategoryScreen(
                    categoryId = id, vm = vm,
                    onBack = { nav.popBackStack() },
                    onOpenTip = { nav.navigate(Routes.tip(it)) },
                    onOpenPremium = { nav.navigate(Routes.PREMIUM) },
                    onOpenJourney = { nav.navigate(Routes.journey(it)) }
                )
            }
            composable(Routes.TIP) { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                TipDetailScreen(tipId = id, vm = vm, onBack = { nav.popBackStack() })
            }
            composable(Routes.JOURNEY) { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                JourneyScreen(categoryId = id, vm = vm,
                    onBack = { nav.popBackStack() },
                    onOpenTip = { nav.navigate(Routes.tip(it)) })
            }
            composable(Routes.EDITOR) { entry ->
                val uri = entry.arguments?.getString("uri") ?: return@composable
                val fromCamera = entry.arguments?.getString("fromCamera") == "true"
                EditorScreen(
                    imageUri = android.net.Uri.parse(uri),
                    isFromCamera = fromCamera,
                    onDone = { nav.popBackStack() },
                    premium = vm.premium,
                    onUpgrade = { nav.navigate(Routes.PREMIUM) }
                )
            }
        }
    }
}
