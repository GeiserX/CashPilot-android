package com.cashpilot.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cashpilot.android.model.AppStatus
import com.cashpilot.android.model.KnownApps
import com.cashpilot.android.model.Settings
import com.cashpilot.android.service.AppDetector
import com.cashpilot.android.util.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val detector = AppDetector(application)

    val settings: StateFlow<Settings> = SettingsStore.settings(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private val _appStatuses = MutableStateFlow<List<AppStatus>>(emptyList())
    val appStatuses: StateFlow<List<AppStatus>> = _appStatuses.asStateFlow()

    fun refreshStatuses() {
        val enabled = settings.value.enabledSlugs
        _appStatuses.value = detector.detectAll(enabled)
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch {
            SettingsStore.update(getApplication(), transform)
        }
    }

    fun toggleApp(slug: String) {
        updateSettings { s ->
            val new = s.enabledSlugs.toMutableSet()
            if (slug in new) new.remove(slug) else new.add(slug)
            s.copy(enabledSlugs = new)
        }
    }

    /** All known apps with installed status. */
    fun installedApps(): List<Pair<KnownApps, Boolean>> {
        // This is a simplified check; full implementation uses PackageManager
        return emptyList()
    }
}
