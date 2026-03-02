package com.dipdev.btkeyboard.ui.screens.keyboard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dipdev.btkeyboard.ui.viewmodel.KeyboardViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Colour tokens ─────────────────────────────────────────────────────────
private val KbdBg = Color(0xFF13142E)   // keyboard background
private val KeyBg = Color(0xFF2A2D52)   // regular letter key
private val FnKeyBg = Color(0xFF1E2044)   // function key (darker)
private val SpaceBg = Color(0xFF262952)   // space bar
private val KeyBorder = Color(0xFF3D3F6C)   // subtle key border
private val KeyText = Color.White
private val FnText = Color(0xFFB0B4D6)
private val ActiveBg = Color(0xFF00B8D9)   // active shift/num indicator
private val ActiveText = Color(0xFF080A18)

// ── Letter layers ─────────────────────────────────────────────────────────
private val LETTERS_LOWER = listOf(
    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
    listOf("z", "x", "c", "v", "b", "n", "m")
)
private val LETTERS_UPPER = listOf(
    listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
    listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
    listOf("Z", "X", "C", "V", "B", "N", "M")
)

// ── Number/symbol layers ──────────────────────────────────────────────────
private val NUMS_ROW1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
private val NUMS_ROW2 = listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")")
private val NUMS_ROW3 = listOf("-", "=", "_", "+", ":", ";", "\"", "'", "[", "]")

// ── Main keyboard composable ──────────────────────────────────────────────

@Composable
fun QwertyKeyboard(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var shifted by remember { mutableStateOf(false) }
    var capsLock by remember { mutableStateOf(false) }  // double-tap
    var numsMode by remember { mutableStateOf(false) }
    var lastTypedChar by remember { mutableStateOf(' ') }

    val autoCaps by viewModel.autoCaps.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val buttonScale by viewModel.buttonScale.collectAsStateWithLifecycle()

    // Initialize shift state based on autoCaps preference
    LaunchedEffect(autoCaps) {
        if (autoCaps) shifted = true
    }

    val rowHeight = (56 * buttonScale).dp

    fun haptic() {
        if (hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun sendChar(c: String) {
        haptic()
        val char = c.firstOrNull()
        if (char != null) {
            viewModel.sendChar(char)
            lastTypedChar = char
        }

        // auto-unshift after one char (unless caps lock)
        if (shifted && !capsLock) shifted = false
    }

    fun sendKey(code: Byte) {
        haptic()
        viewModel.sendSpecialKey(code)

        if (autoCaps) {
            if (code == KeyMap.Keys.SPACE) {
                if (lastTypedChar == '.' || lastTypedChar == '?' || lastTypedChar == '!') {
                    shifted = true
                }
            } else if (code == KeyMap.Keys.ENTER) {
                shifted = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KbdBg)
            .padding(horizontal = 5.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {

        if (!numsMode) {
            // ── Letters mode ─────────────────────────────────────────────
            val letters = if (shifted || capsLock) LETTERS_UPPER else LETTERS_LOWER

            // Row 1 – 10 keys
            KeyRow(Modifier.height(rowHeight)) {
                letters[0].forEach { c ->
                    LetterKey(c, Modifier.weight(1f)) { sendChar(c) }
                }
            }
            // Row 2 – 9 keys, centred
            KeyRow(Modifier.height(rowHeight)) {
                Spacer(Modifier.weight(0.4f))
                letters[1].forEach { c ->
                    LetterKey(c, Modifier.weight(1f)) { sendChar(c) }
                }
                Spacer(Modifier.weight(0.4f))
            }
            // Row 3 – shift + 7 letters + backspace
            KeyRow(Modifier.height(rowHeight)) {
                val shiftActive = shifted || capsLock
                FnKey(
                    label = if (capsLock) "⇪" else "⇧",
                    active = shiftActive,
                    modifier = Modifier.weight(1.5f)
                ) {
                    haptic()
                    if (!shifted && !capsLock) {          // off → on
                        shifted = true
                    } else if (shifted && !capsLock) {    // on → caps
                        capsLock = true
                    } else {                               // caps → off
                        shifted = false; capsLock = false
                    }
                }
                letters[2].forEach { c ->
                    LetterKey(c, Modifier.weight(1f)) { sendChar(c) }
                }
                FnKey("⌫", modifier = Modifier.weight(1.5f)) {
                    sendKey(KeyMap.Keys.BACKSPACE)
                }
            }
        } else {
            // ── Numbers mode ──────────────────────────────────────────────
            KeyRow(Modifier.height(rowHeight)) {
                NUMS_ROW1.forEach { c ->
                    LetterKey(c, Modifier.weight(1f)) { sendChar(c) }
                }
            }
            KeyRow(Modifier.height(rowHeight)) {
                NUMS_ROW2.forEach { c ->
                    LetterKey(c, Modifier.weight(1f)) { sendChar(c) }
                }
            }
            KeyRow(Modifier.height(rowHeight)) {
                NUMS_ROW3.forEach { c ->
                    LetterKey(c, Modifier.weight(1f)) { sendChar(c) }
                }
                FnKey("⌫", modifier = Modifier.weight(1.5f)) {
                    sendKey(KeyMap.Keys.BACKSPACE)
                }
            }
        }

        // ── Row 4 (Space row) ─────────────────────────────────────────────
        KeyRow(Modifier.height(rowHeight)) {
            // ?123 / ABC mode toggle
            FnKey(
                label = if (numsMode) "ABC" else "?123",
                modifier = Modifier.weight(1.5f)
            ) { haptic(); numsMode = !numsMode }

            // Comma
            LetterKey(",", Modifier.weight(0.8f)) { sendChar(",") }

            // Space
            SpaceKey(Modifier.weight(3.5f)) { sendKey(KeyMap.Keys.SPACE) }

            // Period
            LetterKey(".", Modifier.weight(0.8f)) { sendChar(".") }

            // Enter
            FnKey("↵", modifier = Modifier.weight(1.5f)) {
                sendKey(KeyMap.Keys.ENTER)
            }
        }

        // ── Row 5 (Modifiers & Arrows) ────────────────────────────────────
        KeyRow(Modifier.height(rowHeight)) {
            // Ctrl
            FnKey(
                label = "Ctrl",
                active = viewModel.isCtrlOn(),
                modifier = Modifier.weight(1f)
            ) { haptic(); viewModel.toggleCtrl() }

            // Win / GUI
            FnKey(
                label = "Win",
                active = viewModel.isGuiOn(),
                modifier = Modifier.weight(1f)
            ) { haptic(); viewModel.toggleGui() }

            // Alt
            FnKey(
                label = "Alt",
                active = viewModel.isAltOn(),
                modifier = Modifier.weight(1f)
            ) { haptic(); viewModel.toggleAlt() }

            // Shift (sticky modifier version for combinations)
            val shiftActive = shifted || capsLock
            FnKey(
                label = "⇧",
                active = shiftActive,
                modifier = Modifier.weight(1f)
            ) {
                haptic()
                if (!shifted && !capsLock) {          // off → on
                    shifted = true
                } else if (shifted && !capsLock) {    // on → caps
                    capsLock = true
                } else {                               // caps → off
                    shifted = false; capsLock = false
                }
            }

            // Left Arrow
            FnKey("←", modifier = Modifier.weight(1f)) {
                sendKey(KeyMap.Keys.LEFT)
            }

            // Right Arrow
            FnKey("→", modifier = Modifier.weight(1f)) {
                sendKey(KeyMap.Keys.RIGHT)
            }
        }
    }
}

// ── Row wrapper ───────────────────────────────────────────────────────────

@Composable
private fun KeyRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

// ── Letter key ────────────────────────────────────────────────────────────

@Composable
private fun LetterKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .repeatingClickable(interactionSource = interactionSource, onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = KeyBg,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = KeyText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

// ── Function key (shift, backspace, enter, 123…) ──────────────────────────

@Composable
private fun FnKey(
    label: String,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (active) ActiveBg else FnKeyBg, tween(120), label = "fnbg")
    val text by animateColorAsState(if (active) ActiveText else FnText, tween(120), label = "fntx")

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .repeatingClickable(interactionSource = interactionSource, onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = bg,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = label, color = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Space bar ─────────────────────────────────────────────────────────────

@Composable
private fun SpaceKey(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .repeatingClickable(interactionSource = interactionSource, onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = SpaceBg,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "space",
                color = FnText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

// ── Repeating clickable modifier ──────────────────────────────────────────

fun Modifier.repeatingClickable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    initialDelayMillis: Long = 400,
    repeatDelayMillis: Long = 50,
    onClick: () -> Unit
): Modifier = composed {
    val currentClickListener by rememberUpdatedState(onClick)

    this
        .indication(interactionSource, androidx.compose.material.ripple.rememberRipple())
        .pointerInput(interactionSource, enabled) {
            coroutineScope {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val press = PressInteraction.Press(down.position)

                    // Fire immediately once and show press indication
                    if (enabled) {
                        currentClickListener()
                    }

                    val job = launch {
                        interactionSource.emit(press)

                        if (enabled) {
                            delay(initialDelayMillis)
                            while (true) {
                                currentClickListener()
                                delay(repeatDelayMillis)
                            }
                        }
                    }

                    // Wait for release
                    val up = waitForUpOrCancellation()
                    job.cancel() // Stop repeating loop and any pending delayed execution

                    launch {
                        if (up != null) {
                            interactionSource.emit(PressInteraction.Release(press))
                        } else {
                            interactionSource.emit(PressInteraction.Cancel(press))
                        }
                    }
                }
            }
        }
}
