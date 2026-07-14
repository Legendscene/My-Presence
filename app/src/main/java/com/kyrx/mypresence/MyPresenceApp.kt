package com.kyrx.mypresence

import android.app.Application
import com.kyrx.mypresence.core.analytics.AnalyticsManager
import com.kyrx.mypresence.core.analytics.CrashReporter
import com.kyrx.mypresence.domain.repository.AuthRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyPresenceApp : Application() {

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var crashReporter: CrashReporter

    override fun onCreate() {
        super.onCreate()
        val userId = authRepository.getUserIdForCrashReporting()
        if (userId != null) {
            crashReporter.setUserId(userId)
        }
    }
}
