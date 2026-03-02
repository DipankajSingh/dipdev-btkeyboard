package com.dipdev.btkeyboard

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.view.WindowManager
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import com.dipdev.btkeyboard.data.preferences.PreferencesRepository
import com.dipdev.btkeyboard.data.preferences.dataStore
import com.dipdev.btkeyboard.ui.navigation.AppNavGraph
import com.dipdev.btkeyboard.ui.theme.BTKeyboardTheme
import com.dipdev.btkeyboard.ui.viewmodel.KeyboardViewModel

// ── Activity ──────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    private val prefs by lazy { PreferencesRepository(applicationContext.dataStore) }

    private val viewModel: KeyboardViewModel by viewModels {
        KeyboardViewModel.Factory(application, prefs)
    }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.all { it.value }) ensureBtOn()
    }

    private val btEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.startHid()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()

            LaunchedEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            BTKeyboardTheme {
                AppNavGraph(
                    viewModel   = viewModel,
                    onRequestBt = { checkAndStart() }
                )
            }
        }
    }

    // ── BT permission + enable flow ───────────────────────────────────────

    fun checkAndStart() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
        else
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)

        val allGranted = perms.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) ensureBtOn() else permLauncher.launch(perms)
    }

    private fun ensureBtOn() {
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter
        when {
            adapter == null         -> viewModel.startHid() // no BT hardware
            adapter.isEnabled       -> viewModel.startHid()
            else                    -> btEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }
}
