package com.kyrx.mypresence.feature.auth

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kyrx.mypresence.core.utils.PKCE
import com.kyrx.mypresence.domain.repository.AuthRepository
import com.kyrx.mypresence.domain.repository.AuthState
import com.kyrx.mypresence.domain.usecase.PerformOAuthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val performOAuth: PerformOAuthUseCase,
    application: Application
) : AndroidViewModel(application) {

    val authState: StateFlow<AuthState> = authRepository.authState

    private val _isOAuthInProgress = MutableStateFlow(false)
    val isOAuthInProgress: StateFlow<Boolean> = _isOAuthInProgress.asStateFlow()

    private val _oauthError = MutableStateFlow<String?>(null)
    val oauthError: StateFlow<String?> = _oauthError.asStateFlow()

    fun startOAuth() {
        if (_isOAuthInProgress.value) return
        _oauthError.value = null
        _isOAuthInProgress.value = true
        Log.d("AuthViewModel", "Starting OAuth")

        val params = authRepository.prepareAuthorization()
        val challenge = PKCE.generateCodeChallenge(params.codeVerifier)
        val url = authRepository.buildAuthorizationUrl(params.state, challenge)

        val application = getApplication<Application>()
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .apply {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    launchUrl(application, Uri.parse(url))
                }
        } catch (e: ActivityNotFoundException) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                application.startActivity(intent)
            } catch (fallbackError: Exception) {
                Log.e("AuthViewModel", "Browser launch failed: ${fallbackError.message}")
                _isOAuthInProgress.value = false
                _oauthError.value = "No browser found to open Discord login."
                authRepository.cancelOAuth()
                return
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Custom tab launch failed: ${e.message}")
            _isOAuthInProgress.value = false
            _oauthError.value = "Could not open Discord login: ${e.message}"
            authRepository.cancelOAuth()
            return
        }

        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Waiting for Discord deep-link callback...")
                val result = performOAuth()
                if (result.isError) {
                    _oauthError.value = result.errorMessageOrNull()
                    authRepository.cancelOAuth()
                } else {
                    Log.d("AuthViewModel", "Token exchange done, authState=${authState.value}")
                }
            } catch (e: CancellationException) {
                Log.d("AuthViewModel", "OAuth cancelled")
            } catch (e: TimeoutCancellationException) {
                Log.e("AuthViewModel", "OAuth timed out")
                if (authState.value !is AuthState.Authenticated) {
                    _oauthError.value = "OAuth timed out. Try again."
                    authRepository.cancelOAuth()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "OAuth error: ${e::class.simpleName} - ${e.message}")
                if (authState.value !is AuthState.Authenticated) {
                    _oauthError.value = "Error: ${e.message}"
                    authRepository.cancelOAuth()
                }
            } finally {
                _isOAuthInProgress.value = false
                Log.d("AuthViewModel", "OAuth flow ended")
            }
        }
    }

    fun cancelOAuth() {
        Log.d("AuthViewModel", "cancelOAuth called")
        authRepository.cancelOAuth()
        _isOAuthInProgress.value = false
        _oauthError.value = null
    }

    fun getDiscordAccessToken(): String? = authRepository.getAccessToken()

    fun clearOAuthError() {
        _oauthError.value = null
    }
}
