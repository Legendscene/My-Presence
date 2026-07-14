package com.kyrx.mypresence.core.utils

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

data class CallbackResult(
    val code: String? = null,
    val state: String? = null,
    val error: String? = null,
    val errorDescription: String? = null
)

object OAuthCallbackHandler {
    private var deferred: CompletableDeferred<CallbackResult>? = null
    private var pendingResult: CallbackResult? = null

    suspend fun awaitCallback(timeoutMs: Long = 120_000L): CallbackResult {
        pendingResult?.let {
            Log.d("OAuthCallback", "Returning pending callback result")
            pendingResult = null
            return it
        }
        val d = CompletableDeferred<CallbackResult>()
        deferred = d
        Log.d("OAuthCallback", "Created deferred, awaiting callback")
        return withTimeout(timeoutMs) { d.await() }
    }

    fun handleCallback(result: CallbackResult): Boolean {
        Log.d("OAuthCallback", "handleCallback called: error=${result.error}")
        val d = deferred
        if (d != null && !d.isCompleted) {
            Log.d("OAuthCallback", "Completing existing deferred")
            return d.complete(result)
        }
        Log.d("OAuthCallback", "No deferred or already completed, saving as pending")
        pendingResult = result
        return true
    }

    fun cancel() {
        Log.d("OAuthCallback", "cancel() called")
        deferred?.let { d ->
            if (!d.isCompleted) {
                d.completeExceptionally(CancellationException("OAuth cancelled"))
            }
        }
        deferred = null
        pendingResult = null
    }

    fun hasPendingResult(): Boolean = pendingResult != null

    fun consumePendingResult(): CallbackResult? {
        val result = pendingResult
        pendingResult = null
        return result
    }
}
