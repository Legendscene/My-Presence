package com.kyrx.mypresence.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class DiscordUser(
    val id: String = "",
    val username: String = "",
    val discriminator: String = "0",
    val global_name: String? = null,
    val avatar: String? = null
)

@Serializable
data class TokenResponse(
    val access_token: String = "",
    val token_type: String = "",
    val expires_in: Int = 0,
    val refresh_token: String = "",
    val scope: String = ""
)

@Singleton
class DiscordApi @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json
) {
    companion object {
        private const val BASE_URL = "https://discord.com/api/v10"
    }

    suspend fun exchangeCode(code: String, clientId: String, clientSecret: String, redirectUri: String): TokenResponse {
        val response = httpClient.post("$BASE_URL/oauth2/token") {
            url {
                parameters.append("client_id", clientId)
                parameters.append("client_secret", clientSecret)
                parameters.append("grant_type", "authorization_code")
                parameters.append("code", code)
                parameters.append("redirect_uri", redirectUri)
            }
        }
        return json.decodeFromString(response.bodyAsText())
    }

    suspend fun getCurrentUser(accessToken: String): DiscordUser {
        val response = httpClient.get("$BASE_URL/users/@me") {
            header("Authorization", "Bearer $accessToken")
        }
        return json.decodeFromString(response.bodyAsText())
    }

    suspend fun refreshToken(refreshToken: String, clientId: String, clientSecret: String): TokenResponse {
        val response = httpClient.post("$BASE_URL/oauth2/token") {
            url {
                parameters.append("client_id", clientId)
                parameters.append("client_secret", clientSecret)
                parameters.append("grant_type", "refresh_token")
                parameters.append("refresh_token", refreshToken)
            }
        }
        return json.decodeFromString(response.bodyAsText())
    }
}
