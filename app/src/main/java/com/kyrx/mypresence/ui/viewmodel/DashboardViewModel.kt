package com.kyrx.mypresence.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kyrx.mypresence.data.remote.DiscordGateway
import com.kyrx.mypresence.data.remote.DiscordUser
import com.kyrx.mypresence.domain.model.GatewayConnectionState
import com.kyrx.mypresence.domain.model.PresenceData
import com.kyrx.mypresence.domain.repository.AuthRepository
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import com.kyrx.mypresence.service.PresenceService
import com.kyrx.mypresence.core.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val discordGateway: DiscordGateway
) : AndroidViewModel(application) {

    val currentUser: StateFlow<DiscordUser?> = authRepository.currentUser
    val gatewayState: StateFlow<GatewayConnectionState> = discordGateway.state

    private val _presenceEnabled = MutableStateFlow(false)
    val presenceEnabled: StateFlow<Boolean> = _presenceEnabled.asStateFlow()

    private val _presenceName = MutableStateFlow("My Presence")
    val presenceName: StateFlow<String> = _presenceName.asStateFlow()

    private val _presenceDetails = MutableStateFlow("")
    val presenceDetails: StateFlow<String> = _presenceDetails.asStateFlow()

    private val _presenceState = MutableStateFlow("")
    val presenceState: StateFlow<String> = _presenceState.asStateFlow()

    private val _presenceType = MutableStateFlow(0)
    val presenceType: StateFlow<Int> = _presenceType.asStateFlow()

    init {
        loadUser()
        loadPresenceConfig()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val token = preferencesRepository.accessToken.first()
            if (token != null) {
                authRepository.loadCurrentUser(token)
            } else {
                authRepository.authenticateWithToken(Constants.DISCORD_TOKEN)
            }
        }
    }

    private fun loadPresenceConfig() {
        viewModelScope.launch {
            _presenceName.value = preferencesRepository.presenceName.first()
            _presenceDetails.value = preferencesRepository.presenceDetails.first()
            _presenceState.value = preferencesRepository.presenceState.first()
            _presenceType.value = preferencesRepository.presenceType.first()
        }
    }

    fun togglePresence(enabled: Boolean) {
        _presenceEnabled.value = enabled
        if (enabled) {
            startPresenceService()
        } else {
            stopPresenceService()
        }
    }

    private fun startPresenceService() {
        val intent = Intent(application, PresenceService::class.java).apply {
            action = PresenceService.ACTION_START
        }
        application.startForegroundService(intent)
        viewModelScope.launch {
            delay(2000)
            sendPresenceUpdate()
        }
    }

    private fun stopPresenceService() {
        discordGateway.disconnect()
        val intent = Intent(application, PresenceService::class.java).apply {
            action = PresenceService.ACTION_STOP
        }
        application.startService(intent)
    }

    fun updatePresenceName(name: String) {
        _presenceName.value = name
        saveAndUpdate()
    }

    fun updatePresenceDetails(details: String) {
        _presenceDetails.value = details
        saveAndUpdate()
    }

    fun updatePresenceState(state: String) {
        _presenceState.value = state
        saveAndUpdate()
    }

    fun updatePresenceType(type: Int) {
        _presenceType.value = type
        saveAndUpdate()
    }

    private fun saveAndUpdate() {
        viewModelScope.launch {
            preferencesRepository.savePresenceConfig(
                name = _presenceName.value,
                details = _presenceDetails.value,
                state = _presenceState.value,
                type = _presenceType.value
            )
            if (_presenceEnabled.value) {
                sendPresenceUpdate()
            }
        }
    }

    private suspend fun sendPresenceUpdate() {
        val presence = PresenceData(
            name = _presenceName.value,
            details = _presenceDetails.value,
            state = _presenceState.value,
            type = _presenceType.value,
            enabled = _presenceEnabled.value,
            status = "online",
            largeImage = "icon",
            largeText = "My Presence",
            startTimestamp = System.currentTimeMillis()
        )
        val intent = Intent(application, PresenceService::class.java).apply {
            action = PresenceService.ACTION_UPDATE
            putExtra("name", presence.name)
            putExtra("details", presence.details)
            putExtra("state", presence.state)
            putExtra("type", presence.type)
            putExtra("status", presence.status)
            putExtra("enabled", presence.enabled)
        }
        application.startService(intent)
        discordGateway.updatePresence(presence)
    }

    fun logout() {
        viewModelScope.launch {
            discordGateway.disconnect()
            authRepository.logout()
            preferencesRepository.clearAll()
        }
    }
}
