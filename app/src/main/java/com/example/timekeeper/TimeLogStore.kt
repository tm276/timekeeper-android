package com.example.timekeeper

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import java.io.File
import java.util.UUID

object TimeLogStore {
    val entries = mutableListOf<TimeEntry>()
    var version = mutableIntStateOf(0)

    var activeStartMillis = mutableLongStateOf(0L)
        private set

    var settings: TimeSettings = TimeSettings.default()
        private set

    fun initialize(context: Context) {
        entries.clear()
        entries.addAll(LocalPersistence.loadEntries(context))
        activeStartMillis.longValue = LocalPersistence.loadActiveStart(context)
        settings = LocalPersistence.loadSettings(context)
        CsvWindowManager.rewriteAllWindows(context, settings, entries)
        version.intValue = version.intValue + 1
    }

    fun isRunning(): Boolean {
        return activeStartMillis.longValue > 0L
    }

    fun startNow(context: Context, nowMillis: Long) {
        activeStartMillis.longValue = nowMillis
        LocalPersistence.saveActiveStart(context, activeStartMillis.longValue)
        version.intValue = version.intValue + 1
    }

    fun stopAndSave(context: Context, nowMillis: Long, description: String) {
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
        persistAll(context)
    }

    fun removeEntry(context: Context, id: String) {
        entries.removeAll { it.id == id }
        persistAll(context)
    }

    fun updateSettings(context: Context, newSettings: TimeSettings) {
        settings = newSettings
        LocalPersistence.saveSettings(context, settings)
        CsvWindowManager.rewriteAllWindows(context, settings, entries)
        version.intValue = version.intValue + 1
    }

    fun currentCsvFile(context: Context): File {
        return CsvWindowManager.getCurrentWindowFile(
            context = context,
            settings = settings,
            nowMillis = System.currentTimeMillis()
        )
    }

    private fun persistAll(context: Context) {
        LocalPersistence.saveEntries(context, entries)
        LocalPersistence.saveActiveStart(context, activeStartMillis.longValue)
        CsvWindowManager.rewriteAllWindows(context, settings, entries)
        version.intValue = version.intValue + 1
    }
}