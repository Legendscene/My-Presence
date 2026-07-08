package com.kyrx.mypresence.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyrx.mypresence.data.remote.DiscordUser
import com.kyrx.mypresence.data.remote.DiscordGateway
import com.kyrx.mypresence.domain.repository.AuthRepository
import com.kyrx.mypresence.domain.repository.AuthState
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val discordGateway: DiscordGateway
) : ViewModel() {

    val currentUser: StateFlow<DiscordUser?> = authRepository.authState.map { state ->
        if (state is AuthState.Authenticated) state.user else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun signOut() {
        viewModelScope.launch {
            discordGateway.disconnect()
            authRepository.logout()
            preferencesRepository.clearAll()
        }
    }
}
