package com.kyrx.mypresence.feature.dashboard

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kyrx.mypresence.core.gateway.GatewayConnectionState
import com.kyrx.mypresence.data.remote.DiscordGateway
import com.kyrx.mypresence.data.remote.DiscordUser
import com.kyrx.mypresence.domain.model.AppInfo
import com.kyrx.mypresence.domain.model.AppPresenceConfig
import com.kyrx.mypresence.domain.model.PresenceData
import com.kyrx.mypresence.domain.repository.AuthRepository
import com.kyrx.mypresence.domain.repository.AuthState
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import com.kyrx.mypresence.domain.usecase.DetectForegroundAppUseCase
import com.kyrx.mypresence.domain.usecase.TogglePresenceUseCase
import com.kyrx.mypresence.domain.usecase.UpdatePresenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val discordGateway: DiscordGateway,
    private val togglePresence: TogglePresenceUseCase,
    private val updatePresence: UpdatePresenceUseCase,
    private val detectForegroundApp: DetectForegroundAppUseCase
) : AndroidViewModel(application) {

    val authState: StateFlow<AuthState> = authRepository.authState
    val gatewayState: StateFlow<GatewayConnectionState> = discordGateway.state

    private val _presenceEnabled = MutableStateFlow(false)
    val presenceEnabled: StateFlow<Boolean> = _presenceEnabled.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _detectedApp = MutableStateFlow<AppInfo?>(null)
    val detectedApp: StateFlow<AppInfo?> = _detectedApp.asStateFlow()

    val enabledApps: StateFlow<Set<String>> = preferencesRepository.enabledApps
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val keepOnline24_7: StateFlow<Boolean> = preferencesRepository.keepOnline24_7
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val appPresenceConfigs: StateFlow<List<AppPresenceConfig>> = preferencesRepository.appPresenceConfigs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customPresenceName: StateFlow<String> = preferencesRepository.presenceName
        .stateIn(viewModelScope, SharingStarted.Eagerly, "My Presence")
    val customPresenceDetails: StateFlow<String> = preferencesRepository.presenceDetails
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Custom mobile presence")
    val customPresenceState: StateFlow<String> = preferencesRepository.presenceState
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Privacy-safe mode")
    val customPresenceType: StateFlow<Int> = preferencesRepository.presenceType
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init {
        loadUser()
        scanInstalledApps()
        startForegroundDetection()
    }

    private fun loadUser() {
        viewModelScope.launch { authRepository.loadCurrentUser() }
    }

    val currentUser: DiscordUser?
        get() = (authState.value as? AuthState.Authenticated)?.user

    fun togglePresence(enabled: Boolean) {
        _presenceEnabled.value = enabled
        if (enabled) {
            viewModelScope.launch {
                val success = togglePresence.start()
                if (!success) _presenceEnabled.value = false
            }
        } else {
            togglePresence.stop()
        }
    }

    fun toggleKeepOnline(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setKeepOnline24_7(enabled)
            discordGateway.setKeepOnlineMode(enabled)
            if (_presenceEnabled.value) {
                discordGateway.disconnect()
                delay(500)
                togglePresence.start()
            }
        }
    }

    fun toggleApp(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = preferencesRepository.enabledApps.first()
            preferencesRepository.setEnabledApps(
                if (enabled) current + packageName else current - packageName
            )
        }
    }

    fun saveAppPresenceConfig(config: AppPresenceConfig) {
        viewModelScope.launch { preferencesRepository.saveAppPresenceConfig(config) }
    }

    fun removeAppPresenceConfig(packageName: String) {
        viewModelScope.launch { preferencesRepository.removeAppPresenceConfig(packageName) }
    }

    fun saveCustomPresence(name: String, details: String, state: String, type: Int) {
        viewModelScope.launch {
            preferencesRepository.savePresenceConfig(name, details, state, type)
            if (_presenceEnabled.value && _detectedApp.value == null) {
                updatePresence(null)
            }
        }
    }

    fun previewPresenceFor(app: AppInfo): PresenceData {
        val cfg = appPresenceConfigs.value.find { it.packageName == app.packageName }
        return PresenceData(
            name = cfg?.name?.ifBlank { app.appName } ?: app.appName,
            details = cfg?.details?.ifBlank { "Using ${app.appName}" } ?: "Using ${app.appName}",
            state = cfg?.state?.ifBlank { "via My Presence" } ?: "via My Presence",
            type = cfg?.activityType?.takeIf { it >= 0 } ?: 0
        )
    }

    private fun scanInstalledApps() {
        viewModelScope.launch {
            val pm = application.packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val activities = pm.queryIntentActivities(intent, 0)
            val apps = activities
                .filter { it.activityInfo.packageName != application.packageName }
                .distinctBy { it.activityInfo.packageName }
                .map {
                    val ai = it.activityInfo
                    AppInfo(ai.packageName, ai.loadLabel(pm).toString())
                }
                .sortedBy { it.appName }
            _installedApps.value = apps
        }
    }

    private fun startForegroundDetection() {
        viewModelScope.launch {
            while (isActive) {
                delay(if (_presenceEnabled.value) 3000L else 10000L)
                val enabled = enabledApps.value
                val app = detectForegroundApp.getForegroundApp()
                if (app != null && app.packageName in enabled) {
                    if (app != _detectedApp.value) {
                        _detectedApp.value = app
                        if (_presenceEnabled.value) updatePresence(app)
                    }
                } else if (_detectedApp.value != null) {
                    _detectedApp.value = null
                    if (_presenceEnabled.value) updatePresence(null)
                }
            }
        }
    }

    fun hasUsageStatsPermission(): Boolean = detectForegroundApp.hasUsageStatsPermission()

    fun requestUsageAccessIntent(): Intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun requestBatteryOptimizationIntent(): Intent {
        return Intent(
            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            android.net.Uri.parse("package:${application.packageName}")
        )
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = application.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        return pm?.isIgnoringBatteryOptimizations(application.packageName) ?: false
    }

    fun isAppEnabled(packageName: String): Boolean = packageName in enabledApps.value

    fun logout() {
        viewModelScope.launch {
            discordGateway.disconnect()
            authRepository.logout()
            preferencesRepository.clearAll()
        }
    }
}
