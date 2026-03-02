package com.dipdev.btkeyboard.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.dipdev.btkeyboard.ui.screens.keyboard.KeyMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class BluetoothHidManager(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothHidManager"
    }

    sealed class State {
        object Idle : State()
        object Advertising : State()
        data class Connected(val deviceName: String) : State()
        data class Error(val message: String) : State()
    }

    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /** Extra modifier keys toggled via the special-keys panel (Ctrl/Alt/Win). */
    @Volatile
    var stickyModifiers: Byte = 0

    // ── HID Callbacks ──────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d(TAG, "onAppStatusChanged registered=$registered")
            if (registered) _state.value = State.Advertising
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            Log.d(TAG, "onConnectionStateChanged state=$state device=${device.name}")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHost = device
                    _state.value = State.Connected(device.name ?: device.address)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedHost = null
                    _state.value = State.Advertising
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            hidDevice?.replyReport(device, type, id, ByteArray(bufferSize))
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d(TAG, "HID_DEVICE service connected")
                hidDevice = proxy as BluetoothHidDevice
                registerApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            Log.d(TAG, "HID_DEVICE service disconnected")
            hidDevice = null
            _state.value = State.Idle
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    fun start() {
        if (bluetoothAdapter?.isEnabled != true) {
            _state.value = State.Error("Bluetooth is disabled")
            return
        }
        bluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        disconnectHost()
        hidDevice?.unregisterApp()
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        hidDevice = null
        connectedHost = null
        _state.value = State.Idle
    }

    /** Forcefully drop the active connection using hidden API reflection. */
    private fun disconnectHost() {
        val dev = hidDevice ?: return
        val host = connectedHost ?: return
        try {
            val disconnectMethod =
                dev.javaClass.getMethod("disconnect", BluetoothDevice::class.java)
            disconnectMethod.invoke(dev, host)
            Log.d(TAG, "Successfully invoked hidden disconnect() on $host")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to invoke hidden disconnect()", e)
        }
    }

    /** Stop HID and also attempt to disable Bluetooth (works on Android ≤ 11). */
    @SuppressLint("MissingPermission")
    fun stopAndDisableBluetooth() {
        stop()
        try {
            @Suppress("DEPRECATION")
            bluetoothAdapter?.disable()
        } catch (_: Exception) { /* Android 12+ restricts this — silently ignore */
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerApp() {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "BT Keyboard",
            "Android Bluetooth Keyboard",
            "BTKeyboard App",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidKeyboardDescriptor.DESCRIPTOR
        )
        hidDevice?.registerApp(sdp, null, null, Executors.newCachedThreadPool(), hidCallback)
    }

    // ── Send helpers ──────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    fun sendKeyReport(report: HidKeyboardReport) {
        val host = connectedHost ?: return
        val dev = hidDevice ?: return
        dev.sendReport(host, HidKeyboardDescriptor.ID_KEYBOARD, report.toByteArray())
        dev.sendReport(
            host,
            HidKeyboardDescriptor.ID_KEYBOARD,
            HidKeyboardReport.EMPTY.toByteArray()
        )
    }

    /** Press a single special key (e.g. arrow, F-key) with optional sticky modifiers. */
    fun sendSpecialKey(keyCode: Byte) {
        val mod = stickyModifiers
        sendKeyReport(HidKeyboardReport(modifier = mod, key1 = keyCode))
        stickyModifiers = 0
    }

    /** Press a character key, merging sticky modifiers into the report. */
    fun sendChar(char: Char) {
        val (baseMod, code) = KeyMap.charToHid(char) ?: return
        val mod = (baseMod.toInt() or stickyModifiers.toInt()).toByte()
        sendKeyReport(HidKeyboardReport(modifier = mod, key1 = code))
        stickyModifiers = 0
    }

    /** Send an entire string, one character at a time with a small delay. */
    fun sendString(text: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            for (char in text) {
                sendChar(char)
                delay(20)
            }
        }
    }

    /** Send a consumer-control (media) key. */
    @SuppressLint("MissingPermission")
    fun sendConsumerKey(usageId: Int) {
        val host = connectedHost ?: return
        val dev = hidDevice ?: return
        val data = byteArrayOf((usageId and 0xFF).toByte(), (usageId shr 8 and 0xFF).toByte())
        dev.sendReport(host, HidKeyboardDescriptor.ID_CONSUMER, data)
        dev.sendReport(host, HidKeyboardDescriptor.ID_CONSUMER, byteArrayOf(0x00, 0x00))
    }

    /** Send a mouse report with buttons and X/Y displacement. */
    @SuppressLint("MissingPermission")
    fun sendMouseReport(report: HidMouseReport) {
        val host = connectedHost ?: return
        val dev = hidDevice ?: return
        dev.sendReport(host, HidKeyboardDescriptor.ID_MOUSE, report.toByteArray())
    }

    fun isConnected() = _state.value is State.Connected
}
