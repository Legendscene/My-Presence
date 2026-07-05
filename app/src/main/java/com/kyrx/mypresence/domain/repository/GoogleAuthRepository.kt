package com.kyrx.mypresence.domain.repository

interface GoogleAuthRepository {
    suspend fun handleSignInResult(idToken: String): Result<GoogleUser>
    suspend fun signOut()
    fun isSignedIn(): Boolean
}

data class GoogleUser(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?
)
