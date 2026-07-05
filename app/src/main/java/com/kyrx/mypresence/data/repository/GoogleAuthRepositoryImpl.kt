package com.kyrx.mypresence.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kyrx.mypresence.core.utils.GoogleAuthConfig
import com.kyrx.mypresence.domain.repository.GoogleAuthRepository
import com.kyrx.mypresence.domain.repository.GoogleUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GoogleAuthRepository {

    private val _currentUser = MutableStateFlow<GoogleUser?>(null)
    val currentUser: StateFlow<GoogleUser?> = _currentUser.asStateFlow()

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(GoogleAuthConfig.CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent() = googleSignInClient.signInIntent

    override suspend fun handleSignInResult(idToken: String): Result<GoogleUser> {
        return try {
            val user = GoogleUser(
                id = idToken,
                email = "",
                displayName = "Google User",
                photoUrl = null
            )
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        googleSignInClient.signOut()
        _currentUser.value = null
    }

    override fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null
    }
}
