package com.kyrx.mypresence.core.common

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : AppResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun errorMessageOrNull(): String? = when (this) {
        is Success -> null
        is Error -> message
    }

    companion object {
        fun <T> success(data: T): AppResult<T> = Success(data)
        fun error(message: String, exception: Throwable? = null): AppResult<Nothing> = Error(message, exception)
    }
}
