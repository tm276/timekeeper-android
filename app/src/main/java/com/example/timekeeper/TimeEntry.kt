package com.example.timelogger

data class TimeEntry(
    val id: String,
    val startMillis: Long,
    val stopMillis: Long,
    val description: String
)