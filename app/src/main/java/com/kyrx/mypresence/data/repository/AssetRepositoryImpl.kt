package com.kyrx.mypresence.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.DisplayMetrics
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kyrx.mypresence.core.analytics.CrashReporter
import com.kyrx.mypresence.data.remote.DiscordApi
import com.kyrx.mypresence.data.remote.ImgurUploader
import com.kyrx.mypresence.domain.repository.AssetRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val imgurUploader: ImgurUploader,
    private val discordApi: DiscordApi,
    private val json: Json,
    private val crashReporter: CrashReporter
) : AssetRepository {

    companion object {
        private const val TAG = "ASSET_REPO"
        private const val KEY_ASSET_CACHE = "asset_cache_map"
        private const val ASSET_CACHE_VERSION = "v4"
    }

    private val cacheKey = stringPreferencesKey(KEY_ASSET_CACHE)
    private val memoryCacheLock = Any()
    private var memoryCache: MutableMap<String, String>? = null

    override suspend fun resolveAppIcon(
        packageName: String,
        appName: String,
        userToken: String,
        applicationId: String
    ): String? {
        Log.i(TAG, "resolveAppIcon: pkg=$packageName app=$appName")
        crashReporter.setCustomKey("asset_last_package", packageName)

        val cacheId = "$ASSET_CACHE_VERSION:$applicationId:$packageName"
        val cache = loadCache()
        val cachedMpKey = cache[cacheId]

        if (cachedMpKey != null && cachedMpKey.isNotBlank()) {
            Log.i(TAG, "cache HIT: $packageName → $cachedMpKey")
            crashReporter.log("ASSET_REPO: cache hit $packageName → $cachedMpKey")
            return cachedMpKey
        }

        Log.i(TAG, "cache MISS: $cacheId")
        crashReporter.log("ASSET_REPO: cache miss $packageName")

        val playStoreIconUrl = discordApi.findPlayStoreIconUrl(packageName)
        if (!playStoreIconUrl.isNullOrBlank()) {
            Log.i(TAG, "Play Store icon found: $packageName -> $playStoreIconUrl")
            crashReporter.log("ASSET_REPO: Play Store icon found $packageName")
            val playStoreMpKey = discordApi.resolveExternalAsset(userToken, applicationId, playStoreIconUrl)
            if (!playStoreMpKey.isNullOrBlank()) {
                Log.i(TAG, "Discord success via Play Store icon: $playStoreMpKey")
                crashReporter.log("ASSET_REPO: Discord Play Store key $packageName -> $playStoreMpKey")
                saveToCache(cacheId, playStoreMpKey)
                return playStoreMpKey
            }
            Log.w(TAG, "Discord registration failed for Play Store icon: $packageName")
            crashReporter.log("ASSET_REPO: Play Store Discord registration failed $packageName")
        }

        val appInfo = try {
            context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            Log.w(TAG, "getApplicationInfo failed: $packageName — ${e.message}")
            crashReporter.log("ASSET_REPO: getApplicationInfo failed $packageName")
            return null
        }

        val bitmap = extractHighResIcon(appInfo)
        if (bitmap == null) {
            Log.w(TAG, "icon extraction failed: $packageName")
            crashReporter.log("ASSET_REPO: icon extraction failed $packageName")
            return null
        }

        val iconFile = saveBitmapToFilesDir(bitmap, packageName)
        if (iconFile == null) {
            Log.w(TAG, "icon file save failed: $packageName")
            crashReporter.log("ASSET_REPO: icon file save failed $packageName")
            return null
        }

        Log.i(TAG, "icon saved: ${iconFile.absolutePath} (${iconFile.length()}b)")
        crashReporter.log("ASSET_REPO: icon saved $packageName ${iconFile.length()}b")

        Log.d(TAG, "uploading to Imgur: $packageName")
        crashReporter.log("ASSET_REPO: uploading to Imgur $packageName")
        val imgurUrl = imgurUploader.upload(iconFile)
        if (imgurUrl == null) {
            Log.w(TAG, "Imgur upload failed: $packageName")
            crashReporter.log("ASSET_REPO: Imgur upload failed $packageName")
            iconFile.delete()
            return null
        }
        Log.i(TAG, "Imgur success: $imgurUrl")
        crashReporter.log("ASSET_REPO: Imgur success $packageName → $imgurUrl")

        Log.d(TAG, "registering with Discord external-assets: $packageName appId=$applicationId")
        crashReporter.log("ASSET_REPO: Discord registration $packageName")
        Log.d(TAG, "resolveExternalAsset CALL: token_len=${userToken.length} appId=$applicationId imgur=$imgurUrl")
        val mpKey = discordApi.resolveExternalAsset(userToken, applicationId, imgurUrl)
        Log.d(TAG, "resolveExternalAsset RETURN: mpKey=$mpKey")
        if (mpKey == null || mpKey.isBlank()) {
            Log.w(TAG, "Discord registration failed: $packageName")
            crashReporter.log("ASSET_REPO: Discord registration failed $packageName")
            iconFile.delete()
            return null
        }

        Log.i(TAG, "Discord success: $mpKey")
        crashReporter.log("ASSET_REPO: Discord key $packageName → $mpKey")

        saveToCache(cacheId, mpKey)
        Log.i(TAG, "cached: $packageName → $mpKey")

        iconFile.delete()
        return mpKey
    }

    private fun extractHighResIcon(appInfo: android.content.pm.ApplicationInfo): Bitmap? {
        @Suppress("DEPRECATION")
        return try {
            val pm = context.packageManager
            val res = context.packageManager.getResourcesForApplication(appInfo)
            val drawable = runCatching {
                res.getDrawableForDensity(appInfo.icon, DisplayMetrics.DENSITY_XXXHIGH)
            }.getOrNull() ?: appInfo.loadIcon(pm) ?: return null
            val size = maxOf(drawable.intrinsicWidth, drawable.intrinsicHeight, 512).coerceAtMost(1024)
            val bitmap = Bitmap.createBitmap(
                size,
                size,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "extractHighResIcon failed: ${e.message}")
            null
        }
    }

    private fun saveBitmapToFilesDir(bitmap: Bitmap, packageName: String): File? {
        return try {
            val dir = File(context.cacheDir, "presence-icons")
            if (dir.exists() && !dir.isDirectory) {
                dir.delete()
            }
            if (!dir.exists() && !dir.mkdirs()) {
                Log.e(TAG, "icon directory create failed: ${dir.absolutePath}")
                return null
            }
            val safeName = packageName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = File.createTempFile("$safeName-", ".png", dir)
            FileOutputStream(file).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IllegalStateException("Bitmap.compress returned false")
                }
            }
            if (file.length() <= 0L) {
                throw IllegalStateException("PNG file is empty")
            }
            // Log exact PNG bytes so icon upload issues can be diagnosed from Logcat.
            val bytes = file.readBytes()
            Log.i("ASSET_UPLOAD", "PNG file for $packageName")
            Log.i("ASSET_UPLOAD", "  path: ${file.absolutePath}")
            Log.i("ASSET_UPLOAD", "  size: ${bytes.size} bytes")
            Log.i("ASSET_UPLOAD", "  first_32_bytes: ${bytes.take(32).joinToString(" ") { "%02X".format(it) }}")
            Log.i("ASSET_UPLOAD", "  is_png: ${bytes.take(8).let { it[0].toInt()==0x89 && it[1].toInt()==0x50 && it[2].toInt()==0x4E && it[3].toInt()==0x47 && it[4].toInt()==0x0D && it[5].toInt()==0x0A && it[6].toInt()==0x1A && it[7].toInt()==0x0A }}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "saveBitmapToFilesDir failed: ${e.message}")
            null
        }
    }

    private suspend fun loadCache(): Map<String, String> {
        synchronized(memoryCacheLock) {
            memoryCache?.let { return it.toMap() }
        }
        return try {
            val raw = dataStore.data.map { prefs -> prefs[cacheKey] ?: "{}" }.first()
            val decoded = json.decodeFromString<Map<String, String>>(raw)
            synchronized(memoryCacheLock) { memoryCache = decoded.toMutableMap() }
            decoded
        } catch (e: Exception) {
            Log.w(TAG, "loadCache failed: ${e.message}")
            emptyMap()
        }
    }

    private suspend fun saveToCache(cacheId: String, mpKey: String) {
        try {
            val current = loadCache().toMutableMap()
            current[cacheId] = mpKey
            val encoded = json.encodeToString(current)
            dataStore.edit { prefs -> prefs[cacheKey] = encoded }
            synchronized(memoryCacheLock) { memoryCache = current }
        } catch (e: Exception) {
            Log.e(TAG, "saveToCache failed: ${e.message}")
        }
    }
}
