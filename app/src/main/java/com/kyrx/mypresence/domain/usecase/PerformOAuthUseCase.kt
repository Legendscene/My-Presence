package com.kyrx.mypresence.domain.usecase

import com.kyrx.mypresence.core.common.AppResult
import com.kyrx.mypresence.core.utils.OAuthCallbackHandler
import com.kyrx.mypresence.core.utils.PKCE
import com.kyrx.mypresence.domain.repository.AuthRepository
import javax.inject.Inject

class PerformOAuthUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        val params = authRepository.prepareAuthorization()
        val challenge = PKCE.generateCodeChallenge(params.codeVerifier)
        val url = authRepository.buildAuthorizationUrl(params.state, challenge)

        return try {
            val result = OAuthCallbackHandler.awaitCallback()
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
                return AppResult.error("State mismatch")
            }

            authRepository.handleAuthorizationResponse(
                code = code,
                expectedState = pendingParams.state,
                codeVerifier = pendingParams.codeVerifier
            )
            AppResult.success(Unit)
        } catch (e: Exception) {
            AppResult.error(e.message ?: "OAuth failed", e)
        }
    }
}
