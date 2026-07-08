package com.kyrx.mypresence.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val notificationsEnabled: StateFlow<Boolean> = preferencesRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoStartEnabled: StateFlow<Boolean> = preferencesRepository.autoStartEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isDarkMode: StateFlow<Boolean> = preferencesRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setNotificationsEnabled(enabled) }
    }

    fun setAutoStartEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoStartEnabled(enabled) }
    }

    fun setIsDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setIsDarkMode(enabled) }
    }
}
