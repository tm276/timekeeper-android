package com.example.timelogger

enum class DurationUnit {
    DAYS,
    WEEKS
}

data class TimeLogSettings(
    val anchorMillis: Long,
    val durationAmount: Int,
    val durationUnit: DurationUnit,
    val remoteMode: String = "local"
)