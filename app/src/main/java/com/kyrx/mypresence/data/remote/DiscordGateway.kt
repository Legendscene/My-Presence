package com.kyrx.mypresence.data.remote

import com.kyrx.mypresence.core.utils.Constants
import com.kyrx.mypresence.domain.model.GatewayConnectionState
import com.kyrx.mypresence.domain.model.PresenceData
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Singleton
class DiscordGateway @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json
) {
    private val _state = MutableStateFlow<GatewayConnectionState>(GatewayConnectionState.Disconnected)
    val state: StateFlow<GatewayConnectionState> = _state.asStateFlow()

    private var heartbeatJob: Job? = null
    private var connectionJob: Job? = null
    @Volatile private var session: WebSocketSession? = null
    private var heartbeatInterval: Long = 41250
    private var sequenceNumber: Int? = null
    private var sessionId: String? = null
    private var currentToken: String? = null
    private var shouldReconnect = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun connect(token: String) {
        currentToken = token
        shouldReconnect = true
        connectionJob?.cancel()
        connectionJob = scope.launch { connectInternal(token) }
    }

    private suspend fun connectInternal(token: String) {
        _state.value = GatewayConnectionState.Connecting
        try {
            httpClient.webSocket(Constants.DISCORD_WS_URL) {
                session = this
                _state.value = GatewayConnectionState.Connecting
                try {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> handleFrame(frame.readText())
                            else -> {}
                        }
                    }
                } finally {
                    session = null
                }
            }
        } catch (e: Exception) {
            if (shouldReconnect) {
                _state.value = GatewayConnectionState.Error(e.message ?: "Connection failed")
                delay(3000)
                if (shouldReconnect && currentToken != null) {
                    connectInternal(currentToken!!)
                }
            }
        }
    }

    private suspend fun handleFrame(text: String) {
        try {
            val payload = json.decodeFromString<JsonObject>(text)
            val op = payload["op"]?.jsonPrimitive?.intOrNull ?: return
            sequenceNumber = payload["s"]?.jsonPrimitive?.intOrNull ?: sequenceNumber

            when (op) {
                10 -> {
                    val hello = payload["d"]?.jsonObject ?: return
                    heartbeatInterval = hello["heartbeat_interval"]?.jsonPrimitive?.longOrNull ?: 41250
                    startHeartbeat()
                    sendIdentify()
                }
                0 -> handleDispatch(payload)
                9 -> handleInvalidSession()
            }
        } catch (_: Exception) {}
    }

    private suspend fun sendIdentify() {
        val tk = currentToken ?: return
        val identify = buildJsonObject {
            put("op", 2)
            put("d", buildJsonObject {
                put("token", tk)
                put("properties", buildJsonObject {
                    put("\$os", "android")
                    put("\$browser", "My Presence")
                    put("\$device", "My Presence")
                })
                put("presence", buildJsonObject {
                    put("status", "online")
                    put("afk", false)
                    put("activities", buildJsonArray {})
                })
            })
        }
        session?.send(Frame.Text(identify.toString()))
    }

    private suspend fun handleDispatch(payload: JsonObject) {
        val t = payload["t"]?.jsonPrimitive?.contentOrNull
        sequenceNumber = payload["s"]?.jsonPrimitive?.intOrNull ?: sequenceNumber

        when (t) {
            "READY" -> {
                sessionId = payload["d"]?.jsonObject?.get("session_id")?.jsonPrimitive?.content
                _state.value = GatewayConnectionState.Connected(
                    sessionId = sessionId ?: "",
                    sequence = sequenceNumber
                )
            }
            "RESUMED" -> {
                _state.value = GatewayConnectionState.Connected(
                    sessionId = sessionId ?: "",
                    sequence = sequenceNumber
                )
            }
        }
    }

    private suspend fun handleInvalidSession() {
        heartbeatJob?.cancel()
        sessionId = null
        sequenceNumber = null
        _state.value = GatewayConnectionState.Disconnected
        delay(5000)
        if (shouldReconnect && currentToken != null) {
            connectInternal(currentToken!!)
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                val payload = buildJsonObject {
                    put("op", 1)
                    put("d", sequenceNumber?.let { JsonPrimitive(it) } ?: JsonPrimitive(null))
                }
                try { session?.send(Frame.Text(payload.toString())) } catch (_: Exception) {}
                delay(heartbeatInterval)
            }
        }
    }

    suspend fun updatePresence(presence: PresenceData) {
        val activities = buildJsonArray {
            add(buildJsonObject {
                put("name", presence.name)
                put("type", presence.type)
                put("details", presence.details)
                put("state", presence.state)
                if (presence.startTimestamp != null || presence.endTimestamp != null) {
                    put("timestamps", buildJsonObject {
                        presence.startTimestamp?.let { put("start", it) }
                        presence.endTimestamp?.let { put("end", it) }
                    })
                }
                val assetsJson = buildJsonObject {
                    if (presence.largeImage.isNotEmpty()) put("large_image", presence.largeImage)
                    if (presence.largeText.isNotEmpty()) put("large_text", presence.largeText)
                    if (presence.smallImage.isNotEmpty()) put("small_image", presence.smallImage)
                    if (presence.smallText.isNotEmpty()) put("small_text", presence.smallText)
                }
                if (assetsJson.isNotEmpty()) put("assets", assetsJson)
            })
        }

        val payload = buildJsonObject {
            put("op", 4)
            put("d", buildJsonObject {
                put("since", null)
                put("activities", activities)
                put("status", presence.status)
                put("afk", false)
            })
        }
        try { session?.send(Frame.Text(payload.toString())) } catch (_: Exception) {}
    }

    fun disconnect() {
        shouldReconnect = false
        currentToken = null
        heartbeatJob?.cancel()
        connectionJob?.cancel()
        scope.launch {
            try { session?.close(CloseReason(CloseReason.Codes.NORMAL, "Disconnected")) } catch (_: Exception) {}
            session = null
            sessionId = null
            sequenceNumber = null
            _state.value = GatewayConnectionState.Disconnected
        }
    }

    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
