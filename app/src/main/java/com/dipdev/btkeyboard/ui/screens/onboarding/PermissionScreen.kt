package com.dipdev.btkeyboard.ui.screens.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.dipdev.btkeyboard.ui.theme.CyanPrimary
import com.dipdev.btkeyboard.ui.theme.ErrorRed
import com.dipdev.btkeyboard.ui.theme.NavyDeep
import com.dipdev.btkeyboard.ui.theme.SuccessGreen
import com.dipdev.btkeyboard.ui.theme.TextDisabled
import com.dipdev.btkeyboard.ui.theme.TextPrimary
import com.dipdev.btkeyboard.ui.theme.TextSecondary
import com.dipdev.btkeyboard.ui.theme.WarningAmber

// ── Permission Screen ─────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(
    permissions: List<String>,
    onGranted: () -> Unit
) {
    val context = LocalContext.current
    val permState = rememberMultiplePermissionsState(permissions)

    // Auto-proceed if already granted
    LaunchedEffect(permState.allPermissionsGranted) {
        if (permState.allPermissionsGranted) onGranted()
    }

    // Determine which UI state to show
    val isPermanentlyDenied = !permState.allPermissionsGranted &&
            !permState.shouldShowRationale &&
            permState.permissions.any { it.status is PermissionStatus.Denied }

    val icon: String
    val title: String
    val body: String
    val btnText: String
    val btnColor: Color
    val onBtnClick: () -> Unit

    when {
        permState.allPermissionsGranted -> {
            icon = "✅"; title = "All Set!"; body = "Bluetooth permission granted."
            btnText = "Continue"; btnColor = SuccessGreen; onBtnClick = onGranted
        }

        permState.shouldShowRationale -> {
            icon = "⚠️"; title = "Permission Denied"
            body = "BT Keyboard needs Bluetooth access to function. Please grant the permission."
            btnText = "Try Again"; btnColor = WarningAmber
            onBtnClick = { permState.launchMultiplePermissionRequest() }
        }

        isPermanentlyDenied -> {
            icon = "🔒"; title = "Permission Blocked"
            body = "Bluetooth is permanently blocked. Open App Settings to grant it manually."
            btnText = "Open Settings"; btnColor = ErrorRed
            onBtnClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                )
            }
        }

        else -> {
            icon = "📡"; title = "Bluetooth Access\nNeeded"
            body =
                "BT Keyboard uses Bluetooth to connect to your PC or Mac as a wireless keyboard. No data is collected."
            btnText = "Grant Permission"; btnColor = CyanPrimary
            onBtnClick = { permState.launchMultiplePermissionRequest() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(NavyDeep, Color(0xFF0E0F22))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(btnColor.copy(alpha = 0.12f))
                    .border(2.dp, btnColor.copy(alpha = 0.35f), CircleShape)
            ) { Text(icon, fontSize = 56.sp) }

            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Box(
                Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(btnColor)
            )

            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onBtnClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = btnColor,
                    contentColor = NavyDeep
                )
            ) {
                Text(
                    btnText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            "Your data stays on your device.",
            style = MaterialTheme.typography.bodySmall,
            color = TextDisabled,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
