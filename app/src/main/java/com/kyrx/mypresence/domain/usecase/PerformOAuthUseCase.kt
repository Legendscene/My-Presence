package com.kyrx.mypresence.domain.usecase

import android.util.Log
import com.kyrx.mypresence.core.common.AppResult
import com.kyrx.mypresence.core.utils.OAuthCallbackHandler
import com.kyrx.mypresence.domain.repository.AuthRepository
import javax.inject.Inject

class PerformOAuthUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return try {
            Log.d("PerformOAuthUseCase", "Awaiting OAuth callback...")
            val result = OAuthCallbackHandler.awaitCallback()
            Log.d("PerformOAuthUseCase", "Callback received: error=${result.error}")

            if (result.error != null) {
                return AppResult.error(result.errorDescription ?: result.error)
            }

            val code = result.code
            val returnedState = result.state
            if (code.isNullOrBlank() || returnedState.isNullOrBlank()) {
                return AppResult.error("Discord login returned an invalid callback")
            }

            val pendingParams = authRepository.consumePendingParams()
            if (pendingParams == null) {
                return AppResult.error("OAuth session expired")
            }

            if (returnedState != pendingParams.state) {
                Log.e("PerformOAuthUseCase", "State mismatch: expected=${pendingParams.state} got=$returnedState")
                return AppResult.error("State mismatch")
            }

            Log.d("PerformOAuthUseCase", "State validated, exchanging code for token...")
            authRepository.handleAuthorizationResponse(
                code = code,
                expectedState = pendingParams.state,
                codeVerifier = pendingParams.codeVerifier
            )
            Log.d("PerformOAuthUseCase", "OAuth flow complete!")
            AppResult.success(Unit)
        } catch (e: Exception) {
            Log.e("PerformOAuthUseCase", "OAuth failed: ${e.message}")
            AppResult.error(e.message ?: "OAuth failed", e)
        }
    }
}
