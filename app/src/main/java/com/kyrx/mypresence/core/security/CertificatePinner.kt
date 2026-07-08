package com.kyrx.mypresence.core.security

import okhttp3.CertificatePinner
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

object CertificatePinnerProvider {

    private val discordPins = setOf(
        "sha256/+6sRExbssVjnU74W2ycdqy/V/6H88fI7qFg5DnIxI0Y=",
        "sha256/3XJ6o3C2V1Y8Z0aQ7n5L9mW4tR6kD8fG2hJ0sS4=",
    )

    fun createPinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add("discord.com", discordPins.first())
            .add("discordapp.com", discordPins.first())
            .add("cdn.discordapp.com", discordPins.first())
            .build()
    }

    fun getPins(): Set<String> = discordPins
}
