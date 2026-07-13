package com.kyrx.mypresence.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.LruCache
import androidx.core.content.ContextCompat
import com.kyrx.mypresence.domain.model.AppCategory
import android.util.Log
import com.kyrx.mypresence.domain.model.AppInfo
import com.kyrx.mypresence.domain.repository.AppRepository
import com.kyrx.mypresence.domain.repository.AppSort
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _foregroundApp = MutableStateFlow<AppInfo?>(null)
    private val _isScanning = MutableStateFlow(false)
    private val _sort = MutableStateFlow(AppSort.NAME_AZ)
    private val _searchQuery = MutableStateFlow("")
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    private val _favoritesOnly = MutableStateFlow(false)
    private val iconCacheLock = Any()
    private val _cachedIcons = object : LruCache<String, Drawable?>(300) {
        override fun sizeOf(key: String, value: Drawable?): Int = 1
    }
    private val _packageChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private var appCache: List<AppInfo> = emptyList()
    @Volatile private var refreshDone = false

    override val installedApps: StateFlow<List<AppInfo>> = combine(
        _allApps, _searchQuery, _sort, _favorites, _favoritesOnly
    ) { apps, query, sort, favs, onlyFavs ->
        var filtered = if (query.isBlank()) apps
        else apps.filter { it.appName.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }

        filtered = filtered.map { it.copy(isFavorite = it.packageName in favs) }

        if (onlyFavs) {
            filtered = filtered.filter { it.isFavorite }
        }

        when (sort) {
            AppSort.NAME_AZ -> filtered.sortedBy { it.appName.lowercase() }
            AppSort.NAME_ZA -> filtered.sortedByDescending { it.appName.lowercase() }
            AppSort.CATEGORY -> filtered.sortedWith(compareBy({ it.category.ordinal }, { it.appName.lowercase() }))
            AppSort.RECENTLY_USED -> filtered
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val foregroundApp: StateFlow<AppInfo?> = _foregroundApp.asStateFlow()
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    override val hasUsageStatsPermission: Flow<Boolean> = MutableStateFlow(checkUsageStatsPermission()).asStateFlow()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            scope.launch { refresh() }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        scope.launch {
            refresh()
            startForegroundDetection()
        }
    }

    override suspend fun refresh() {
        if (_isScanning.value) return
        _isScanning.value = true
        try {
            val pm = context.packageManager
            val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= 33) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_META_DATA)
            }

            val launcherApps = pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
                0
            ).map { it.activityInfo.packageName }.toSet()

            val apps = packages
                .filter { it.applicationInfo != null }
                .filter { it.packageName != context.packageName }
                .filter { it.packageName in launcherApps }
                .mapNotNull { pkg ->
                    val ai = pkg.applicationInfo ?: return@mapNotNull null
                    val appName = pm.getApplicationLabel(ai)?.toString() ?: pkg.packageName
                    val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val versionName = pkg.versionName ?: ""
                    val versionCode = if (Build.VERSION.SDK_INT >= 28) {
                        pkg.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        pkg.versionCode.toLong()
                    }
                    val category = AppCategory.categorize(pkg.packageName, appName, isSystem)
                    AppInfo(
                        packageName = pkg.packageName,
                        appName = appName,
                        isSystemApp = isSystem,
                        versionName = versionName,
                        versionCode = versionCode,
                        category = category
                    )
                }
                .distinctBy { it.packageName }

            appCache = apps
            _allApps.value = apps
            synchronized(iconCacheLock) { _cachedIcons.evictAll() }
            refreshDone = true
        } finally {
            _isScanning.value = false
        }
    }

    override fun getInstalledApps(): List<AppInfo> = appCache

    override suspend fun getAppIcon(packageName: String): Drawable? {
        synchronized(iconCacheLock) {
            _cachedIcons.get(packageName)?.let { return it }
        }
        return try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            val icon = ai.loadIcon(pm)
            synchronized(iconCacheLock) { _cachedIcons.put(packageName, icon) }
            icon
        } catch (_: Exception) {
            null
        }
    }

    override fun setFavorite(packageName: String, favorite: Boolean) {
        val current = _favorites.value.toMutableSet()
        if (favorite) current.add(packageName) else current.remove(packageName)
        _favorites.value = current
    }

    override fun setSort(sort: AppSort) { _sort.value = sort }
    override fun setSearchQuery(query: String) { _searchQuery.value = query }
    override fun setFavoritesOnly(enabled: Boolean) { _favoritesOnly.value = enabled }

    override fun checkUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private suspend fun startForegroundDetection() {
        while (!refreshDone) { delay(100) }
        scope.launch {
            while (scope.isActive) {
                detectForeground()
                delay(1000)
            }
        }
    }

    private suspend fun detectForeground() {
        if (!checkUsageStatsPermission()) {
            Log.w("AppDetect", "No Usage Stats permission — foreground detection disabled")
            return
        }
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager == null) {
                Log.w("AppDetect", "UsageStatsManager not available")
                return
            }
            val now = System.currentTimeMillis()
            val usageEvents = usageStatsManager.queryEvents(now - 5000, now)
            var foregroundPkg: String? = null
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND || event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    foregroundPkg = event.packageName
                    Log.d("DETECT_PIPE", "DETECT: event=${event.eventType} pkg=$foregroundPkg ts=${event.timeStamp} thread=${Thread.currentThread().id} hash=${event.hashCode()}")
                }
            }

            val pm = context.packageManager
            if (foregroundPkg != null && foregroundPkg != context.packageName) {
                val appInfo = appCache.find { it.packageName == foregroundPkg } ?: run {
                    val appName = try {
                        val ai = pm.getApplicationInfo(foregroundPkg!!, 0)
                        pm.getApplicationLabel(ai).toString()
                    } catch (_: Exception) { foregroundPkg!! }
                    AppInfo(packageName = foregroundPkg!!, appName = appName)
                }
                if (_foregroundApp.value?.packageName != foregroundPkg) {
                    _foregroundApp.value = appInfo
                }
            } else {
                if (_foregroundApp.value != null) {
                    _foregroundApp.value = null
                }
            }
        } catch (e: Exception) {
            Log.w("AppDetect", "Foreground detection exception: ${e::class.simpleName} - ${e.message}")
        }
    }
}
