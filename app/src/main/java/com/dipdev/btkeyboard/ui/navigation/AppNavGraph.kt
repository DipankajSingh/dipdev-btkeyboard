package com.dipdev.btkeyboard.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dipdev.btkeyboard.ui.screens.keyboard.MainScreen
import com.dipdev.btkeyboard.ui.screens.onboarding.OnboardingScreen
import com.dipdev.btkeyboard.ui.screens.onboarding.PermissionScreen
import com.dipdev.btkeyboard.ui.screens.settings.SettingsScreen
import com.dipdev.btkeyboard.ui.viewmodel.KeyboardViewModel

// ── Screen states ─────────────────────────────────────────────────────────

private enum class AppScreen { ONBOARDING, PERMISSIONS, MAIN, SETTINGS, TRACKPAD }

// ── App Navigator (state machine — no NavHost) ────────────────────────────

@Composable
fun AppNavGraph(
    viewModel: KeyboardViewModel,
    onRequestBt: () -> Unit
) {
    val context = LocalContext.current
    val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()

    // Check permissions synchronously so we don't show permission screen unnecessarily
    val permList = remember { btPermissions() }
    val initialPermsGranted = remember(context) {
        permList.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    var permissionsGranted by remember { mutableStateOf(initialPermsGranted) }

    val initialScreen = when {
        !onboardingDone -> AppScreen.ONBOARDING
        !permissionsGranted -> AppScreen.PERMISSIONS
        else -> AppScreen.MAIN
    }

    // Allow dynamic navigation between screens (e.g. MAIN <-> SETTINGS)
    var currentScreen by remember(initialScreen) { mutableStateOf(initialScreen) }

    when (currentScreen) {
        AppScreen.ONBOARDING -> OnboardingScreen(
            onFinished = { viewModel.completeOnboarding() },
            onSkip = { viewModel.completeOnboarding() }
        )

        AppScreen.PERMISSIONS -> PermissionScreen(
            permissions = permList,
            onGranted = {
                permissionsGranted = true
                onRequestBt()
            }
        )

        AppScreen.MAIN -> MainScreen(
            viewModel = viewModel,
            onRequestBt = onRequestBt,
            onOpenSettings = { currentScreen = AppScreen.SETTINGS }
        )

        AppScreen.SETTINGS -> SettingsScreen(
            viewModel = viewModel,
            onBack = { currentScreen = AppScreen.MAIN }
        )

        AppScreen.TRACKPAD -> {
            // Reusing MainScreen housing but swapping keyboard for trackpad 
            // Better: We should wrap TrackpadScreen inside a Scaffold to hold the Status bar 
            // OR just switch entirely. Let's make an explicit Trackpad wrapper 
            // Wait, we need the status bar. The best way is to modify MainScreen to handle modes, BUT 
            // since we added onOpenTrackpad to MainScreen, let's just use MainScreen to host TrackpadScreen.
            // Wait, I will refactor MainScreen directly to handle swapping child views instead of jumping AppNavScreens entirely to keep StatusBar shared.
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────

private fun btPermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
    else
        listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
