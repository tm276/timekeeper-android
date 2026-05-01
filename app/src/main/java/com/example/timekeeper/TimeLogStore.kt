package com.example.timelogger

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import java.util.UUID

object TimeLogStore {
    val entries = mutableListOf<TimeEntry>()
    var version = mutableIntStateOf(0)

    var activeStartMillis = mutableLongStateOf(0L)
        private set

    fun isRunning(): Boolean {
        return activeStartMillis.longValue > 0L
    }

    fun startNow(nowMillis: Long) {
        activeStartMillis.longValue = nowMillis
        version.intValue = version.intValue + 1
    }

    fun stopAndSave(nowMillis: Long, description: String) {
        val start = activeStartMillis.longValue
        if (start <= 0L) return

        entries.add(
            TimeEntry(
                id = UUID.randomUUID().toString(),
                startMillis = start,
                stopMillis = nowMillis,
                description = description.trim()
            )
        )

        activeStartMillis.longValue = 0L
        version.intValue = version.intValue + 1
    }

    fun cancelActiveSession() {
        activeStartMillis.longValue = 0L
        version.intValue = version.intValue + 1
    }

    fun removeEntry(id: String) {
        entries.removeAll { it.id == id }
        version.intValue = version.intValue + 1
    }
}