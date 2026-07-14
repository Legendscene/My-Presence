package com.kyrx.mypresence.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyrx.mypresence.core.analytics.AnalyticsManager
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    val notificationsEnabled: StateFlow<Boolean> = preferencesRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoStartEnabled: StateFlow<Boolean> = preferencesRepository.autoStartEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isDarkMode: StateFlow<Boolean> = preferencesRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val analyticsEnabled: StateFlow<Boolean> = preferencesRepository.analyticsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val customPresenceName: StateFlow<String> = preferencesRepository.presenceName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "My Presence")
    val customPresenceDetails: StateFlow<String> = preferencesRepository.presenceDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val customPresenceState: StateFlow<String> = preferencesRepository.presenceState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val customPresenceType: StateFlow<Int> = preferencesRepository.presenceType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun saveCustomPresence(name: String, details: String, state: String, type: Int) {
        viewModelScope.launch { preferencesRepository.savePresenceConfig(name, details, state, type) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setNotificationsEnabled(enabled) }
    }

    fun setAutoStartEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoStartEnabled(enabled) }
    }

    fun setIsDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setIsDarkMode(enabled) }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        analyticsManager.setConsent(enabled)
        viewModelScope.launch { preferencesRepository.setAnalyticsEnabled(enabled) }
    }
}
