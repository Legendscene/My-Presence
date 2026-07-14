package com.kyrx.mypresence.core.gateway

data class GatewayConfig(
    val url: String = "wss://gateway.discord.gg/?v=10&encoding=json",
    val connectTimeoutMs: Long = 25_000L,
    val helloTimeoutMs: Long = 15_000L,
    val heartbeatIntervalMs: Long = 41_250L,
    val maxReconnectAttempts: Int = 5,
    val reconnectDelayMs: Long = 5_000L,
    val heartbeatTimeoutMultiplier: Int = 3,
    val maxHeartbeatMisses: Int = 3,
    val resumeTimeoutMs: Long = 10_000L
)
