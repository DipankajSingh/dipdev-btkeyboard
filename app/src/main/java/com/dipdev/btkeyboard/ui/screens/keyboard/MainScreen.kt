package com.dipdev.btkeyboard.ui.screens.keyboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dipdev.btkeyboard.data.bluetooth.BluetoothHidManager
import com.dipdev.btkeyboard.ui.screens.trackpad.TrackpadScreen
import com.dipdev.btkeyboard.ui.theme.CyanPrimary
import com.dipdev.btkeyboard.ui.theme.ErrorRed
import com.dipdev.btkeyboard.ui.theme.SuccessGreen
import com.dipdev.btkeyboard.ui.theme.WarningAmber
import com.dipdev.btkeyboard.ui.viewmodel.KeyboardViewModel

// ── Section background colours – clearly distinct from keyboard ──────────
private val HeaderBg = Color(0xFF181B42)  // rich indigo-navy (clearly visible)
private val KeyStripBg = Color(0xFF13163A)  // slightly darker indigo
private val TextRowBg = Color(0xFF1A1D48)  // matching header family

// ── Main Screen ───────────────────────────────────────────────────────────

@Composable
fun MainScreen(
    viewModel: KeyboardViewModel,
    onRequestBt: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val currentBtState by viewModel.btState.collectAsStateWithLifecycle()
    var showTrackpad by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080A18))
            .windowInsetsPadding(WindowInsets.systemBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status bar
        StatusBar(
            viewModel = viewModel,
            state = currentBtState,
            onRequestBt = onRequestBt,
            onOpenSettings = onOpenSettings,
            onToggleTrackpad = { showTrackpad = !showTrackpad },
            showingTrackpad = showTrackpad
        )

        Spacer(Modifier.height(16.dp))

        // Main peripheral content
        if (showTrackpad) {
            TrackpadScreen(
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Cyan accent divider – always clearly visible
            Box(Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(CyanPrimary.copy(alpha = 0.5f)))

            // ── Key strip ────────────────────────────────────────────────────
            KeyStrip(viewModel = viewModel)

            Box(Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CyanPrimary.copy(alpha = 0.2f)))

            // ── Text send ────────────────────────────────────────────────────
            TextSendBar(viewModel = viewModel)

            Box(Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CyanPrimary.copy(alpha = 0.2f)))

            // Flexible spacer to push the keyboard to the bottom
            Spacer(Modifier.weight(1f))

            // ── QWERTY keyboard (pure Compose — natural sizing) ───────────────
            QwertyKeyboard(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── Status Bar ────────────────────────────────────────────────────────────

@Composable
fun StatusBar(
    viewModel: KeyboardViewModel,
    state: BluetoothHidManager.State,
    onRequestBt: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleTrackpad: () -> Unit,
    showingTrackpad: Boolean
) {
    val isConnected = state is BluetoothHidManager.State.Connected
    val isAdvertising = state is BluetoothHidManager.State.Advertising

    val dotColor = when (state) {
        is BluetoothHidManager.State.Idle -> ErrorRed
        is BluetoothHidManager.State.Advertising -> WarningAmber
        is BluetoothHidManager.State.Connected -> SuccessGreen
        is BluetoothHidManager.State.Error -> ErrorRed
    }
    val statusText = when (state) {
        is BluetoothHidManager.State.Idle -> "Disconnected"
        is BluetoothHidManager.State.Advertising -> "Advertising…"
        is BluetoothHidManager.State.Connected -> state.deviceName
        is BluetoothHidManager.State.Error -> "Error"
    }

    // Animated pulse for advertising state
    val pulse = rememberInfiniteTransition(label = "pulse")
    val dotScale by pulse.animateFloat(
        initialValue = 0.85f, targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "dot"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Row: Status Text & Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Status dot
            Box(
                Modifier
                    .size(10.dp)
                    .scale(if (isAdvertising) dotScale else 1f)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            // Text
            Text(
                statusText,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            // Settings Button (Subtle at top right)
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFFAAB2CC),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bottom Row: Explanatory Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // BT Connect/Disconnect Button
            val btText =
                if (isConnected) "Disconnect" else if (isAdvertising) "Stop Pairing" else "Connect PC"
            val btColor =
                if (isConnected) ErrorRed else if (isAdvertising) WarningAmber else CyanPrimary
            val btBgColor =
                if (isConnected) ErrorRed.copy(alpha = 0.15f) else if (isAdvertising) WarningAmber.copy(
                    alpha = 0.15f
                ) else CyanPrimary.copy(alpha = 0.15f)

            Button(
                onClick = {
                    if (isConnected || isAdvertising) viewModel.stopHid()
                    else onRequestBt()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = btBgColor,
                    contentColor = btColor
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(btText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            // Trackpad / Keyboard Switch
            val viewText = if (showingTrackpad) "Keyboard" else "Trackpad"
            val viewIcon = if (showingTrackpad) Icons.Default.Keyboard else Icons.Default.TouchApp

            Button(
                onClick = onToggleTrackpad,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF252860),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = viewIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(viewText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Key Strip ─────────────────────────────────────────────────────────────

@Composable
fun KeyStrip(
    viewModel: KeyboardViewModel
) {
    val navKeys = listOf(
        "←" to KeyMap.Keys.LEFT,
        "→" to KeyMap.Keys.RIGHT,
        "↑" to KeyMap.Keys.UP,
        "↓" to KeyMap.Keys.DOWN
    )
    val specialKeys = listOf(
        "Esc" to KeyMap.Keys.ESC, "Tab" to KeyMap.Keys.TAB,
        "Del" to KeyMap.Keys.DELETE, "Ins" to KeyMap.Keys.INSERT,
        "Home" to KeyMap.Keys.HOME, "End" to KeyMap.Keys.END,
        "PgUp" to KeyMap.Keys.PAGE_UP, "PgDn" to KeyMap.Keys.PAGE_DOWN,
        "PrtSc" to KeyMap.Keys.PRINT_SCREEN, "Pause" to KeyMap.Keys.PAUSE
    )
    val fnKeys = (1..12).map { n -> "F$n" to (KeyMap.Keys.F1.toInt() + n - 1).toByte() }
    val mediaTxt = listOf("⏯", "⏹", "⏭", "⏮", "🔊", "🔉", "🔇")
    val mediaIds = listOf(
        KeyMap.Consumer.PLAY_PAUSE, KeyMap.Consumer.STOP,
        KeyMap.Consumer.NEXT_TRACK, KeyMap.Consumer.PREV_TRACK,
        KeyMap.Consumer.VOLUME_UP, KeyMap.Consumer.VOLUME_DOWN, KeyMap.Consumer.MUTE
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(KeyStripBg)
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item { SectionLabel("NAV") }
        items(navKeys) { (lbl, code) -> StripKey(lbl) { viewModel.sendSpecialKey(code) } }
        item { StripSep() }

        item { SectionLabel("SPEC") }
        items(specialKeys) { (lbl, code) -> StripKey(lbl) { viewModel.sendSpecialKey(code) } }
        item { StripSep() }

        item { SectionLabel("FN") }
        items(fnKeys) { (lbl, code) -> StripKey(lbl) { viewModel.sendSpecialKey(code) } }
        item { StripSep() }

        item { SectionLabel("MEDIA") }
        itemsIndexed(mediaTxt) { i, lbl -> StripKey(lbl) { viewModel.sendConsumerKey(mediaIds[i]) } }
    }
}

@Composable
fun StripKey(label: String, active: Boolean = false, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (active) CyanPrimary.copy(0.22f) else Color(0xFF252860),
        label = "bg"
    )
    val border by animateColorAsState(if (active) CyanPrimary else Color(0xFF3A3D70), label = "bd")
    val text by animateColorAsState(if (active) CyanPrimary else Color.White, label = "tc")

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.height(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(
                label,
                color = text,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        color = Color(0xFF6B70A8),
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(end = 2.dp)
    )
}

@Composable
fun StripSep() {
    Box(Modifier
        .width(1.dp)
        .height(22.dp)
        .background(Color(0xFF3A3D70)))
}

// ── Text Send Bar ─────────────────────────────────────────────────────────

@Composable
fun TextSendBar(viewModel: KeyboardViewModel) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    val hasText = text.isNotEmpty()

    val sendBg by animateColorAsState(
        if (hasText) CyanPrimary else Color(0xFF252860),
        tween(150), label = "send"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TextRowBg)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledIconButton(
            onClick = {
                val clip = context.getSystemService(android.content.ClipboardManager::class.java)
                viewModel.pasteAndSend(clip)
            },
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(8.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xFF252860),
                contentColor = Color(0xFFAAB2CC)
            )
        ) { Icon(Icons.Filled.ContentPaste, null, Modifier.size(18.dp)) }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .height(42.dp),
            placeholder = { Text("Type & send…", color = Color(0xFF6B70A8), fontSize = 12.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = Color(0xFF3A3D70),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = CyanPrimary,
                focusedContainerColor = Color(0xFF252860),
                unfocusedContainerColor = Color(0xFF1E2158)
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            textStyle = MaterialTheme.typography.bodySmall
        )

        FilledIconButton(
            onClick = {
                if (hasText) {
                    viewModel.sendString(text); text = ""
                }
            },
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(8.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = sendBg,
                contentColor = if (hasText) Color(0xFF080A18) else Color(0xFF6B70A8)
            )
        ) { Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp)) }
    }
}
