package com.kyrx.mypresence.feature.dashboard

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kyrx.mypresence.core.gateway.GatewayConnectionState
import com.kyrx.mypresence.core.gateway.GatewayDiagnostics
import com.kyrx.mypresence.data.remote.DiscordUser
import com.kyrx.mypresence.domain.model.AppInfo
import com.kyrx.mypresence.domain.model.AppPresenceConfig
import com.kyrx.mypresence.domain.model.CustomRpcPreset
import com.kyrx.mypresence.domain.model.PresenceData
import com.kyrx.mypresence.domain.repository.AppRepository
import com.kyrx.mypresence.domain.repository.AuthRepository
import com.kyrx.mypresence.domain.repository.AuthState
import com.kyrx.mypresence.domain.repository.GatewayRepository
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import com.kyrx.mypresence.domain.usecase.TogglePresenceUseCase
import com.kyrx.mypresence.domain.usecase.UpdatePresenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val gatewayRepository: GatewayRepository,
    private val togglePresence: TogglePresenceUseCase,
    private val updatePresence: UpdatePresenceUseCase,
    private val appRepository: AppRepository
) : AndroidViewModel(application) {

    private val presenceSeq = AtomicInteger(0)
    private var lastPresenceSentMs = 0L
    private var lastSentAppPackage: String? = null
    private var lastSentSource: String? = null
    private var presenceResendJob: Job? = null

    val authState: StateFlow<AuthState> = authRepository.authState
    val gatewayState: StateFlow<GatewayConnectionState> = gatewayRepository.state
    val diagnostics: StateFlow<GatewayDiagnostics> = gatewayRepository.diagnostics

    private val _presenceEnabled = MutableStateFlow(false)
    val presenceEnabled: StateFlow<Boolean> = _presenceEnabled.asStateFlow()

    val installedApps: StateFlow<List<AppInfo>> = appRepository.installedApps
    val detectedApp: StateFlow<AppInfo?> = appRepository.foregroundApp

    val enabledApps: StateFlow<Set<String>> = preferencesRepository.enabledApps
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val keepOnline24_7: StateFlow<Boolean> = preferencesRepository.keepOnline24_7
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val appPresenceConfigs: StateFlow<List<AppPresenceConfig>> = preferencesRepository.appPresenceConfigs
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customRpcPresets: StateFlow<List<CustomRpcPreset>> = preferencesRepository.customRpcPresets
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
        restorePresenceState()
    }

    private fun sendPresence(app: AppInfo?, source: String) {
        val seq = presenceSeq.incrementAndGet()
        val now = System.currentTimeMillis()
        val delta = if (lastPresenceSentMs > 0) now - lastPresenceSentMs else -1L
        val appPkg = app?.packageName
        if (appPkg == lastSentAppPackage && source == lastSentSource && delta > 0 && delta < 3000) {
            Log.d("DashboardVM", "[PRESENCE #$seq] Skipping duplicate send: same app ($appPkg) from same source ($source) ${delta}ms ago")
            return
        }
        lastSentAppPackage = appPkg
        lastSentSource = source
        Log.d("DashboardVM", "[PRESENCE #$seq] sendPresence() from=$source elapsedSinceLast=${delta}ms")
        if (app == null) {
            Log.d("DashboardVM", "[PRESENCE #$seq] No foreground app detected – sending fallback: Playing My Presence / Using My Presence")
        } else {
            val enabled = enabledApps.value.isEmpty() || app.packageName in enabledApps.value
            Log.d("DashboardVM", "[PRESENCE #$seq] Foreground app: ${app.appName} (${app.packageName}) enabled=$enabled")
        }
        lastPresenceSentMs = now
        viewModelScope.launch { updatePresence(app) }
    }

    private fun startPresenceResendLoop() {
        stopPresenceResendLoop()
        Log.d("DashboardVM", "Presence resend loop is owned by PresenceService")
    }

    private fun stopPresenceResendLoop() {
        presenceResendJob?.cancel()
        presenceResendJob = null
    }

    private fun restorePresenceState() {
        viewModelScope.launch {
            val token = preferencesRepository.userToken.first()
            if (token.isNotBlank()) {
                _presenceEnabled.value = true
                preferencesRepository.setPresenceEnabled(true)
                togglePresence.start()
            }
        }
    }

    fun togglePresenceEnabled(enabled: Boolean) {
        _presenceEnabled.value = enabled
        viewModelScope.launch {
            preferencesRepository.setPresenceEnabled(enabled)
        }
        if (enabled) {
            togglePresence.start()
        } else {
            togglePresence.stop()
        }
    }

    fun toggleKeepOnline(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setKeepOnline24_7(enabled)
            gatewayRepository.setMaxReconnectAttempts(if (enabled) 10000 else 5)
            if (_presenceEnabled.value) {
                gatewayRepository.disconnect()
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

    fun saveCustomRpcPreset(preset: CustomRpcPreset) {
        viewModelScope.launch { preferencesRepository.saveCustomRpcPreset(preset) }
    }

    fun deleteCustomRpcPreset(presetId: String) {
        viewModelScope.launch { preferencesRepository.deleteCustomRpcPreset(presetId) }
    }

    fun saveCustomPresence(name: String, details: String, state: String, type: Int) {
        viewModelScope.launch {
            preferencesRepository.savePresenceConfig(name, details, state, type)
            if (_presenceEnabled.value && detectedApp.value == null) {
                Log.d("DashboardVM", "Custom presence saved, re-sending presence")
                sendPresence(null, "custom-save")
            }
        }
    }

    fun previewPresenceFor(app: AppInfo): PresenceData {
        val cfg = appPresenceConfigs.value.find { it.packageName == app.packageName }
        return PresenceData(
            name = cfg?.name?.ifBlank { app.appName } ?: app.appName,
            details = cfg?.details?.ifBlank { "Using ${app.appName}" } ?: "Using ${app.appName}",
            state = cfg?.state?.takeUnless {
                it.equals("via My Presence", ignoreCase = true) ||
                    it.equals("via My Presence app", ignoreCase = true)
            }.orEmpty(),
            type = cfg?.activityType?.takeIf { it >= 0 } ?: 0
        )
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            appRepository.refresh()
            if (_presenceEnabled.value) {
                gatewayRepository.disconnect()
                togglePresence.start()
            }
            kotlinx.coroutines.delay(800)
            _isRefreshing.value = false
        }
    }

    fun hasUsageStatsPermission(): Boolean = appRepository.checkUsageStatsPermission()
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

    fun isAppEnabled(packageName: String): Boolean =
        enabledApps.value.isEmpty() || packageName in enabledApps.value

    fun logout() {
        viewModelScope.launch {
            gatewayRepository.disconnect()
            authRepository.logout()
            preferencesRepository.clearAll()
        }
    }
}
