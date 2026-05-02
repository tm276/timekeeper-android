package com.example.timekeeper

data class TimeEntry(
    val clientId: String,
    val startMillis: Long,
    val stopMillis: Long,
    val description: String,
    val durationMinutes: Long,
)