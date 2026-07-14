package com.kyrx.mypresence.core.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashReporter @Inject constructor() {

    fun logNonFatal(throwable: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    fun setUserId(id: String) {
        FirebaseCrashlytics.getInstance().setUserId(id)
    }

    fun setCustomKey(key: String, value: String) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Int) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Boolean) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Long) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    fun setCustomKey(key: String, value: Double) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }

    fun log(message: String) {
        FirebaseCrashlytics.getInstance().log(message)
    }
}
