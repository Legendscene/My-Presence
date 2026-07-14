package com.kyrx.mypresence.feature.presence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyrx.mypresence.domain.model.CustomRpcPreset
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomPresetsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _presets = MutableStateFlow<List<CustomRpcPreset>>(emptyList())
    val presets: StateFlow<List<CustomRpcPreset>> = _presets.asStateFlow()

    init {
        loadPresets()
    }

    private fun loadPresets() {
        viewModelScope.launch {
            _presets.value = preferencesRepository.customRpcPresets.first()
        }
    }

    fun createPreset(name: String) {
        val preset = CustomRpcPreset(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            preferencesRepository.saveCustomRpcPreset(preset)
            loadPresets()
        }
    }

    fun savePreset(preset: CustomRpcPreset) {
        viewModelScope.launch {
            preferencesRepository.saveCustomRpcPreset(preset.copyWith())
            loadPresets()
        }
    }

    fun deletePreset(presetId: String) {
        viewModelScope.launch {
            preferencesRepository.deleteCustomRpcPreset(presetId)
            loadPresets()
        }
    }

    fun duplicatePreset(preset: CustomRpcPreset) {
        val newPreset = preset.copyWith(
            newName = "${preset.name} (Copy)",
            newIsFavorite = false
        ).copy(
            id = java.util.UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            preferencesRepository.saveCustomRpcPreset(newPreset)
            loadPresets()
        }
    }

    fun toggleFavorite(presetId: String) {
        viewModelScope.launch {
            val currentPresets = _presets.value
            val preset = currentPresets.find { it.id == presetId }
                ?: return@launch
            val updated = preset.copyWith(newIsFavorite = !preset.isFavorite)
            preferencesRepository.saveCustomRpcPreset(updated)
            loadPresets()
        }
    }

    fun applyPreset(preset: CustomRpcPreset) {
        val now = System.currentTimeMillis()
        val applied = preset.toPresenceData().copy(
            startTimestamp = preset.startTimestamp?.let { now },
            endTimestamp = preset.endTimestamp?.let { end ->
                val anchor = preset.startTimestamp ?: preset.createdAt
                val duration = end - anchor
                if (duration > 0) now + duration else end
            }
        )
        viewModelScope.launch {
            preferencesRepository.savePresenceConfig(applied)
        }
    }
}
