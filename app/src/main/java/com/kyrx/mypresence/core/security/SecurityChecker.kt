package com.kyrx.mypresence.core.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.security.MessageDigest

object SecurityChecker {

    fun isDeviceRooted(): Boolean {
        return checkSUBinary() || checkTestKeys() || checkDangerousProps()
    }

    fun isEmulator(): Boolean {
        return checkBuildFields() || checkQemuDrivers() || checkEmulatorFiles()
    }

    fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    fun isTampered(context: Context, expectedSignature: String = ""): Boolean {
        if (expectedSignature.isEmpty()) return false
        return getApkSignature(context) != expectedSignature
    }

    fun isRunningOnExternalStorage(context: Context): Boolean {
        return try {
            val ai = context.packageManager.getApplicationInfo(context.packageName, 0)
            val sourceDir = File(ai.sourceDir)
            sourceDir.absolutePath.contains("/mnt/") || sourceDir.absolutePath.contains("/sdcard/")
        } catch (_: Exception) {
            false
        }
    }

    private fun checkSUBinary(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su",
            "/su/bin/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkTestKeys(): Boolean {
        return Build.TAGS?.contains("test-keys") == true
    }

    private fun checkDangerousProps(): Boolean {
        val dangerousProps = mapOf(
            "ro.debuggable" to "1",
            "ro.secure" to "0"
        )
        return dangerousProps.any { (prop, value) ->
            try {
                val process = Runtime.getRuntime().exec(arrayOf("getprop", prop))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val line = reader.readLine()
                reader.close()
                process.destroy()
                line == value
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun checkBuildFields(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.PRODUCT.contains("sdk") ||
                Build.PRODUCT.contains("google_sdk") ||
                Build.PRODUCT.contains("sdk_google") ||
                Build.BOARD.lowercase().contains("nox") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
    }

    private fun checkQemuDrivers(): Boolean {
        return try {
            val drivers = File("/proc/tty/drivers").readText()
            drivers.contains("goldfish") || drivers.contains("qemu")
        } catch (_: Exception) {
            false
        }
    }

    private fun checkEmulatorFiles(): Boolean {
        val emulatorFiles = arrayOf(
            "/system/lib/libc_malloc_debug_qemu.so",
            "/system/lib/libc_malloc_debug_leak.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props"
        )
        return emulatorFiles.any { File(it).exists() }
    }

    private fun getApkSignature(context: Context): String {
        return try {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val flags = PackageManager.GET_SIGNING_CERTIFICATES
                pm.getPackageInfo(context.packageName, flags)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            val certBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = info.signingInfo ?: return ""
                val certs = signingInfo.apkContentsSigners
                certs.firstOrNull()?.toByteArray() ?: return ""
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.getOrNull(0)?.toByteArray() ?: return ""
            }
            val digest = MessageDigest.getInstance("SHA-256").digest(certBytes)
            android.util.Base64.encodeToString(digest, android.util.Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }
}
