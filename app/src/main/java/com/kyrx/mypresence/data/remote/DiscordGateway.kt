package com.kyrx.mypresence.data.remote

import com.kyrx.mypresence.core.utils.Constants
import com.kyrx.mypresence.core.gateway.GatewayConnectionState
import com.kyrx.mypresence.domain.model.PresenceData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Singleton
class DiscordGateway @Inject constructor(
    private val json: Json,
    private val okHttpClient: OkHttpClient
) {
    private val _state = MutableStateFlow<GatewayConnectionState>(GatewayConnectionState.Disconnected)
    val state: StateFlow<GatewayConnectionState> = _state.asStateFlow()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var connectionJob: Job? = null
    private var reconnectJob: Job? = null
    private var heartbeatInterval: Long = 41250
    private var sequenceNumber: Int? = null
    private var sessionId: String? = null
    @Volatile private var currentToken: String? = null
    @Volatile private var shouldReconnect = false
    @Volatile private var connected = false
    @Volatile private var keepOnlineMode = false
    private var reconnectAttempts = 0
    private companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val MAX_KEEP_ONLINE_ATTEMPTS = 10000
        private const val CONNECT_TIMEOUT_MS = 25000L
    }

    val isConnected: Boolean get() = _state.value is GatewayConnectionState.Connected

    suspend fun awaitConnected(timeoutMs: Long = CONNECT_TIMEOUT_MS) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (isConnected) return
            delay(100)
        }
        throw CancellationException("Gateway did not connect within ${timeoutMs}ms")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun setKeepOnlineMode(enabled: Boolean) {
        keepOnlineMode = enabled
    }

    fun connect(token: String) {
        currentToken = token
        shouldReconnect = true
        reconnectAttempts = 0
        disconnectInternal()
        connectionJob = scope.launch { connectInternal(token) }
    }

    private suspend fun connectInternal(token: String) {
        reconnectAttempts++
        val maxAttempts = if (keepOnlineMode) MAX_KEEP_ONLINE_ATTEMPTS else MAX_RECONNECT_ATTEMPTS
        if (reconnectAttempts > maxAttempts) {
            _state.value = GatewayConnectionState.Error("Max reconnection attempts reached")
            shouldReconnect = false
            return
        }
        _state.value = GatewayConnectionState.Connecting
        try {
            val request = Request.Builder()
                .url(Constants.DISCORD_WS_URL)
                .build()

            val latch = java.util.concurrent.CountDownLatch(1)
            var opened = false

            okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    webSocket = ws
                    opened = true
                    latch.countDown()
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    scope.launch { handleFrame(text) }
                }

                override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                    ws.close(code, reason)
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    webSocket = null
                    if (shouldReconnect) {
                        connected = false
                        scope.launch { scheduleReconnect() }
                    }
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    webSocket = null
                    opened = false
                    latch.countDown()
                    if (shouldReconnect) {
                        connected = false
                        scope.launch { scheduleReconnect() }
                    }
                }
            })

            latch.await(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            if (!opened) {
                _state.value = GatewayConnectionState.Error("Connection timeout")
                if (shouldReconnect) scheduleReconnect()
            } else {
                connected = true
            }
        } catch (e: Exception) {
            _state.value = GatewayConnectionState.Error(e.message ?: "Connection failed")
            if (shouldReconnect) scheduleReconnect()
        }
    }

    private suspend fun scheduleReconnect() {
        delay(5000)
        if (shouldReconnect && currentToken != null) {
            connectInternal(currentToken!!)
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
            })
        }
        try { webSocket?.send(identify.toString()) } catch (_: Exception) {}
    }

    private suspend fun handleDispatch(payload: JsonObject) {
        val t = payload["t"]?.jsonPrimitive?.contentOrNull
        sequenceNumber = payload["s"]?.jsonPrimitive?.intOrNull ?: sequenceNumber

        when (t) {
            "READY" -> {
                reconnectAttempts = 0
                sessionId = payload["d"]?.jsonObject?.get("session_id")?.jsonPrimitive?.content
                _state.value = GatewayConnectionState.Connected(
                    sessionId = sessionId ?: "",
                    sequence = sequenceNumber
                )
            }
            "RESUMED" -> {
                reconnectAttempts = 0
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
        _state.value = GatewayConnectionState.Error("Invalid session - token may be expired")
        shouldReconnect = false
        currentToken = null
        disconnectInternal()
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                val payload = buildJsonObject {
                    put("op", 1)
                    put("d", sequenceNumber?.let { JsonPrimitive(it) } ?: JsonPrimitive(null))
                }
                try { webSocket?.send(payload.toString()) } catch (_: Exception) {}
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
        try { webSocket?.send(payload.toString()) } catch (_: Exception) {}
    }

    private fun disconnectInternal() {
        shouldReconnect = false
        connected = false
        heartbeatJob?.cancel()
        connectionJob?.cancel()
        reconnectJob?.cancel()
        try { webSocket?.close(1000, "Disconnected") } catch (_: Exception) {}
        webSocket = null
    }

    fun disconnect() {
        disconnectInternal()
        sessionId = null
        sequenceNumber = null
        currentToken = null
        _state.value = GatewayConnectionState.Disconnected
    }

    fun destroy() {
        disconnect()
        scope.cancel()
        okHttpClient.dispatcher.executorService.shutdown()
    }
}
