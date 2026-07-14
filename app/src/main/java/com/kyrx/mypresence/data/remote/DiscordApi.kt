package com.kyrx.mypresence.data.remote

import android.util.Log
import com.kyrx.mypresence.core.analytics.CrashReporter
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class DiscordUser(
    val id: String = "",
    val username: String = "",
    val discriminator: String = "0",
    val display_name: String? = null,
    val global_name: String? = null,
    val avatar: String? = null,
    val avatar_decoration_data: AvatarDecorationData? = null,
    val banner: String? = null,
    val banner_color: String? = null,
    val accent_color: Int? = null,
    val pronouns: String? = null,
    val nameplate: Int? = null,
    val bio: String? = null,
    val bot: Boolean = false,
    val system: Boolean? = null,
    val flags: Int? = null,
    val public_flags: Int? = null,
    val premium_type: Int? = null,
    val locale: String? = null,
    val email: String? = null,
    val verified: Boolean? = null,
    val mfa_enabled: Boolean? = null,
    val phone: String? = null,
    val purchased_flags: Int? = null,
    val premium_usage_flags: Int? = null
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

@Serializable
data class DiscordExternalAsset(
    val url: String = "",
    @kotlinx.serialization.SerialName("external_asset_path")
    val externalAssetPath: String = ""
)

@Serializable
private data class ExternalAssetRequest(
    val urls: List<String>
)

enum class Badge(
    val flag: Int,
    val displayName: String,
    val iconRes: String
) {
    STAFF(1 shl 0, "Discord Staff", "ic_badge_staff"),
    PARTNER(1 shl 1, "Discord Partner", "ic_badge_partner"),
    HYPESQUAD(1 shl 2, "HypeSquad Events", "ic_badge_hypesquad"),
    BUG_HUNTER_LEVEL_1(1 shl 3, "Bug Hunter Level 1", "ic_badge_bug_hunter_1"),
    HYPESQUAD_ONLINE_HOUSE_1(1 shl 6, "HypeSquad Bravery", "ic_badge_bravery"),
    HYPESQUAD_ONLINE_HOUSE_2(1 shl 7, "HypeSquad Brilliance", "ic_badge_brilliance"),
    PREMIUM_EARLY_SUPPORTER(1 shl 8, "Early Supporter", "ic_badge_early_supporter"),
    BUG_HUNTER_LEVEL_2(1 shl 9, "Bug Hunter Level 2", "ic_badge_bug_hunter_2"),
    VERIFIED_BOT(1 shl 10, "Verified Bot", "ic_badge_verified_bot"),
    VERIFIED_DEVELOPER(1 shl 11, "Early Verified Bot Developer", "ic_badge_verified_developer"),
    CERTIFIED_MODERATOR(1 shl 12, "Certified Moderator", "ic_badge_certified_moderator"),
    BOT_HTTP_INTERACTIONS(1 shl 14, "Bot HTTP Interactions", "ic_badge_bot_http"),
    ACTIVE_DEVELOPER(1 shl 16, "Active Developer", "ic_badge_active_developer")
}

@Singleton
class DiscordApi @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val crashReporter: CrashReporter
) {
    companion object {
        private const val BASE_URL = "https://discord.com/api/v10"
        private const val TOKEN_URL = "https://discord.com/api/v10/oauth2/token"
        private const val REVOKE_URL = "https://discord.com/api/v10/oauth2/token/revoke"
        private const val CDN_URL = "https://cdn.discordapp.com"

        fun getUserBadges(publicFlags: Int): List<Badge> =
            Badge.entries.filter { (publicFlags and it.flag) != 0 }

        fun formatAvatarUrl(userId: String, avatarHash: String, size: Int = 512): String {
            val ext = if (avatarHash.startsWith("a_")) "gif" else "png"
            return "$CDN_URL/avatars/$userId/$avatarHash.$ext?size=$size"
        }

        fun formatAvatarDecorationUrl(userId: String, decorationHash: String): String =
            "$CDN_URL/avatar-decoration/$userId/$decorationHash.png"

        fun formatBannerUrl(userId: String, bannerHash: String, size: Int = 1080): String {
            val ext = if (bannerHash.startsWith("a_")) "gif" else "png"
            return "$CDN_URL/banners/$userId/$bannerHash.$ext?size=$size"
        }
    }

    suspend fun exchangeCode(
        code: String,
        codeVerifier: String,
        clientId: String,
        redirectUri: String
    ): TokenResponse {
        return try {
            val body = buildMap<String, String> {
                put("client_id", clientId)
                put("grant_type", "authorization_code")
                put("code", code)
                put("redirect_uri", redirectUri)
                put("code_verifier", codeVerifier)
            }

            val bodyEncoded = body.toFormUrlEncoded()
            crashReporter.log("exchangeCode: POST to $TOKEN_URL with redirect_uri=$redirectUri")

            val response = httpClient.post(TOKEN_URL) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(bodyEncoded)
            }

            val bodyText = response.bodyAsText().take(500)
            crashReporter.log("exchangeCode response: status=${response.status.value} body=$bodyText")

            if (response.status.value != 200) {
                throw Exception("Discord API error (${response.status.value}): $bodyText")
            }

            val tokenResponse: TokenResponse = try {
                json.decodeFromString(bodyText)
            } catch (e: Exception) {
                crashReporter.log("exchangeCode: JSON parsing failed: ${e.message}")
                throw Exception("Failed to parse token response: ${e.message}")
            }

            if (tokenResponse.access_token.isBlank()) {
                throw Exception("Token exchange failed: empty access_token")
            }
            tokenResponse
        } catch (e: Exception) {
            crashReporter.logNonFatal(e)
            crashReporter.log("exchangeCode failed: ${e.message}")
            throw e
        }
    }

    suspend fun refreshToken(
        refreshToken: String,
        clientId: String
    ): TokenResponse {
        return try {
            val body = buildMap<String, String> {
                put("client_id", clientId)
                put("grant_type", "refresh_token")
                put("refresh_token", refreshToken)
            }

            val response = httpClient.post(TOKEN_URL) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(body.toFormUrlEncoded())
            }
            json.decodeFromString(response.bodyAsText())
        } catch (e: Exception) {
            crashReporter.logNonFatal(e)
            crashReporter.log("refreshToken failed: ${e.message}")
            throw e
        }
    }

    suspend fun revokeToken(
        accessToken: String,
        clientId: String
    ) {
        try {
            val body = buildMap<String, String> {
                put("client_id", clientId)
                put("token", accessToken)
            }

            httpClient.post(REVOKE_URL) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(body.toFormUrlEncoded())
            }
        } catch (e: Exception) {
            crashReporter.log("revokeToken failed: ${e.message}")
        }
    }

    suspend fun getCurrentUser(accessToken: String): DiscordUser {
        return try {
            val response = httpClient.get("$BASE_URL/users/@me") {
                header("Authorization", "Bearer $accessToken")
            }
            json.decodeFromString(response.bodyAsText())
        } catch (e: Exception) {
            crashReporter.logNonFatal(e)
            crashReporter.log("getCurrentUser failed: ${e.message}")
            throw e
        }
    }

    suspend fun getCurrentUserWithUserToken(userToken: String): DiscordUser {
        return try {
            val response = httpClient.get("$BASE_URL/users/@me") {
                header("Authorization", userToken)
            }
            val rawJson = response.bodyAsText()
            Log.d("API_RESPONSE", "getCurrentUserWithUserToken: status=${response.status.value}")
            Log.d("API_RESPONSE", "raw JSON body: $rawJson")
            json.decodeFromString(rawJson)
        } catch (e: Exception) {
            crashReporter.logNonFatal(e)
            crashReporter.log("getCurrentUserWithUserToken failed: ${e.message}")
            throw e
        }
    }

    suspend fun resolveExternalAsset(
        userToken: String,
        applicationId: String,
        imageUrl: String
    ): String? {
        if (userToken.isBlank() || applicationId.isBlank() || imageUrl.isBlank()) return null
        Log.i("ASSET_UPLOAD", "Discord external-assets request")
        Log.i("ASSET_UPLOAD", "  POST $BASE_URL/applications/$applicationId/external-assets")
        Log.i("ASSET_UPLOAD", "  Authorization: ${userToken.take(8)}... (len=${userToken.length})")
        Log.i("ASSET_UPLOAD", "  body: {\"urls\":[\"$imageUrl\"]}")
        return try {
            val response = httpClient.post("$BASE_URL/applications/$applicationId/external-assets") {
                header("Authorization", userToken)
                contentType(ContentType.Application.Json)
                setBody(ExternalAssetRequest(listOf(imageUrl)))
            }
            val body = response.bodyAsText()
            Log.i("ASSET_UPLOAD", "Discord external-assets response")
            Log.i("ASSET_UPLOAD", "  status: ${response.status.value}")
            Log.i("ASSET_UPLOAD", "  body: $body")
            if (response.status.value !in 200..299) {
                crashReporter.log("resolveExternalAsset failed status=${response.status.value} body=${body.take(200)}")
                return null
            }
            val assets = json.decodeFromString(ListSerializer(DiscordExternalAsset.serializer()), body)
            val mpKey = assets.firstOrNull()?.externalAssetPath?.takeIf { it.isNotBlank() }?.let { "mp:$it" }
            Log.i("ASSET_UPLOAD", "parsed mpKey=$mpKey")
            mpKey
        } catch (e: Exception) {
            crashReporter.log("resolveExternalAsset failed: ${e.message}")
            Log.e("ASSET_UPLOAD", "external-assets exception ${e.message}")
            null
        }
    }

    suspend fun findPlayStoreIconUrl(packageName: String): String? {
        if (packageName.isBlank()) return null
        return try {
            val response = httpClient.get("https://play.google.com/store/apps/details") {
                url {
                    parameters.append("id", packageName)
                    parameters.append("hl", "en")
                    parameters.append("gl", "US")
                }
            }
            val html = response.bodyAsText()
            val iconUrl = listOf(
                Regex("""<meta[^>]+property=["']og:image["'][^>]+content=["']([^"']+)["']"""),
                Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:image["']""")
            ).firstNotNullOfOrNull { regex ->
                regex.find(html)?.groupValues?.getOrNull(1)
            }?.replace("&amp;", "&")

            Log.i("ASSET_UPLOAD", "Play Store icon lookup pkg=$packageName icon=$iconUrl")
            iconUrl
        } catch (e: Exception) {
            Log.w("ASSET_UPLOAD", "Play Store icon lookup failed pkg=$packageName error=${e.message}")
            null
        }
    }
}

private fun Map<String, String>.toFormUrlEncoded(): String =
    entries.joinToString("&") { (key, value) ->
        "${key.encodeFormUrl()}=${value.encodeFormUrl()}"
    }

private fun String.encodeFormUrl(): String {
    return java.net.URLEncoder.encode(this, "UTF-8")
}
