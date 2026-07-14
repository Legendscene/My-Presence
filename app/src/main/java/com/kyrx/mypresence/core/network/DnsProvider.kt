package com.kyrx.mypresence.core.network

import okhttp3.Dns
import org.json.JSONObject
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

@Singleton
class DnsProvider @Inject constructor() : Dns {

    private val cache = mutableMapOf<String, Pair<Long, List<InetAddress>>>()
    private val hardFailCache = mutableMapOf<String, Long>()
    private val cacheTtlMs = 120_000L
    private val dnsRetryDelayMs = 10_000L

    private val hardcodedFallback = mapOf(
        "gateway.discord.gg" to listOf("162.159.128.233", "162.159.130.233")
    )

    override fun lookup(hostname: String): List<InetAddress> {
        val now = System.currentTimeMillis()
        cache[hostname]?.let { (time, ips) ->
            if (now - time < cacheTtlMs) return ips
        }
        hardFailCache[hostname]?.let { failTime ->
            if (now - failTime < dnsRetryDelayMs) {
                hardcodedFallback[hostname]?.let { fallbackIps ->
                    val parsed = fallbackIps.map { InetAddress.getByName(it) }
                    return parsed
                }
            }
        }
        val ips = try {
            Dns.SYSTEM.lookup(hostname)
        } catch (_: Exception) {
            try { dohLookup(hostname) } catch (_: Exception) { null }
        }
        if (ips != null && ips.isNotEmpty()) {
            cache[hostname] = now to ips
            hardFailCache.remove(hostname)
            return ips
        }
        hardFailCache[hostname] = now
        hardcodedFallback[hostname]?.let { fallbackIps ->
            val parsed = fallbackIps.map { InetAddress.getByName(it) }
            cache[hostname] = now to parsed
            return parsed
        }
        throw UnknownHostException("DNS resolution failed for $hostname")
    }

    private fun dohLookup(hostname: String): List<InetAddress> {
        val urls = listOf(
            "https://dns.google/dns-query" to "dns.google",
            "https://cloudflare-dns.com/dns-query" to "cloudflare-dns.com"
        )
        for ((urlStr, sni) in urls) {
            try {
                val url = URL("$urlStr?name=$hostname&type=A")
                val conn = url.openConnection() as HttpsURLConnection
                conn.setRequestProperty("Accept", "application/dns-json")
                conn.setRequestProperty("Host", sni)
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val text = conn.inputStream.bufferedReader().readText()
                val answer = JSONObject(text).optJSONArray("Answer")
                if (answer != null) {
                    val ips = mutableListOf<InetAddress>()
                    for (i in 0 until answer.length()) {
                        val r = answer.optJSONObject(i) ?: continue
                        if (r.optInt("type") == 1) {
                            val s = r.optString("data")
                            if (s.isNotBlank()) ips.add(InetAddress.getByName(s))
                        }
                    }
                    if (ips.isNotEmpty()) return ips
                }
            } catch (_: Exception) { }
        }
        throw UnknownHostException("DoH failed for $hostname")
    }
}
