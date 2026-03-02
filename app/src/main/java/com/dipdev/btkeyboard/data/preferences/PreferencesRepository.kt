package com.dipdev.btkeyboard.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Datastore instance
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
        val PREF_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val PREF_BUTTON_SCALE = floatPreferencesKey("button_scale")
        val PREF_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val PREF_AUTO_CAPS = booleanPreferencesKey("auto_caps")
    }

    val onboardingDone: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_DONE] ?: false }
    val hapticsEnabled: Flow<Boolean> = dataStore.data.map { it[PREF_HAPTICS_ENABLED] ?: true }
    val buttonScale: Flow<Float> = dataStore.data.map { it[PREF_BUTTON_SCALE] ?: 1.0f }
    val keepScreenOn: Flow<Boolean> = dataStore.data.map { it[PREF_KEEP_SCREEN_ON] ?: true }
    val autoCaps: Flow<Boolean> = dataStore.data.map { it[PREF_AUTO_CAPS] ?: true }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[ONBOARDING_DONE] = done }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[PREF_HAPTICS_ENABLED] = enabled }
    }

    suspend fun setButtonScale(scale: Float) {
        dataStore.edit { it[PREF_BUTTON_SCALE] = scale }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[PREF_KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setAutoCaps(enabled: Boolean) {
        dataStore.edit { it[PREF_AUTO_CAPS] = enabled }
    }
}
