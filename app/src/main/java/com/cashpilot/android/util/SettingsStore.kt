package com.cashpilot.android.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cashpilot_settings")

object SettingsStore {
    private val SERVER_URL = stringPreferencesKey("server_url")
    private val API_KEY = stringPreferencesKey("api_key")
    private val HEARTBEAT_INTERVAL = intPreferencesKey("heartbeat_interval")
    private val ENABLED_SLUGS = stringSetPreferencesKey("enabled_slugs")
    private val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")

    fun settings(context: Context): Flow<Settings> =
        context.dataStore.data.map { prefs ->
            Settings(
                serverUrl = prefs[SERVER_URL] ?: "",
                apiKey = prefs[API_KEY] ?: "",
                heartbeatIntervalSeconds = prefs[HEARTBEAT_INTERVAL] ?: 30,
                enabledSlugs = prefs[ENABLED_SLUGS] ?: KnownApps.all.map { it.slug }.toSet(),
                // Migration: mark as completed only if both URL and key were configured before this field existed
                setupCompleted = prefs[SETUP_COMPLETED]
                    ?: (prefs[SERVER_URL]?.isNotEmpty() == true && prefs[API_KEY]?.isNotEmpty() == true),
            )
        }

    suspend fun update(context: Context, transform: (Settings) -> Settings) {
        context.dataStore.edit { prefs ->
            val current = Settings(
                serverUrl = prefs[SERVER_URL] ?: "",
                apiKey = prefs[API_KEY] ?: "",
                heartbeatIntervalSeconds = prefs[HEARTBEAT_INTERVAL] ?: 30,
                enabledSlugs = prefs[ENABLED_SLUGS] ?: KnownApps.all.map { it.slug }.toSet(),
                setupCompleted = prefs[SETUP_COMPLETED]
                    ?: (prefs[SERVER_URL]?.isNotEmpty() == true && prefs[API_KEY]?.isNotEmpty() == true),
            )
            val updated = transform(current)
            prefs[SERVER_URL] = updated.serverUrl
            prefs[API_KEY] = updated.apiKey
            prefs[HEARTBEAT_INTERVAL] = updated.heartbeatIntervalSeconds
            prefs[ENABLED_SLUGS] = updated.enabledSlugs
            prefs[SETUP_COMPLETED] = updated.setupCompleted
        }
    }
}
