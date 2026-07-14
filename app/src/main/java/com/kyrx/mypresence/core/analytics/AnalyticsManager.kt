package com.kyrx.mypresence.core.analytics

import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kyrx.mypresence.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            val consented = preferencesRepository.analyticsEnabled.first()
            applyConsent(consented)
        }
    }

    fun setConsent(enabled: Boolean) {
        applyConsent(enabled)
    }

    private fun applyConsent(enabled: Boolean) {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
        Firebase.analytics.setAnalyticsCollectionEnabled(enabled)
    }
}
