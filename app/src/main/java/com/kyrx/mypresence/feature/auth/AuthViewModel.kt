package com.kyrx.mypresence.feature.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kyrx.mypresence.domain.repository.AuthRepository
import com.kyrx.mypresence.domain.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    application: Application
) : AndroidViewModel(application) {

    val authState = authRepository.authState

    private val _isLoginInProgress = MutableStateFlow(false)
    val isLoginInProgress: StateFlow<Boolean> = _isLoginInProgress.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _showMoreOptions = MutableStateFlow(false)
    val showMoreOptions: StateFlow<Boolean> = _showMoreOptions.asStateFlow()

    fun toggleMoreOptions() {
        _showMoreOptions.value = !_showMoreOptions.value
    }

    fun loginWithToken(token: String) {
        if (_isLoginInProgress.value) return
        _loginError.value = null
        _isLoginInProgress.value = true
        viewModelScope.launch {
            try {
                authRepository.loginWithToken(token.trim())
            } catch (e: Exception) {
                Log.e("AuthViewModel", "login failed: ${e.message}")
                _loginError.value = "Login failed: ${e.message}"
            } finally {
                _isLoginInProgress.value = false
            }
        }
    }

    fun handleGoogleSignIn(idToken: String, displayName: String?, email: String?) {
        viewModelScope.launch {
            try {
                authRepository.saveGoogleAccount(idToken, displayName, email)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign-in save failed: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun clearError() {
        _loginError.value = null
    }
}
