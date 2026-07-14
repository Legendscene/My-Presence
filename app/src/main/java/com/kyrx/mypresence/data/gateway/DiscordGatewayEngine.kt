package com.kyrx.mypresence.data.gateway

import android.util.Log
import com.kyrx.mypresence.core.analytics.CrashReporter
import com.kyrx.mypresence.core.gateway.GatewayConfig
import com.kyrx.mypresence.core.gateway.GatewayConnectionState
import com.kyrx.mypresence.core.gateway.GatewayDiagnostics
import com.kyrx.mypresence.core.gateway.GatewayEngine
import com.kyrx.mypresence.core.gateway.GatewayEvent
import com.kyrx.mypresence.core.gateway.GatewayIdentityProvider
import com.kyrx.mypresence.core.utils.Constants
import com.kyrx.mypresence.data.remote.DiscordApi
import com.kyrx.mypresence.domain.model.PresenceData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GATEWAY_PIPELINE"
private val CONNECTION_ID_COUNTER = AtomicInteger(0)

private fun String.tokenHash(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(this.toByteArray(Charsets.UTF_8))
    return digest.take(8).joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Singleton
class DiscordGatewayEngine @Inject constructor(
    private val json: Json,
    private val okHttpClient: OkHttpClient,
    private val crashReporter: CrashReporter,
    private val discordApi: DiscordApi,
    private val config: GatewayConfig = GatewayConfig()
) : GatewayEngine {

    private val _state = MutableStateFlow<GatewayConnectionState>(GatewayConnectionState.Disconnected)
    override val state: StateFlow<GatewayConnectionState> = _state.asStateFlow()

    private val _diagnostics = MutableStateFlow(GatewayDiagnostics())
    override val diagnostics: StateFlow<GatewayDiagnostics> = _diagnostics.asStateFlow()

    private val _events = MutableSharedFlow<GatewayEvent>(extraBufferCapacity = 64, replay = 0)
    override val events: SharedFlow<GatewayEvent> = _events.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var connectionJob: Job? = null
    private var heartbeatInterval: Long = config.heartbeatIntervalMs
    private var sequenceNumber: Int? = null
    private var sessionId: String? = null
    private var resumeGatewayUrl: String? = null
    private var identityProvider: GatewayIdentityProvider? = null
    private var userId: String? = null
    private var lastOp3SentMs = 0L
    private var op3Sequence = AtomicInteger(0)
    private var dispatchCounter = AtomicInteger(0)
    private var heartbeatSentCount = 0

    @Volatile private var shouldReconnect = false
    @Volatile private var maxReconnectAttempts: Int = config.maxReconnectAttempts
    @Volatile private var helloReceived = false
    private var reconnectAttempts = 0
    private val connecting = AtomicBoolean(false)

    private var lastHeartbeatSent: AtomicLong = AtomicLong(0)
    private var missedHeartbeats: AtomicInteger = AtomicInteger(0)
    private var lastHeartbeatAck: AtomicLong = AtomicLong(0)

    private var lastPresenceData: PresenceData? = null
    private var lastPresencePayload: String? = null

    @Volatile private var resumePending = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isReconnecting = AtomicBoolean(false)

    // â”€â”€ Timeline instrumentation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private var timelineConnectionId: Int = 0
    private var timelineStartMs: Long = 0L

    private fun timeline(relativeMs: Long, opCode: String, event: String, detail: String = "") {
        val elapsed = if (timelineStartMs > 0) relativeMs - timelineStartMs else 0L
        val thread = Thread.currentThread().name
        val sid = sessionId?.take(8) ?: "-"
        Log.i("TIMELINE", "t=+${elapsed}ms [conn=${timelineConnectionId}] ${opCode.padEnd(6)} ${event.padEnd(30)} thread=${thread.padEnd(20)} session=${sid} ${detail}")
    }

    private fun timelineMark(): Long = System.currentTimeMillis()

    // â”€â”€ Token lifecycle hash tracking â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private var tokenHashLoaded: String? = null
    private var tokenHashIdentify: String? = null

    private fun logTokenLifecycle() {
        val loaded = tokenHashLoaded ?: "â€”"
        val identify = tokenHashIdentify ?: "â€”"
        val match = loaded == identify && loaded != "â€”"
        Log.i("TOKEN_LIFECYCLE", "â•”â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•—")
        Log.i("TOKEN_LIFECYCLE", "â•‘         TOKEN LIFECYCLE â€” GATEWAY SIDE             â•‘")
        Log.i("TOKEN_LIFECYCLE", "â•šâ•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•")
        Log.i("TOKEN_LIFECYCLE", "  See logcat: filter TOKEN_LIFECYCLE for full chain:")
        Log.i("TOKEN_LIFECYCLE", "    CAPTURED  (DiscordLoginWebView)")
        Log.i("TOKEN_LIFECYCLE", "    STORED    (AuthRepositoryImpl)")
        Log.i("TOKEN_LIFECYCLE", "    LOADED    (UserTokenProvider)     hash=${loaded}")
        Log.i("TOKEN_LIFECYCLE", "    IDENTIFY  (GatewayEngine.send)    hash=${identify}")
        if (match) {
            Log.i("TOKEN_LIFECYCLE", "  GATEWAY CHECK: âœ“ LOADED == IDENTIFY â€” token preserved through serialization")
        } else {
            Log.e("TOKEN_LIFECYCLE", "  GATEWAY CHECK: âœ— LOADED != IDENTIFY â€” token CHANGED between provider and wire!")
        }
        Log.i("TOKEN_LIFECYCLE", "  Compare with CAPTURED/STORED above to verify full chain.")
        Log.i("TOKEN_LIFECYCLE", "â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•")
    }

    // â”€â”€ Event log â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private val eventLog = mutableListOf<String>()
    private val eventLogLock = Any()
    private val maxEventLogSize = 100

    private fun logEvent(event: String) {
        val ts = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
        synchronized(eventLogLock) {
            eventLog.add("[$ts] $event")
            if (eventLog.size > maxEventLogSize) {
                eventLog.removeAt(0)
            }
        }
    }

    // â”€â”€ Pipeline Step Logging â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private fun logPipeline(step: Int, msg: String) {
        Log.d(TAG, "[STEP $step/15] $msg")
        crashReporter.log("PIPELINE_STEP_$step: $msg")
    }

    private fun updateEventLog(eventLogEntry: List<String>) {
        synchronized(eventLogLock) {
            for (e in eventLogEntry) {
                logEvent(e)
            }
        }
    }

    override fun connect(provider: GatewayIdentityProvider) {
        if (!connecting.compareAndSet(false, true)) {
            Log.w(TAG, "connect() called while connection already in progress â€” ignoring duplicate call")
            return
        }
        timelineConnectionId = CONNECTION_ID_COUNTER.incrementAndGet()
        timelineStartMs = System.currentTimeMillis()
        synchronized(eventLogLock) { eventLog.clear() }
        tokenHashLoaded = null
        tokenHashIdentify = null

        val now = timelineMark()
        Log.i("TIMELINE", "")
        Log.i("TIMELINE", "â•”â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•—")
        Log.i("TIMELINE", "â•‘           GATEWAY CONNECTION #$timelineConnectionId                â•‘")
        Log.i("TIMELINE", "â•šâ•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•")
        timeline(now, "OP--", "connect() called", "provider=${provider.name}")

        logPipeline(1, "connect() called provider=${provider.name}")
        logEvent("CONNECT called, provider=${provider.name}")
        disconnectInternal()
        identityProvider = provider
        shouldReconnect = true
        reconnectAttempts = 0
        userId = null
        sessionId = null
        sequenceNumber = null
        updateDiagnostics { copy(currentAuthProvider = provider.name, authMilestone = "Connecting", connectionStartedAt = System.currentTimeMillis()) }
        crashReporter.setCustomKey("gateway_last_action", "connect:${provider.name}")
        connectionJob = scope.launch { connectInternal(provider) }
    }

    private suspend fun connectInternal(provider: GatewayIdentityProvider) {
        connecting.set(true)
        helloReceived = false
        reconnectAttempts++
        logPipeline(2, "connectInternal() attempt=$reconnectAttempts/$maxReconnectAttempts")
        if (reconnectAttempts > maxReconnectAttempts) {
            Log.e(TAG, "Max reconnection attempts reached")
            _state.value = GatewayConnectionState.FatalError("Max reconnection attempts reached")
            shouldReconnect = false
            connecting.set(false)
            return
        }

        isReconnecting.set(false)
        _state.value = GatewayConnectionState.Connecting
        emitEvent(GatewayEvent.Reconnecting(reconnectAttempts, maxReconnectAttempts))

        try {
            val wsUrl = resumeGatewayUrl ?: config.url
            logPipeline(3, "Creating WebSocket to $wsUrl")

            val wsVersion = wsUrl.substringAfter("v=").substringBefore("&")
            val wsEncoding = wsUrl.substringAfter("encoding=").substringBefore("&")
            Log.d(TAG, "[NET] Gateway URL   = $wsUrl")
            Log.d(TAG, "[NET] API version   = v$wsVersion")
            Log.d(TAG, "[NET] Encoding      = $wsEncoding")

            // Log all request headers by building with explicit headers
            val reqHeaders = mapOf(
                "User-Agent" to "Discord/290200 (win32; Windows NT 10.0; en-US; 10.0.22621; c68c818)"
            )
            val requestBuilder = Request.Builder()
                .url(wsUrl)
            for ((k, v) in reqHeaders) {
                requestBuilder.header(k, v)
            }
            val request = requestBuilder.build()
            Log.d(TAG, "[NET] WebSocket upgrade request headers:")
            Log.d(TAG, "[NET]   URL: $wsUrl")
            for (i in 0 until request.headers.size) {
                Log.d(TAG, "[NET]   ${request.headers.name(i)}: ${request.headers.value(i)}")
            }

            val opened = AtomicBoolean(false)
            val latch = java.util.concurrent.CountDownLatch(1)
            val failed = AtomicBoolean(false)

            okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    val now = timelineMark()
                    timeline(now, "OP--", "WebSocket Open", "status=${response.code}")
                    Log.d(TAG, "[NET] WebSocket handshake response: ${response.code} ${response.message}")
                    Log.d(TAG, "[NET] Response headers:")
                    for (i in 0 until response.headers.size) {
                        Log.d(TAG, "[NET]   ${response.headers.name(i)}: ${response.headers.value(i)}")
                    }
                    logEvent("WebSocket OPEN, status=${response.code}")
                    webSocket = ws
                    opened.set(true)
                    latch.countDown()
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    scope.launch { handleFrame(text) }
                }

                override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                    Log.w(TAG, "WebSocket onClosing: code=$code reason=$reason")
                    logEvent("WebSocket CLOSING code=$code reason='$reason'")
                    if (webSocket !== ws) { Log.d(TAG, "Ignoring stale onClosing"); return }
                    ws.close(code, reason)
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    Log.w(TAG, "WebSocket onClosed: code=$code reason=$reason")
                    logEvent("WebSocket CLOSED code=$code reason='$reason'")
                    if (webSocket !== ws) { Log.d(TAG, "Ignoring stale onClosed"); return }
                    webSocket = null
                    handleClose(code, reason)
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket onFailure: ${t::class.simpleName} - ${t.message}")
                    logEvent("WebSocket FAILURE: ${t::class.simpleName} - ${t.message}")
                    if (webSocket !== ws) { Log.d(TAG, "Ignoring stale onFailure"); return }
                    if (failed.compareAndSet(false, true)) {
                        webSocket = null
                        opened.set(false)
                        latch.countDown()
                        handleClose(-1, t.message ?: "Unknown error")
                    }
                }
            })

            val connected = latch.await(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
            if (!connected && !failed.get()) {
                logPipeline(2, "WebSocket connect timeout after ${config.connectTimeoutMs}ms")
                webSocket?.close(1000, "Connect timeout")
                webSocket = null
                _state.value = GatewayConnectionState.Disconnected
                connecting.set(false)
                scheduleReconnect(provider)
            } else if (failed.get()) {
                logPipeline(2, "WebSocket connection failed")
                connecting.set(false)
            } else {
                logPipeline(4, "WebSocket connected successfully, awaiting HELLO...")
                delay(config.helloTimeoutMs)
                if (!helloReceived) {
                    Log.e(TAG, "HELLO timeout: no OP-10 received within ${config.helloTimeoutMs}ms of WebSocket open")
                    logEvent("HELLO TIMEOUT (no OP-10)")
                    webSocket?.close(1000, "HELLO timeout")
                    webSocket = null
                    _state.value = GatewayConnectionState.Disconnected
                    connecting.set(false)
                    scheduleReconnect(provider)
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "connectInternal exception: ${e::class.simpleName} - ${e.message}")
            _state.value = GatewayConnectionState.Disconnected
            scheduleReconnect(provider)
        }
    }

    private suspend fun handleFrame(text: String) {
        try {
            val payload = json.decodeFromString<JsonObject>(text)
            val op = payload["op"]?.jsonPrimitive?.intOrNull
            val seq = payload["s"]?.jsonPrimitive?.intOrNull
            val eventType = payload["t"]?.jsonPrimitive?.contentOrNull

            val now = timelineMark()
            val opName = when (op) {
                0 -> "OP-0 (${eventType ?: "DISPATCH"})"
                1 -> "OP-1 (HEARTBEAT)"
                7 -> "OP-7 (RECONNECT)"
                9 -> "OP-9 (INVALID_SESSION)"
                10 -> "OP-10 (HELLO)"
                11 -> "OP-11 (HEARTBEAT_ACK)"
                else -> "OP-$op"
            }
            timeline(now, "OP-$op", "RECV ${eventType ?: ""}", "seq=$seq")

            Log.d(TAG, "OP-$op received | seq=$seq | event=$eventType | raw=${text.take(120)}")

            if (seq != null) {
                sequenceNumber = seq
                updateDiagnostics { copy(lastSequenceNumber = seq) }
            }

            when (op) {
                0 -> {
                    Log.d(TAG, "OP-0 Dispatch event=$eventType seq=$seq")
                    logEvent("DISPATCH $eventType seq=$seq")
                    if (eventType == "READY") {
                        val d = payload["d"]?.jsonObject
                        userId = d?.get("user")?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                        Log.d(TAG, "READY: user_id=$userId session_id=${d?.get("session_id")?.jsonPrimitive?.contentOrNull?.take(8)}")
                    }
                    handleDispatch(payload)
                }
                1 -> {
                    Log.d(TAG, "OP-1 Heartbeat requested by server")
                    logEvent("HEARTBEAT REQUEST (OP-1)")
                    handleHeartbeatRequest()
                }
                7 -> {
                    logPipeline(14, "OP-7 Reconnect requested by server")
                    logEvent("RECONNECT REQUESTED (OP-7)")
                    handleReconnect()
                }
                9 -> {
                    Log.w(TAG, "OP-9 Invalid session: ${payload["d"]}")
                    logEvent("INVALID SESSION (OP-9) d=${payload["d"]}")
                    handleInvalidSession(payload)
                }
                10 -> {
                    logPipeline(5, "OP-10 HELLO received")
                    logEvent("HELLO RECEIVED (OP-10)")
                    handleHello(payload)
                }
                11 -> {
                    Log.d(TAG, "OP-11 Heartbeat ACK (latency will be computed)")
                    logEvent("HEARTBEAT ACK (OP-11)")
                    handleHeartbeatAck()
                }
                else -> Log.w(TAG, "Unknown OP code: $op payload=${text.take(80)}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleFrame exception: ${e::class.simpleName} - ${e.message}")
            emitEvent(GatewayEvent.Error("Failed to parse frame: ${e.message}"))
        }
    }

    private suspend fun handleHello(payload: JsonObject) {
        helloReceived = true
        var now = timelineMark()
        timeline(now, "OP-10", "HELLO received", "heartbeat_interval payload logged")
        val hello = payload["d"]?.jsonObject ?: return
        Log.d(TAG, "HELLO full payload: ${payload.toString().take(300)}")
        updateDiagnostics { copy(authMilestone = "HELLO received") }
        heartbeatInterval = hello["heartbeat_interval"]?.jsonPrimitive?.longOrNull ?: config.heartbeatIntervalMs
        logPipeline(6, "Heartbeat interval = ${heartbeatInterval}ms")
        startHeartbeat()

        val provider = identityProvider ?: return
        _state.value = GatewayConnectionState.Authenticating

        logPipeline(7, "Requesting identity from provider: ${provider.name}")
        val identity = provider.provideIdentity()
        if (identity == null) {
            Log.e(TAG, "Identity provider returned null (no token)")
            _state.value = GatewayConnectionState.Unauthorized(
                "No Discord token found. Please connect your Discord account first."
            )
            provider.onAuthError("No identity provided")
            heartbeatJob?.cancel()
            connectionJob?.cancel()
            shouldReconnect = false
            return
        }

        tokenHashLoaded = identity.token.tokenHash()
        Log.i("TOKEN_LIFECYCLE", "  LOADED    hash=${tokenHashLoaded}  at UserTokenProvider.provideIdentity")

        val tokenSource = when (provider.name) {
            "UserToken" -> "User session token (from LoginWebView localStorage)"
            "Bot" -> "Bot token (BuildConfig)"
            "OAuth" -> "OAuth access token"
            else -> provider.name
        }
        val maskedPrefix = identity.token.take(8)
        Log.d(TAG, "[AUTH] Token source  = $tokenSource")
        Log.d(TAG, "[AUTH] Token length  = ${identity.token.length} chars")
        Log.d(TAG, "[AUTH] Token prefix  = \"$maskedPrefix...\"")
        Log.d(TAG, "[AUTH] Provider name = ${provider.name}")
        Log.d(TAG, "[AUTH] Token NOT blank? ${identity.token.isNotBlank()}")

        val presencePayload = lastPresenceData?.let { p ->
            val hasAssets = p.largeImage.isNotBlank() || p.smallImage.isNotBlank()
            val hasButtons = p.button1Label.isNotBlank() || p.button2Label.isNotBlank()
            val hasParty = p.partySize != null
            val hasStream = p.type == 1 && p.streamUrl.isValidStreamingUrl()
            buildJsonObject {
                put("since", 0)
                put("activities", buildJsonArray {
                    add(buildJsonObject {
                        put("name", p.name)
                        put("type", p.type)
                        put("details", p.details)
                        put("state", p.state)
                        if (hasAssets || hasButtons || hasParty || hasStream) {
                            put("application_id", Constants.RPC_APPLICATION_ID)
                        }
                        if (p.partySize != null) {
                            put("party", buildJsonObject {
                                put("id", "mypresence")
                                put("size", buildJsonArray {
                                    add(JsonPrimitive(p.partySize))
                                    add(JsonPrimitive(p.partyMax ?: p.partySize))
                                })
                            })
                        }
                        if (p.startTimestamp != null || p.endTimestamp != null) {
                            put("timestamps", buildJsonObject {
                                p.startTimestamp?.let { put("start", it) }
                                p.endTimestamp?.let { put("end", it) }
                            })
                        }
                        val assetsJson = buildJsonObject {
                            if (p.largeImage.isNotEmpty()) put("large_image", p.largeImage)
                            if (p.largeText.isNotEmpty()) put("large_text", p.largeText)
                            if (p.largeUrl.isValidRpcUrl()) put("large_url", p.largeUrl.trim())
                            if (p.smallImage.isNotEmpty()) put("small_image", p.smallImage)
                            if (p.smallText.isNotEmpty()) put("small_text", p.smallText)
                        }
                        if (assetsJson.isNotEmpty()) put("assets", assetsJson)
                    })
                })
                put("status", p.status)
                put("afk", false)
            }
        } ?: buildJsonObject {
            put("since", 0)
            put("activities", buildJsonArray {})
            put("status", "online")
            put("afk", false)
        }

        val identifyPayload = buildJsonObject {
            put("op", 2)
            put("d", buildJsonObject {
                put("token", identity.token)
                put("properties", buildJsonObject {
                    identity.properties.forEach { (k, v) -> put(k, v) }
                })
                identity.intents?.let { put("intents", it) }
                identity.capabilities?.let { put("capabilities", it) }
                identity.compress?.let { put("compress", it) }
                identity.largeThreshold?.let { put("largeThreshold", it) }
                put("presence", presencePayload)
            })
        }
        val payloadString = identifyPayload.toString()
        val masked = payloadString.replace(identity.token, identity.token.take(8) + "...")
        tokenHashIdentify = identity.token.tokenHash()
        Log.i("TOKEN_LIFECYCLE", "  IDENTIFY  hash=${tokenHashIdentify}  at DiscordGatewayEngine.handleHello (WebSocket.send)")
        logTokenLifecycle()
        logPipeline(8, "OP-2 IDENTIFY sent: $masked")
        logEvent("IDENTIFY SENT (OP-2) token_hash=${identity.token.tokenHash()} provider=${provider.name}")
        now = timelineMark()
        timeline(now, "OP-2", "IDENTIFY sent", "token_hash=${tokenHashIdentify}")
        Log.d(TAG, "[AUTH] â”€â”€â”€â”€â”€â”€â”€â”€ IDENTIFY payload field analysis â”€â”€â”€â”€â”€â”€â”€â”€")
        Log.d(TAG, "[AUTH] op              = 2 (IDENTIFY)")
        Log.d(TAG, "[AUTH] d.token         = \"${identity.token.take(8)}...\" (length=${identity.token.length})")
        Log.d(TAG, "[AUTH] d.capabilities  = ${identity.capabilities}")
        Log.d(TAG, "[AUTH] d.compress      = ${identity.compress}")
        Log.d(TAG, "[AUTH] d.largeThreshold = ${identity.largeThreshold}")
        Log.d(TAG, "[AUTH] d.properties.os      = \"${identity.properties["os"]}\"")
        Log.d(TAG, "[AUTH] d.properties.browser = \"${identity.properties["browser"]}\"")
        Log.d(TAG, "[AUTH] d.properties.device  = \"${identity.properties["device"]}\"")
        Log.d(TAG, "[AUTH] d.intents       = ${identity.intents} (should be null for user tokens)")
        Log.d(TAG, "[AUTH] d.presence      = sent with ${presencePayload.toString().take(100)}")
        Log.d(TAG, "[AUTH] d.client_state  = NOT sent")
        Log.d(TAG, "[AUTH] â”€â”€â”€â”€â”€â”€â”€â”€ Full IDENTIFY payload string â”€â”€â”€â”€â”€â”€â”€â”€")
        Log.d(TAG, "[AUTH] ${masked}")
        Log.d(TAG, "[AUTH] â”€â”€â”€â”€â”€â”€â”€â”€ End IDENTIFY payload â”€â”€â”€â”€â”€â”€â”€â”€")
        Log.d(TAG, "[AUTH] â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€")
        updateDiagnostics { copy(authMilestone = "IDENTIFY sent") }
        Log.d(TAG, "[AUTH] Sending IDENTIFY via WebSocket.send()...")
        Log.d(TAG, "[AUTH] Payload bytes = ${payloadString.toByteArray().size} UTF-8 bytes")
        Log.d(TAG, "[AUTH] Payload first 120 = ${payloadString.take(120)}")
        val sendResult = webSocket?.send(payloadString)
        Log.d(TAG, "[AUTH] WebSocket.send() returned: $sendResult")
        if (sendResult == true) {
            Log.d(TAG, "[AUTH] âœ“ IDENTIFY queued for transmission")
        } else {
            Log.e(TAG, "[AUTH] âœ— IDENTIFY send FAILED or webSocket was null!")
            updateDiagnostics { copy(authMilestone = "IDENTIFY send failed", state = GatewayConnectionState.FatalError("IDENTIFY send failed")) }
            _state.value = GatewayConnectionState.FatalError("IDENTIFY send failed â€” WebSocket not available")
            connecting.set(false)
            return
        }
        connecting.set(false)
    }

    private suspend fun handleDispatch(payload: JsonObject) {
        val t = payload["t"]?.jsonPrimitive?.contentOrNull ?: return
        val dc = dispatchCounter.incrementAndGet()
        emitEvent(GatewayEvent.Dispatch(t))
        Log.d(TAG, "[DISPATCH #$dc] $t received")

        when (t) {
            "READY" -> {
                reconnectAttempts = 0
                resumePending = false
                val d = payload["d"]?.jsonObject
                val now = timelineMark()
                sessionId = d?.get("session_id")?.jsonPrimitive?.content
                userId = d?.get("user")?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
                val fullSid = sessionId ?: ""
                timeline(now, "OP-0", "READY received", "user_id=$userId session=${fullSid.take(8)}")
                Log.d(TAG, "[DISPATCH #$dc] READY full JSON: ${payload.toString().take(500)}")
                logEvent("READY RECEIVED user_id=$userId")
                resumeGatewayUrl = d?.get("resume_gateway_url")?.jsonPrimitive?.content
                    ?.let { "$it/?v=10&encoding=json" }
                val seq = sequenceNumber
                updateDiagnostics { copy(
                    sessionId = sessionId,
                    readyReceived = true,
                    lastGatewayDispatch = "READY",
                    authMilestone = "READY received"
                ) }
                Log.d(TAG, "[STATE] Connected ONLY set here â€” after READY dispatch")
                logPipeline(9, "READY received! user_id=$userId session_id=${fullSid.take(8)}... seq=$seq")
                crashReporter.setCustomKey("gateway_session", fullSid)
                crashReporter.setCustomKey("gateway_user_id", userId ?: "")
                _state.value = GatewayConnectionState.Connected(
                    sessionId = fullSid,
                    sequence = seq
                )
                emitEvent(GatewayEvent.Connected)
            }
            "RESUMED" -> {
                reconnectAttempts = 0
                resumePending = false
                Log.d(TAG, "[STATE] Connected set from RESUMED dispatch")
                logEvent("RESUMED RECEIVED")
                logPipeline(9, "RESUMED received")
                _state.value = GatewayConnectionState.Connected(
                    sessionId = sessionId ?: "",
                    sequence = sequenceNumber
                )
                emitEvent(GatewayEvent.ResumeSucceeded)
            }
            "PRESENCE_UPDATE" -> {
                val d = payload["d"]?.jsonObject
                val activitiesVal = d?.get("activities")
                val activitiesArr = activitiesVal?.jsonArray
                val userName = d?.get("user")?.jsonObject?.get("username")?.jsonPrimitive?.contentOrNull
                val accepted = activitiesArr != null && activitiesArr.isNotEmpty()
                val now = timelineMark()
                timeline(now, "OP-0", "PRESENCE_UPDATE", "accepted=$accepted user=$userName")
                if (accepted) {
                    for (i in activitiesArr.indices) {
                        val obj = activitiesArr[i].jsonObject
                        timeline(now, "OP-0", "  Activity[$i]", "name=${obj["name"]} details=${obj["details"]} state=${obj["state"]}")
                    }
                }
                Log.d(TAG, "[OP4_VERIFY] Discord accepted our activity: $accepted")
                if (accepted) {
                    for (i in activitiesArr.indices) {
                        val obj = activitiesArr[i].jsonObject
                        val assetsObj = obj["assets"]?.jsonObject
                        val largeImage = assetsObj?.get("large_image")?.jsonPrimitive?.contentOrNull
                        val largeText = assetsObj?.get("large_text")?.jsonPrimitive?.contentOrNull
                        Log.i("OP4_VERIFY", "Activity[$i]: name=${obj["name"]} " +
                            "details=${obj["details"]} state=${obj["state"]} " +
                            "large_image=$largeImage large_text=$largeText " +
                            "has_assets=${assetsObj != null}")
                    }
                    Log.d("OP4_VERIFY", "Full PRESENCE_UPDATE activities JSON: ${payload.toString().take(500)}")
                } else {
                    Log.w("OP4_VERIFY", "PRESENCE_UPDATE received with empty activities â€” Discord may have rejected our assets")
                }
                updateDiagnostics { copy(
                    lastOp3Accepted = accepted,
                    lastGatewayDispatch = "PRESENCE_UPDATE",
                    presenceUpdateCount = presenceUpdateCount + 1
                ) }
            }
        }
    }

    private suspend fun handleHeartbeatRequest() {
        val now = timelineMark()
        timeline(now, "OP-1", "Heartbeat requested by server", "responding immediately")
        sendHeartbeat()
    }

    private suspend fun handleReconnect() {
        logPipeline(14, "Server requested reconnect (OP 7)")
        logEvent("HANDLE_RECONNECT (OP-7)")
        emitEvent(GatewayEvent.Error("Server requested reconnect (OP 7)"))
        _state.value = GatewayConnectionState.Reconnecting
        val provider = identityProvider ?: return
        disconnectInternal()
        shouldReconnect = true // disconnectInternal sets it to false; must restore
        delay(1500)
        connectionJob = scope.launch { connectInternal(provider) }
    }

    private suspend fun handleInvalidSession(payload: JsonObject) {
        val canResume = payload["d"]?.jsonPrimitive?.content == "true"
        logPipeline(13, "OP-9 Invalid session canResume=$canResume")
        logEvent("INVALID_SESSION canResume=$canResume")
        _state.value = GatewayConnectionState.InvalidSession(canResume)
        emitEvent(GatewayEvent.Error("Invalid session (OP 9), canResume=$canResume"))
        heartbeatJob?.cancel()
        identityProvider?.onSessionInvalidated()
        reconnectAttempts = 0
        if (canResume && sessionId != null) {
            Log.d(TAG, "Attempting resume session")
            resumeSession()
        } else {
            Log.d(TAG, "Cannot resume, fresh connect")
            sessionId = null
            sequenceNumber = null
            userId = null
            updateDiagnostics { copy(sessionId = null, lastSequenceNumber = null) }
            val provider = identityProvider ?: return
            disconnectInternal()
            delay(1500)
            connectionJob = scope.launch { connectInternal(provider) }
        }
    }

    private suspend fun resumeSession() {
        logPipeline(13, "Resuming session_id=${sessionId?.take(8)}... seq=$sequenceNumber")
        _state.value = GatewayConnectionState.Resuming
        val resumePayload = buildJsonObject {
            put("op", 6)
            put("d", buildJsonObject {
                put("token", identityProvider?.provideIdentity()?.token ?: "")
                put("session_id", sessionId ?: "")
                put("seq", sequenceNumber ?: 0)
            })
        }
        resumePending = true
        webSocket?.send(resumePayload.toString())
        delay(config.resumeTimeoutMs)
        if (resumePending) {
            Log.w(TAG, "Resume timed out after ${config.resumeTimeoutMs}ms â€” falling back to fresh connect")
            logEvent("RESUME TIMEOUT â€” fresh connect")
            resumePending = false
            shouldReconnect = true
            val provider = identityProvider ?: return
            disconnectInternal()
            delay(500)
            connectionJob = scope.launch { connectInternal(provider) }
        }
    }

    private suspend fun handleHeartbeatAck() {
        val now = System.currentTimeMillis()
        val sent = lastHeartbeatSent.get()
        if (sent > 0) {
            val latency = now - sent
            lastHeartbeatAck.set(now)
            missedHeartbeats.set(0)
            val ackCount = _diagnostics.value.heartbeatAckCount + 1
            updateDiagnostics { copy(heartbeatPing = latency, heartbeatAckCount = ackCount, lastHeartbeatAckMs = now) }
            emitEvent(GatewayEvent.HeartbeatAcknowledged(latency))
            val tMark = timelineMark()
            timeline(tMark, "OP-11", "Heartbeat ACK", "latency=${latency}ms")
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        missedHeartbeats.set(0)
        val now = timelineMark()
        timeline(now, "OP--", "Heartbeat started", "interval=${heartbeatInterval}ms")
        logPipeline(6, "Starting heartbeat loop interval=${heartbeatInterval}ms")
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(heartbeatInterval)
                sendHeartbeat()
                checkHeartbeatTimeout()
            }
        }
    }

    private suspend fun sendHeartbeat() {
        val seq = sequenceNumber
        lastHeartbeatSent.set(System.currentTimeMillis())
        val payload = buildJsonObject {
            put("op", 1)
            put("d", seq?.let { JsonPrimitive(it) } ?: JsonNull)
        }
        val payloadString = payload.toString()
        val now = timelineMark()
        timeline(now, "OP-1", "Heartbeat sent", "seq=$seq")
        webSocket?.send(payloadString)
        heartbeatSentCount++
        updateDiagnostics { copy(heartbeatsSent = heartbeatSentCount) }
        emitEvent(GatewayEvent.HeartbeatSent(seq))
    }

    private suspend fun checkHeartbeatTimeout() {
        val ackTime = lastHeartbeatAck.get()
        if (ackTime == 0L) {
            lastHeartbeatAck.set(System.currentTimeMillis())
            return
        }
        val timeSinceLastAck = System.currentTimeMillis() - ackTime
        if (timeSinceLastAck > heartbeatInterval * config.heartbeatTimeoutMultiplier) {
            val missed = missedHeartbeats.incrementAndGet()
            Log.w(TAG, "Heartbeat timeout: ${timeSinceLastAck}ms since last ACK (missed=$missed/${config.maxHeartbeatMisses})")
            emitEvent(GatewayEvent.HeartbeatLost(missed))
            if (missed >= config.maxHeartbeatMisses) {
                Log.e(TAG, "Max heartbeat misses reached, reconnecting")
                _state.value = GatewayConnectionState.HeartbeatLost
                val provider = identityProvider ?: return
                disconnectInternal()
                connectionJob = scope.launch { connectInternal(provider) }
            }
        }
    }

    companion object {
        private val CLOSE_CODE_DESCRIPTIONS = mapOf(
            1000 to "Normal closure (client requested disconnect)",
            1001 to "Going away (app closing or server shutdown)",
            4000 to "Unknown error (generic Discord close)",
            4001 to "Unknown opcode (client sent an unrecognized OP code)",
            4002 to "Invalid session (session no longer valid â€” must re-identify)",
            4003 to "Bot required (this endpoint requires a bot token)",
            4004 to "Authentication failed (token is invalid, expired, or revoked)",
            4005 to "Already authenticated (IDENTIFY sent twice on same connection)",
            4006 to "Invalid sequence (session sequence number is wrong)",
            4007 to "Invalid sequence (resume sequence number is wrong â€” re-identify required)",
            4008 to "Rate limited (sending too many payloads â€” slow down)",
            4009 to "Session timeout (session expired â€” must reconnect)",
            4010 to "Invalid shard (shard ID or count is invalid)",
            4011 to "Sharding required (this connection requires sharding)",
            4012 to "Invalid API version (gateway URL version is wrong)",
            4013 to "Invalid intents (you specified invalid intent bits)",
            4014 to "Disallowed intents (you requested intents you don't have)"
        )

        private fun describeCloseCode(code: Int): String =
            CLOSE_CODE_DESCRIPTIONS[code] ?: when {
                code <= 0 -> "Abnormal closure (connection lost or network error)"
                code in 1000..1999 -> "WebSocket protocol close (code=$code)"
                code in 4000..4999 -> "Discord gateway close (code=$code)"
                else -> "Unknown close code $code"
            }
    }

    private var closedByUser = false

    private fun handleClose(code: Int, reason: String) {
        val now = timelineMark()
        val closeDesc = describeCloseCode(code)
        timeline(now, "CLOSE", "Connection closed", "code=$code ($closeDesc) reason='$reason'")
        val provider = identityProvider
        closedByUser = false

        updateDiagnostics { copy(
            lastCloseCode = code,
            lastCloseReason = "$closeDesc â€” $reason",
            authMilestone = when {
                code >= 4000 -> "${authMilestone ?: "?"} â†’ CLOSE $code"
                else -> "${authMilestone ?: "?"} â†’ DISCONNECTED"
            }
        ) }
        logEvent("CLOSE code=$code reason='$reason' description=$closeDesc")

        Log.w(TAG, "CLOSE $code ($closeDesc): $reason")
        Log.w(TAG, "CLOSE reason bytes: ${reason.toByteArray(Charsets.UTF_8).joinToString(" ") { "%02x".format(it.toInt() and 0xFF) }}")
        logPipeline(15, "Close code=$code ($closeDesc) reason=$reason willReconnect=$shouldReconnect")
        crashReporter.log("Gateway close code=$code reason=$reason desc=$closeDesc willReconnect=$shouldReconnect")
        crashReporter.setCustomKey("gateway_last_close_code", code)
        crashReporter.setCustomKey("gateway_last_close_reason", "$closeDesc â€” $reason")

        updateDiagnostics { copy(eventLog = synchronized(eventLogLock) { eventLog.toList() }) }

        when {
            code == 4004 -> {
                shouldReconnect = false
                Log.e(TAG, "[AUTH] ==============================")
                Log.e(TAG, "[AUTH] CLOSE 4004: Authentication FAILED â€” token invalid/expired/revoked")
                Log.e(TAG, "[AUTH] Reason: '$reason'")
                Log.e(TAG, "[AUTH] Provider was: ${provider?.name ?: "null"}")
                Log.e(TAG, "[AUTH] Session ID: ${sessionId?.take(8) ?: "none"}")
                Log.e(TAG, "[AUTH] ==============================")
                heartbeatJob?.cancel()
                _state.value = GatewayConnectionState.Unauthorized("Authentication failed (4004): $reason")
                scope.launch { provider?.onSessionInvalidated() }
            }
            code == 4010 || code == 4011 || code == 4012 || code == 4013 || code == 4014 -> {
                Log.e(TAG, "CLOSE $code ($closeDesc): $reason")
                _state.value = GatewayConnectionState.FatalError("Fatal close $code: $closeDesc â€” $reason")
                shouldReconnect = false
            }
            code == 4002 || (code == 4007 && reason.contains("Invalid sequence")) -> {
                Log.w(TAG, "CLOSE $code ($closeDesc): $reason â€” initiating fresh re-identify")
                _state.value = GatewayConnectionState.InvalidSession(false)
                heartbeatJob?.cancel()
                sessionId = null
                sequenceNumber = null
                userId = null
                reconnectAttempts = 0
                scope.launch {
                    provider?.onSessionInvalidated()
                    val p = identityProvider
                    if (p != null && !isReconnecting.getAndSet(true)) {
                        disconnectInternal()
                        delay(1500)
                        connectionJob = scope.launch { connectInternal(p) }
                    }
                }
            }
            code == 4000 || code == 4001 || code == 4009 || code <= 0 -> {
                Log.w(TAG, "CLOSE $code ($closeDesc): $reason â€” reconnecting")
                _state.value = GatewayConnectionState.Reconnecting
                if (shouldReconnect && provider != null && isReconnecting.compareAndSet(false, true)) {
                    scope.launch { scheduleReconnect(provider) }
                }
            }
            else -> {
                Log.d(TAG, "CLOSE $code ($closeDesc): $reason")
                _state.value = GatewayConnectionState.Disconnected
                if (shouldReconnect && provider != null && isReconnecting.compareAndSet(false, true)) {
                    scope.launch { scheduleReconnect(provider) }
                }
            }
        }
    }

    private suspend fun scheduleReconnect(provider: GatewayIdentityProvider) {
        val delayMs = config.reconnectDelayMs * (1 shl (reconnectAttempts.coerceAtMost(5) - 1))
        val actualDelay = delayMs.coerceAtMost(30_000L)
        logPipeline(14, "scheduleReconnect attempt=$reconnectAttempts delay=${actualDelay}ms")
        emitEvent(GatewayEvent.Reconnecting(reconnectAttempts, maxReconnectAttempts))
        delay(actualDelay)
        if (shouldReconnect) {
            connectionJob?.cancel()
            isReconnecting.set(false)
            connectionJob = scope.launch { connectInternal(provider) }
        }
    }

    override suspend fun updatePresence(presence: PresenceData) {
        val now = timelineMark()
        val seqNum = op3Sequence.incrementAndGet()

        val elapsed = if (lastOp3SentMs > 0) now - lastOp3SentMs else -1L
        lastOp3SentMs = now

        logPipeline(10, "[OP-3 #$seqNum] updatePresence() called name=${presence.name} type=${presence.type} status=${presence.status}")
        Log.d(TAG, "[OP-3 #$seqNum] â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€")
        Log.d(TAG, "[OP-3 #$seqNum] Time since last OP-3: ${elapsed}ms")
        Log.d(TAG, "[OP-3 #$seqNum] Gateway state: ${_state.value}")
        Log.d(TAG, "[OP-3 #$seqNum] Session ID: ${sessionId?.take(8)}... User ID: $userId")
        Log.d(TAG, "[OP-3 #$seqNum] RPC_APPLICATION_ID: ${Constants.RPC_APPLICATION_ID}")
        Log.d(TAG, "[OP-3 #$seqNum] WebSocket null? ${webSocket == null}")

        // â”€â”€ Step 5: log current activity object BEFORE serialization â”€â”€
        Log.d(TAG, "[OP-3 #$seqNum] Activity object before serialization:")
        Log.d(TAG, "[OP-3 #$seqNum]   name       = \"${presence.name}\"")
        Log.d(TAG, "[OP-3 #$seqNum]   type       = ${presence.type} (${activityTypeLabel(presence.type)})")
        Log.d(TAG, "[OP-3 #$seqNum]   details    = \"${presence.details}\"")
        Log.d(TAG, "[OP-3 #$seqNum]   state      = \"${presence.state}\"")
        Log.d(TAG, "[OP-3 #$seqNum]   status     = \"${presence.status}\"")
        Log.d(TAG, "[OP-3 #$seqNum]   timestamp  = ${presence.startTimestamp}")
        Log.d(TAG, "[OP-3 #$seqNum]   largeImage = \"${presence.largeImage}\"")
        Log.d(TAG, "[OP-3 #$seqNum]   largeText  = \"${presence.largeText}\"")
        Log.d(TAG, "[OP-3 #$seqNum]   largeUrl   = \"${presence.largeUrl}\"")
        Log.d(TAG, "[OP-3 #$seqNum]   smallImage = \"${presence.smallImage}\"")
        Log.d(TAG, "[OP-3 #$seqNum]   smallText  = \"${presence.smallText}\"")

        if (webSocket == null) {
            Log.w(TAG, "[OP-3 #$seqNum] BLOCKED: webSocket is null (not connected) â€” presence NOT sent")
            return
        }

        val resolvedLargeImage = resolveActivityImage(presence.largeImage)
        val resolvedSmallImage = resolveActivityImage(presence.smallImage)
        val buttonPairs = buildList {
            if (presence.button1Label.isNotBlank() && presence.button1Url.isValidRpcUrl()) {
                add(presence.button1Label.rpcLabel() to presence.button1Url.trim())
            }
            if (presence.button2Label.isNotBlank() && presence.button2Url.isValidRpcUrl()) {
                add(presence.button2Label.rpcLabel() to presence.button2Url.trim())
            }
        }

        // â”€â”€ Build the activity JSON â”€â”€
        val hasValidParty = presence.partySize != null &&
            presence.partyMax != null &&
            presence.partySize > 0 &&
            presence.partyMax >= presence.partySize
        val hasStreamingUrl = presence.type == 1 && presence.streamUrl.isValidStreamingUrl()
        val shouldAttachApplicationId =
            resolvedLargeImage != null ||
                resolvedSmallImage != null ||
                buttonPairs.isNotEmpty() ||
                hasValidParty ||
                hasStreamingUrl

        val activity = buildJsonObject {
            put("name", presence.name.rpcName())
            put("type", presence.type)
            presence.details.rpcText()?.let { put("details", it) }
            presence.state.rpcText()?.let { put("state", it) }
            if (shouldAttachApplicationId && Constants.RPC_APPLICATION_ID.isNotBlank()) {
                put("application_id", Constants.RPC_APPLICATION_ID)
            }
            put("instance", true)
            presence.platform.rpcText()?.let { put("platform", it.lowercase()) }
            if (hasStreamingUrl) {
                put("url", presence.streamUrl.trim())
            }
            if (buttonPairs.isNotEmpty()) {
                put("buttons", buildJsonArray {
                    buttonPairs.forEach { add(JsonPrimitive(it.first)) }
                })
                put("metadata", buildJsonObject {
                    put("button_urls", buildJsonArray {
                        buttonPairs.forEach { add(JsonPrimitive(it.second)) }
                    })
                })
            }
            if (hasValidParty) {
                put("party", buildJsonObject {
                    put("id", "mypresence-${userId ?: "session"}")
                    put("size", buildJsonArray {
                        add(JsonPrimitive(presence.partySize))
                        add(JsonPrimitive(presence.partyMax))
                    })
                })
            }
            if (presence.startTimestamp != null || presence.endTimestamp != null) {
                put("timestamps", buildJsonObject {
                    presence.startTimestamp?.let { put("start", it) }
                    presence.endTimestamp?.let { put("end", it) }
                })
            }
            val assetsJson = buildJsonObject {
                resolvedLargeImage?.let { put("large_image", it) }
                presence.largeText.rpcText()?.let { put("large_text", it) }
                if (resolvedLargeImage != null && presence.largeUrl.isValidRpcUrl()) {
                    put("large_url", presence.largeUrl.trim())
                }
                resolvedSmallImage?.let { put("small_image", it) }
                presence.smallText.rpcText()?.let { put("small_text", it) }
            }
            if (assetsJson.isNotEmpty()) put("assets", assetsJson)
        }

        val activities = buildJsonArray { add(activity) }

        val payload = buildJsonObject {
            put("op", 3)
            put("d", buildJsonObject {
                put("since", 0)
                put("activities", activities)
                put("status", presence.status)
                put("afk", false)
            })
        }

        // â”€â”€ Step 6: log the serialized JSON â”€â”€
        val payloadString = payload.toString()
        val assetsJson = payload["d"]?.jsonObject?.get("activities")?.jsonArray?.firstOrNull()?.jsonObject?.get("assets")?.jsonObject
        val op3LargeImage = assetsJson?.get("large_image")?.jsonPrimitive?.contentOrNull
        val op3LargeText = assetsJson?.get("large_text")?.jsonPrimitive?.contentOrNull
        val op3SmallImage = assetsJson?.get("small_image")?.jsonPrimitive?.contentOrNull
        val op3SmallText = assetsJson?.get("small_text")?.jsonPrimitive?.contentOrNull
        updateDiagnostics { copy(currentPresencePayload = payloadString) }
        Log.d(TAG, "[OP-3 #$seqNum] Serialized JSON (${payloadString.length} chars):")
        Log.d(TAG, "[OP-3 #$seqNum]   $payloadString")
        Log.d(TAG, "[OP-3 #$seqNum]   contains 'application_id' = ${payloadString.contains("application_id")} (value=${Constants.RPC_APPLICATION_ID})")
        Log.d(TAG, "[OP-3 #$seqNum]   contains 'assets'         = ${payloadString.contains("assets")}")
        Log.d(TAG, "[OP-3 #$seqNum]   contains 'timestamps'     = ${payloadString.contains("timestamps")}")
        Log.d(TAG, "[OP-3 #$seqNum]   contains 'party'          = ${payloadString.contains("party")}")
        Log.d(TAG, "[OP-3 #$seqNum]   contains 'secrets'        = ${payloadString.contains("secrets")}")
        Log.d(TAG, "[OP-3 #$seqNum]   Display = ${activityTypeLabel(presence.type)} ${presence.name} | ${presence.details} | ${presence.state}")
        Log.i("ASSET_OP3", "OP3_ASSETS: large_image=\"$op3LargeImage\" large_text=\"$op3LargeText\" " +
            "small_image=\"$op3SmallImage\" small_text=\"$op3SmallText\" | ${presence.name}")

        // â”€â”€ Step 7: send + log result â”€â”€
        updateDiagnostics { copy(
            lastOp3Timestamp = now,
            lastActivityJson = payloadString,
            lastOp3Accepted = false
        ) }
        val sendResult = webSocket?.send(payloadString)
        if (sendResult == true) {
            lastPresenceData = presence
        }
        val tNow = timelineMark()
        timeline(tNow, "OP-3", "Presence Update sent", "name=${presence.name} details=${presence.details}")
        Log.d(TAG, "[OP-3 #$seqNum] WebSocket.send() returned: $sendResult")
        if (sendResult == true) {
            Log.d(TAG, "[OP-3 #$seqNum] âœ“ Presence Update SENT SUCCESSFULLY")
        } else {
            Log.w(TAG, "[OP-3 #$seqNum] âœ— Presence Update SEND FAILED or returned null")
        }
        Log.d(TAG, "[OP-3 #$seqNum] â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€")
        Log.d(TAG, "[OP3_VERIFY] Awaiting PRESENCE_UPDATE dispatch to confirm Discord acceptance...")
    }

    private fun activityTypeLabel(type: Int): String = when (type) {
        0 -> "Playing"
        1 -> "Streaming"
        2 -> "Listening to"
        3 -> "Watching"
        4 -> "Custom"
        5 -> "Competing in"
        else -> "Activity($type)"
    }

    private suspend fun resolveActivityImage(raw: String): String? {
        val value = raw.trim()
        if (value.isBlank()) return null
        if (value.startsWith("mp:")) return value
        if (!value.startsWith("http://", ignoreCase = true) && !value.startsWith("https://", ignoreCase = true)) {
            return if (value.startsWith("attachments/")) "mp:$value" else value
        }

        val token = identityProvider?.provideIdentity()?.token.orEmpty()
        return discordApi.resolveExternalAsset(
            userToken = token,
            applicationId = Constants.RPC_APPLICATION_ID,
            imageUrl = value
        )
    }

    private fun String.rpcName(): String =
        trim().ifBlank { "My Presence" }.take(128)

    private fun String.rpcLabel(): String =
        trim().take(32)

    private fun String.rpcText(): String? =
        trim().take(128).takeIf { it.isNotBlank() }

    private fun String.isValidRpcUrl(): Boolean =
        runCatching {
            val uri = URI(trim())
            uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
        }.getOrDefault(false)

    private fun String.isValidStreamingUrl(): Boolean {
        if (!isValidRpcUrl()) return false
        val host = runCatching { URI(trim()).host.lowercase() }.getOrNull() ?: return false
        return host.endsWith("twitch.tv") ||
            host.endsWith("youtube.com") ||
            host.endsWith("youtu.be")
    }

    private fun disconnectInternal() {
        shouldReconnect = false
        helloReceived = false
        resumePending = false
        connecting.set(false)
        heartbeatJob?.cancel()
        connectionJob?.cancel()
        webSocket?.close(1000, "Disconnected")
        webSocket = null
    }

    override fun setMaxReconnectAttempts(max: Int) {
        maxReconnectAttempts = max
    }

    override fun disconnect() {
        Log.d(TAG, "disconnect() called")
        disconnectInternal()
        sessionId = null
        sequenceNumber = null
        userId = null
        identityProvider = null
        _state.value = GatewayConnectionState.Disconnected
        updateDiagnostics { GatewayDiagnostics(state = GatewayConnectionState.Disconnected) }
        emitEvent(GatewayEvent.Disconnected)
    }

    override suspend fun clearPresence() {
        Log.i(TAG, "clearPresence() â€” sending OP-3 with empty activities to clear Discord presence")
        if (webSocket == null) {
            Log.w(TAG, "clearPresence: webSocket is null, cannot clear")
            return
        }
        val payload = buildJsonObject {
            put("op", 3)
            put("d", buildJsonObject {
                put("since", 0)
                put("activities", buildJsonArray {})
                put("status", "online")
                put("afk", false)
            })
        }
        val payloadString = payload.toString()
        Log.d(TAG, "CLEAR_PRESENCE payload: $payloadString")
        val sendResult = webSocket?.send(payloadString)
        Log.d(TAG, "CLEAR_PRESENCE send result: $sendResult")
    }

    override fun destroy() {
        Log.d(TAG, "destroy() called")
        disconnect()
        scope.cancel()
    }

    private fun updateDiagnostics(transform: GatewayDiagnostics.() -> GatewayDiagnostics) {
        val updated = _diagnostics.value.transform()
        _diagnostics.value = updated.copy(
            state = _state.value,
            stateLabel = _state.value.label,
            activeWebSocketCount = if (webSocket != null) 1 else 0,
            isCoroutineScopeActive = scope.isActive,
            lastHeartbeatAckElapsed = if (lastHeartbeatAck.get() > 0) System.currentTimeMillis() - lastHeartbeatAck.get() else null
        )
    }

    private fun emitEvent(event: GatewayEvent) {
        _events.tryEmit(event)
    }
}
