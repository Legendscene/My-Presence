package com.kyrx.mypresence.feature.diagnostics

data class LogEntry(
    val id: Long,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val level: Level = Level.INFO
) {
    enum class Level { DEBUG, INFO, WARN, ERROR }

    val formattedTime: String
        get() {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
            return String.format(
                "%02d:%02d:%02d",
                cal.get(java.util.Calendar.HOUR_OF_DAY),
                cal.get(java.util.Calendar.MINUTE),
                cal.get(java.util.Calendar.SECOND)
            )
        }
}
