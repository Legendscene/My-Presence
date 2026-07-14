package com.kyrx.mypresence.domain.repository

interface AssetRepository {
    suspend fun resolveAppIcon(
        packageName: String,
        appName: String,
        userToken: String,
        applicationId: String
    ): String?
}
