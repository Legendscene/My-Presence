package com.kyrx.mypresence.core.network

import okhttp3.Dns
import org.json.JSONObject
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

@Singleton
class DnsProvider @Inject constructor() : Dns {

    private val cache = mutableMapOf<String, Pair<Long, List<InetAddress>>>()
    private val cacheTtlMs = 300_000L

    override fun lookup(hostname: String): List<InetAddress> {
        val now = System.currentTimeMillis()
        cache[hostname]?.let { (time, ips) ->
            if (now - time < cacheTtlMs) return ips
        }
        val ips = try {
            Dns.SYSTEM.lookup(hostname)
        } catch (_: Exception) {
            dohLookup(hostname)
        }
        cache[hostname] = now to ips
        return ips
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
                conn.hostnameVerifier = HostnameVerifier { _, _ -> true }
                conn.setRequestProperty("Accept", "application/dns-json")
                conn.setRequestProperty("Host", sni)
                conn.connectTimeout = 2000
                conn.readTimeout = 2000
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
        throw UnknownHostException("DNS resolution failed for $hostname (DoH fallback failed)")
    }
}
