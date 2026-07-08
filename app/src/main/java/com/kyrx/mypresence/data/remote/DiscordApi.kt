package com.kyrx.mypresence.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
    val avatar: String? = null,
    val avatar_decoration_data: AvatarDecorationData? = null,
    val banner: String? = null,
    val accent_color: Int? = null,
    val bot: Boolean = false
)

@Serializable
data class AvatarDecorationData(
    val asset: String = "",
    val sku_id: String? = null
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
        private const val TOKEN_URL = "https://discord.com/api/v10/oauth2/token"
        private const val REVOKE_URL = "https://discord.com/api/v10/oauth2/token/revoke"
    }

    suspend fun exchangeCode(
        code: String,
        codeVerifier: String,
        clientId: String,
        redirectUri: String
    ): TokenResponse {
        val body = buildMap<String, String> {
            put("client_id", clientId)
            put("grant_type", "authorization_code")
            put("code", code)
            put("redirect_uri", redirectUri)
            put("code_verifier", codeVerifier)
        }

        val response = httpClient.post(TOKEN_URL) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(body.toFormUrlEncoded())
        }

        val bodyText = response.bodyAsText()
        if (response.status.value != 200) {
            throw Exception("Discord API error (${response.status.value}): $bodyText")
        }

        val tokenResponse: TokenResponse = json.decodeFromString(bodyText)
        if (tokenResponse.access_token.isBlank()) {
            throw Exception("Token exchange failed: empty access_token")
        }
        return tokenResponse
    }

    suspend fun refreshToken(
        refreshToken: String,
        clientId: String
    ): TokenResponse {
        val body = buildMap<String, String> {
            put("client_id", clientId)
            put("grant_type", "refresh_token")
            put("refresh_token", refreshToken)
        }

        val response = httpClient.post(TOKEN_URL) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(body.toFormUrlEncoded())
        }
        return json.decodeFromString(response.bodyAsText())
    }

    suspend fun revokeToken(
        accessToken: String,
        clientId: String
    ) {
        val body = buildMap<String, String> {
            put("client_id", clientId)
            put("token", accessToken)
        }

        httpClient.post(REVOKE_URL) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(body.toFormUrlEncoded())
        }
    }

    suspend fun getCurrentUser(accessToken: String): DiscordUser {
        val response = httpClient.get("$BASE_URL/users/@me") {
            header("Authorization", "Bearer $accessToken")
        }
        return json.decodeFromString(response.bodyAsText())
    }
}

private fun Map<String, String>.toFormUrlEncoded(): String =
    entries.joinToString("&") { (key, value) ->
        "${key.encodeFormUrl()}=${value.encodeFormUrl()}"
    }

private fun String.encodeFormUrl(): String {
    return java.net.URLEncoder.encode(this, "UTF-8")
}
