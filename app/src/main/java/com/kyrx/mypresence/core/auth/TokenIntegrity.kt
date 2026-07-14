package com.kyrx.mypresence.core.auth

import android.util.Log
import java.security.MessageDigest

object TokenIntegrity {
    private const val TAG = "TOKEN_INTEGRITY"

    data class TokenFingerprint(
        val sha256: String,
        val length: Int,
        val first8: String,
        val last8: String
    )

    var capturedFingerprint: TokenFingerprint? = null
        private set

    fun computeFingerprint(token: String): TokenFingerprint {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        return TokenFingerprint(
            sha256 = hash,
            length = token.length,
            first8 = token.take(8),
            last8 = token.takeLast(8)
        )
    }

    fun recordCapture(token: String) {
        val fp = computeFingerprint(token)
        capturedFingerprint = fp
        Log.d(TAG, "╔══════════════════════════════════════════╗")
        Log.d(TAG, "║         TOKEN CAPTURED (WebView)        ║")
        Log.d(TAG, "╚══════════════════════════════════════════╝")
        Log.d(TAG, "  Length:        ${fp.length}")
        Log.d(TAG, "  SHA-256:       ${fp.sha256}")
        Log.d(TAG, "  First 8:       \"${fp.first8}\"")
        Log.d(TAG, "  Last 8:        \"${fp.last8}\"")
        logIntegrity(token)
        Log.d(TAG, "╚══════════════════════════════════════════╝")
    }

    fun verifyToken(label: String, token: String, stage: String) {
        val fp = computeFingerprint(token)
        val captured = capturedFingerprint
        Log.d(TAG, "╔══════════════════════════════════════════╗")
        Log.d(TAG, "║  TOKEN VERIFY: $label")
        Log.d(TAG, "║  Stage: $stage")
        Log.d(TAG, "╚══════════════════════════════════════════╝")
        Log.d(TAG, "  Length:        ${fp.length}")
        Log.d(TAG, "  SHA-256:       ${fp.sha256}")
        Log.d(TAG, "  First 8:       \"${fp.first8}\"")
        Log.d(TAG, "  Last 8:        \"${fp.last8}\"")
        if (captured != null) {
            val match = fp == captured
            Log.d(TAG, "  MATCH capture: ${if (match) "✓ YES" else "✗ NO — MISMATCH!"}")
            if (!match) {
                Log.w(TAG, "  ⚠ TOKEN CHANGED since capture!")
                Log.w(TAG, "  Capture was: length=${captured.length} sha256=${captured.sha256}")
                Log.w(TAG, "  Capture was: first8=\"${captured.first8}\" last8=\"${captured.last8}\"")
                Log.w(TAG, "  Current is:  length=${fp.length} sha256=${fp.sha256}")
                Log.w(TAG, "  Current is:  first8=\"${fp.first8}\" last8=\"${fp.last8}\"")
            } else {
                Log.d(TAG, "  ✓ Token integrity preserved through $label")
            }
        } else {
            Log.d(TAG, "  No capture fingerprint recorded (not yet captured)")
        }
        logIntegrity(token)
        Log.d(TAG, "╚══════════════════════════════════════════╝")
    }

    private fun logIntegrity(token: String) {
        val hasWhitespace = token.any { it.isWhitespace() }
        val containsBot = token.contains("Bot", ignoreCase = true)
        val containsBearer = token.contains("Bearer", ignoreCase = true)
        val hasNewlines = token.contains("\n") || token.contains("\r")
        val allPrintable = token.all { it.code in 32..126 }
        val nullChars = token.count { it.code == 0 }
        val utf8Bytes = token.toByteArray(Charsets.UTF_8)
        Log.d(TAG, "  Integrity checks:")
        Log.d(TAG, "    Whitespace:     $hasWhitespace")
        Log.d(TAG, "    Newlines:       $hasNewlines")
        Log.d(TAG, "    Contains 'Bot': $containsBot (should be false for user tokens)")
        Log.d(TAG, "    Contains 'Bearer': $containsBearer (should be false)")
        Log.d(TAG, "    All printable ASCII: $allPrintable")
        Log.d(TAG, "    Null chars (0x00): $nullChars")
        Log.d(TAG, "    UTF-8 bytes:    ${utf8Bytes.size} (chars: ${token.length})")
        Log.d(TAG, "    Multi-byte:     ${utf8Bytes.size != token.length}")
    }
}
