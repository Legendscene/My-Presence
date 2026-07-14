package com.kyrx.mypresence.feature.apps

import androidx.lifecycle.ViewModel
import com.kyrx.mypresence.domain.model.AppCategory
import com.kyrx.mypresence.domain.model.AppInfo
import com.kyrx.mypresence.domain.repository.AppRepository
import com.kyrx.mypresence.domain.repository.AppSort
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class InstalledAppsViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _currentSort = MutableStateFlow(AppSort.CATEGORY)
    private val _showFavoritesOnly = MutableStateFlow(false)

    val filteredApps: StateFlow<List<AppInfo>> = appRepository.installedApps
    val isScanning: StateFlow<Boolean> = appRepository.isScanning
    val foregroundApp: StateFlow<AppInfo?> = appRepository.foregroundApp
    val enabledApps: StateFlow<Set<String>> = preferencesRepository.enabledApps
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val currentSort: StateFlow<AppSort> = _currentSort.asStateFlow()
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    init {
        viewModelScope.launch {
            appRepository.refresh()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        appRepository.setSearchQuery(query)
    }

    fun setSort(sort: AppSort) {
        _currentSort.value = sort
        appRepository.setSort(sort)
    }

    fun toggleFavorite(packageName: String) {
        val current = filteredApps.value.find { it.packageName == packageName }?.isFavorite ?: false
        appRepository.setFavorite(packageName, !current)
    }

    fun toggleEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = preferencesRepository.enabledApps.first()
            preferencesRepository.setEnabledApps(
                if (enabled) current + packageName else current - packageName
            )
        }
    }

    fun setFavoritesOnly(enabled: Boolean) {
        _showFavoritesOnly.value = enabled
        appRepository.setFavoritesOnly(enabled)
        if (enabled) {
            appRepository.setSearchQuery("")
            _searchQuery.value = ""
        }
    }

    fun enableAll() {
        viewModelScope.launch {
            val all = filteredApps.value.map { it.packageName }.toSet()
            preferencesRepository.setEnabledApps(all)
        }
    }

    fun disableAll() {
        viewModelScope.launch {
            preferencesRepository.setEnabledApps(emptySet())
        }
    }

    fun invertSelection() {
        viewModelScope.launch {
            val all = filteredApps.value.map { it.packageName }.toSet()
            val current = preferencesRepository.enabledApps.first()
            preferencesRepository.setEnabledApps((current - all) + (all - current))
        }
    }

    fun enableCategory(category: AppCategory) {
        viewModelScope.launch {
            val catApps = appRepository.getInstalledApps()
                .filter { it.category == category }
                .map { it.packageName }
                .toSet()
            val current = preferencesRepository.enabledApps.first()
            preferencesRepository.setEnabledApps(current union catApps)
        }
    }

    fun disableCategory(category: AppCategory) {
        viewModelScope.launch {
            val catApps = appRepository.getInstalledApps()
                .filter { it.category == category }
                .map { it.packageName }
                .toSet()
            val current = preferencesRepository.enabledApps.first()
            preferencesRepository.setEnabledApps(current - catApps)
        }
    }
}
