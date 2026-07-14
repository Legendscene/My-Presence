package com.kyrx.mypresence.domain.repository

import com.kyrx.mypresence.domain.model.AppCategory
import com.kyrx.mypresence.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class AppSort(val displayName: String) {
    CATEGORY("Category"),
    NAME_AZ("A-Z"),
    NAME_ZA("Z-A"),
    RECENTLY_USED("Recently Used")
}

interface AppRepository {
    val installedApps: StateFlow<List<AppInfo>>
    val foregroundApp: StateFlow<AppInfo?>
    val isScanning: StateFlow<Boolean>
    val hasUsageStatsPermission: Flow<Boolean>

    suspend fun refresh()
    fun setFavorite(packageName: String, favorite: Boolean)
    fun setSort(sort: AppSort)
    fun setSearchQuery(query: String)
    fun setFavoritesOnly(enabled: Boolean)
    fun getInstalledApps(): List<AppInfo>
    suspend fun getAppIcon(packageName: String): android.graphics.drawable.Drawable?
    fun checkUsageStatsPermission(): Boolean
}
