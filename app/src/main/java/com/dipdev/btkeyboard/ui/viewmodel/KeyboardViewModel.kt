package com.dipdev.btkeyboard.ui.viewmodel

import android.app.Application
import android.content.ClipboardManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dipdev.btkeyboard.data.bluetooth.BluetoothHidManager
import com.dipdev.btkeyboard.data.bluetooth.HidMouseReport
import com.dipdev.btkeyboard.data.preferences.PreferencesRepository
import com.dipdev.btkeyboard.ui.screens.keyboard.KeyMap
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────

class KeyboardViewModel(
    application: Application,
    private val prefs: PreferencesRepository
) : AndroidViewModel(application) {

    // Internal HID manager – lives with the ViewModel (Activity lifecycle-safe)
    private val hidManager = BluetoothHidManager(application)

    // ── BT state ──────────────────────────────────────────────────────────
    val btState: StateFlow<BluetoothHidManager.State> = hidManager.state

    // ── Sticky modifiers ──────────────────────────────────────────────────
    private var ctrlOn = false
    private var altOn = false
    private var guiOn = false

    fun toggleCtrl() {
        ctrlOn = !ctrlOn; updateModifiers()
    }

    fun toggleAlt() {
        altOn = !altOn; updateModifiers()
    }

    fun toggleGui() {
        guiOn = !guiOn; updateModifiers()
    }

    fun isCtrlOn() = ctrlOn
    fun isAltOn() = altOn
    fun isGuiOn() = guiOn

    private fun updateModifiers() {
        var m = 0
        if (ctrlOn) m = m or KeyMap.MOD_CTRL.toInt()
        if (altOn) m = m or KeyMap.MOD_ALT.toInt()
        if (guiOn) m = m or KeyMap.MOD_GUI.toInt()
        hidManager.stickyModifiers = m.toByte()
    }

    // ── Preferences flows ──────────────────────────────────────────────────────────
    val onboardingDone: StateFlow<Boolean> = prefs.onboardingDone
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val hapticsEnabled: StateFlow<Boolean> = prefs.hapticsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val buttonScale: StateFlow<Float> = prefs.buttonScale
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    val keepScreenOn: StateFlow<Boolean> = prefs.keepScreenOn
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val autoCaps: StateFlow<Boolean> = prefs.autoCaps
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun completeOnboarding() {
        viewModelScope.launch { prefs.setOnboardingDone(true) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setHapticsEnabled(enabled) }
    }

    fun setButtonScale(scale: Float) {
        viewModelScope.launch { prefs.setButtonScale(scale) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { prefs.setKeepScreenOn(enabled) }
    }

    fun setAutoCaps(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoCaps(enabled) }
    }

    // ── BT lifecycle actions ──────────────────────────────────────────────
    fun startHid() = hidManager.start()
    fun stopHid() = hidManager.stop()
    fun disableBluetooth() = hidManager.stopAndDisableBluetooth()

    // ── Key send helpers ──────────────────────────────────────────────────
    fun sendChar(c: Char) = hidManager.sendChar(c)
    fun sendSpecialKey(code: Byte) = hidManager.sendSpecialKey(code)
    fun sendConsumerKey(usageId: Int) {
        hidManager.sendConsumerKey(usageId)
    }

    // ── Mouse actions ─────────────────────────────────────────────────────
    fun sendMouseMovement(dx: Byte, dy: Byte) {
        hidManager.sendMouseReport(HidMouseReport(dx = dx, dy = dy))
    }

    fun sendMouseClick(leftClick: Boolean = false, rightClick: Boolean = false) {
        var buttons: Byte = 0
        if (leftClick) buttons = (buttons.toInt() or 0x01).toByte()
        if (rightClick) buttons = (buttons.toInt() or 0x02).toByte()
        hidManager.sendMouseReport(HidMouseReport(buttons = buttons))
        // Release immediately for a click
        hidManager.sendMouseReport(HidMouseReport.EMPTY)
    }

    fun sendString(text: String) {
        hidManager.sendString(text, viewModelScope)
    }

    fun pasteAndSend(clipboard: ClipboardManager?) {
        clipboard?.primaryClip?.getItemAt(0)?.text
            ?.toString()
            ?.takeIf { it.isNotEmpty() }
            ?.let { sendString(it) }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────
    override fun onCleared() {
        super.onCleared()
        hidManager.stop()
    }

    // ── Factory ───────────────────────────────────────────────────────────
    class Factory(
        private val application: Application,
        private val prefs: PreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(KeyboardViewModel::class.java)) {
                return KeyboardViewModel(application, prefs) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: $modelClass")
        }
    }
}
