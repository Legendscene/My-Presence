package com.kyrx.mypresence.data.remote

import android.util.Log
import com.kyrx.mypresence.BuildConfig
import com.kyrx.mypresence.core.analytics.CrashReporter
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ImgurResponse(
    @SerialName("data") val data: ImgurData
)

@Serializable
data class ImgurData(
    @SerialName("link") val link: String
)

@Singleton
class ImgurUploader @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val crashReporter: CrashReporter
) {
    companion object {
        private const val TAG = "IMGUR_UPLOADER"
        private const val CATBOX_UPLOAD_URL = "https://catbox.moe/user/api.php"
        private const val LITTERBOX_UPLOAD_URL = "https://litterbox.catbox.moe/resources/internals/api.php"
        private const val ZERO_X_ZERO_UPLOAD_URL = "https://0x0.st"
        private const val IMGUR_API_URL = "https://api.imgur.com/3/image"
        private const val UPLOAD_TIMEOUT_MS = 12_000L
    }

    suspend fun upload(file: File): String? {
        val fileSize = file.length()
        val fileBytes = file.readBytes()
        Log.d(TAG, "upload: file=${file.name} size=${fileSize}b")
        crashReporter.log("ImgurUploader: uploading ${file.name} (${fileSize}b)")

        uploadToCatbox(file, fileBytes)?.let { return it }
        uploadToLitterbox(file, fileBytes)?.let { return it }
        uploadToZeroXZero(file, fileBytes)?.let { return it }

        val imgurClientId = BuildConfig.IMGUR_CLIENT_ID.trim()
        if (imgurClientId.isBlank()) {
            Log.w(TAG, "upload failed: IMGUR_CLIENT_ID is blank and keyless upload failed")
            crashReporter.log("ImgurUploader: no configured fallback client id")
            return null
        }

        return uploadToImgur(file, fileBytes, imgurClientId)
    }

    private suspend fun uploadToCatbox(file: File, fileBytes: ByteArray): String? {
        Log.i("ASSET_UPLOAD", "Catbox upload request")
        Log.i("ASSET_UPLOAD", "  POST $CATBOX_UPLOAD_URL")
        return withTimeoutOrNull(UPLOAD_TIMEOUT_MS) {
            try {
                val response = httpClient.post(CATBOX_UPLOAD_URL) {
                    contentType(ContentType.MultiPart.FormData)
                    setBody(MultiPartFormDataContent(
                        formData {
                            append("reqtype", "fileupload")
                            append("fileToUpload", fileBytes, Headers.build {
                                append(HttpHeaders.ContentType, "image/png")
                                append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                            })
                        }
                    ))
                }
                val bodyText = response.bodyAsText().trim()
                val status = response.status.value
                Log.i("ASSET_UPLOAD", "Catbox upload response status=$status body=$bodyText")
                if (status in 200..299 && bodyText.startsWith("http")) bodyText else null
            } catch (e: Exception) {
                Log.e(TAG, "Catbox upload exception: ${e.message}")
                null
            }
        }.also {
            if (it == null) Log.w(TAG, "Catbox upload failed or timed out")
        }
    }

    private suspend fun uploadToLitterbox(file: File, fileBytes: ByteArray): String? {
        Log.i("ASSET_UPLOAD", "Litterbox upload request")
        Log.i("ASSET_UPLOAD", "  POST $LITTERBOX_UPLOAD_URL")
        return withTimeoutOrNull(UPLOAD_TIMEOUT_MS) {
            try {
                val response = httpClient.post(LITTERBOX_UPLOAD_URL) {
                    contentType(ContentType.MultiPart.FormData)
                    setBody(MultiPartFormDataContent(
                        formData {
                            append("reqtype", "fileupload")
                            append("time", "1h")
                            append("fileToUpload", fileBytes, Headers.build {
                                append(HttpHeaders.ContentType, "image/png")
                                append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                            })
                        }
                    ))
                }
                val bodyText = response.bodyAsText().trim()
                val status = response.status.value
                Log.i("ASSET_UPLOAD", "Litterbox upload response status=$status body=$bodyText")
                if (status in 200..299 && bodyText.startsWith("http")) bodyText else null
            } catch (e: Exception) {
                Log.e(TAG, "Litterbox upload exception: ${e.message}")
                null
            }
        }.also {
            if (it == null) Log.w(TAG, "Litterbox upload failed or timed out")
        }
    }

    private suspend fun uploadToZeroXZero(file: File, fileBytes: ByteArray): String? {
        Log.i("ASSET_UPLOAD", "0x0.st upload request")
        Log.i("ASSET_UPLOAD", "  POST $ZERO_X_ZERO_UPLOAD_URL")
        Log.i("ASSET_UPLOAD", "  multipart: file (${fileBytes.size} bytes)")

        return withTimeoutOrNull(UPLOAD_TIMEOUT_MS) {
            try {
            val response = httpClient.post(ZERO_X_ZERO_UPLOAD_URL) {
                contentType(ContentType.MultiPart.FormData)
                setBody(MultiPartFormDataContent(
                    formData {
                        append("file", fileBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                        })
                    }
                ))
            }
            val bodyText = response.bodyAsText().trim()
            val status = response.status.value

            Log.i("ASSET_UPLOAD", "0x0.st upload response")
            Log.i("ASSET_UPLOAD", "  status: $status")
            Log.i("ASSET_UPLOAD", "  body: $bodyText")

            if (status in 200..299 && bodyText.startsWith("http")) {
                crashReporter.log("ImageUploader: keyless upload success")
                bodyText
            } else {
                Log.w(TAG, "0x0.st upload failed: status=$status body=${bodyText.take(200)}")
                null
            }
            } catch (e: Exception) {
                Log.e(TAG, "0x0.st upload exception: ${e.message}")
                crashReporter.log("ImageUploader: keyless exception ${e.message}")
                null
            }
        }.also {
            if (it == null) Log.w(TAG, "0x0.st upload failed or timed out")
        }
    }

    private suspend fun uploadToImgur(file: File, fileBytes: ByteArray, clientId: String): String? {
        Log.i("ASSET_UPLOAD", "Imgur upload request")
        Log.i("ASSET_UPLOAD", "  POST $IMGUR_API_URL")
        Log.i("ASSET_UPLOAD", "  Authorization: Client-ID ${clientId.take(4)}...")
        Log.i("ASSET_UPLOAD", "  multipart: image (${fileBytes.size} bytes), type=raw")
        Log.i("ASSET_UPLOAD", "  PNG first 32 bytes: ${fileBytes.take(32).joinToString(" ") { "%02X".format(it) }}")

        return try {
            val response = httpClient.post(IMGUR_API_URL) {
                header("Authorization", "Client-ID $clientId")
                contentType(ContentType.MultiPart.FormData)
                setBody(MultiPartFormDataContent(
                    formData {
                        append("image", fileBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                        })
                        append("type", "raw")
                    }
                ))
            }
            val bodyText = response.bodyAsText()
            val status = response.status.value

            Log.i("ASSET_UPLOAD", "Imgur upload response")
            Log.i("ASSET_UPLOAD", "  status: $status")
            Log.i("ASSET_UPLOAD", "  body: $bodyText")

            if (status == 200) {
                val parsed = json.decodeFromString<ImgurResponse>(bodyText)
                val link = parsed.data.link
                Log.i("ASSET_UPLOAD", "Imgur success link=$link")
                crashReporter.log("ImgurUploader: success link=$link")
                link
            } else {
                Log.w(TAG, "upload failed: status=$status body=${bodyText.take(200)}")
                crashReporter.log("ImgurUploader: failed status=$status")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "upload exception: ${e.message}")
            crashReporter.log("ImgurUploader: exception ${e.message}")
            Log.e("ASSET_UPLOAD", "Imgur exception ${e.message}")
            null
        }
    }
}
