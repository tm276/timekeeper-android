package com.example.timekeeper

import android.content.Context
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max

object CsvWindowManager {

    fun getCurrentWindowFile(
        context: Context,
        client: ClientProfile,
        settings: TimeSettings,
        nowMillis: Long
    ): File {
        val window = calculateWindow(settings, nowMillis)
        val safeClientName = sanitizeFileName(client.clientName)
        val fileName = "timelog_${safeClientName}_${window.startDate}_to_${window.endDate}.csv"
        return File(getClientFolder(context, client), fileName)
    }

    fun writeCurrentWindowCsv(
        context: Context,
        client: ClientProfile,
        settings: TimeSettings,
        entries: List<TimeEntry>,
        nowMillis: Long = System.currentTimeMillis()
    ): File {
        val window = calculateWindow(settings, nowMillis)
        val file = getCurrentWindowFile(context, client, settings, nowMillis)

        val filteredEntries = entries.filter { entry ->
            entry.clientId == client.id &&
                    entry.stopMillis in window.startMillis until window.endExclusiveMillis
        }

        val lines = mutableListOf<String>()
        lines.add("name,client,date,startTime,stopTime,durationMinutes,description")

        filteredEntries
            .sortedBy { it.startMillis }
            .forEach { entry ->
                val date = csvEscape(TimeFormatUtils.formatDate(entry.startMillis))
                val start = csvEscape(TimeFormatUtils.formatTime(entry.startMillis))
                val stop = csvEscape(TimeFormatUtils.formatTime(entry.stopMillis))
                val duration = TimeFormatUtils.formatDurationMinutes(
                    startMillis = entry.startMillis,
                    stopMillis = entry.stopMillis
                )
                val name = csvEscape(client.userName.ifBlank { settings.userName })
                val clientName = csvEscape(client.clientName)
                val description = csvEscape(entry.description)

                lines.add("$name,$clientName,$date,$start,$stop,$duration,$description")
            }

        file.writeText(lines.joinToString("\n"))
        return file
    }

    fun rewriteAllWindows(
        context: Context,
        client: ClientProfile,
        settings: TimeSettings,
        entries: List<TimeEntry>
    ) {
        val clientEntries = entries.filter { it.clientId == client.id }
        if (clientEntries.isEmpty()) return

        deleteExistingWindowFiles(context, client)

        val grouped = clientEntries.groupBy { entry ->
            calculateWindow(settings, entry.stopMillis)
        }

        grouped.forEach { (window, windowEntries) ->
            val safeClientName = sanitizeFileName(client.clientName)
            val file = File(
                getClientFolder(context, client),
                "timelog_${safeClientName}_${window.startDate}_to_${window.endDate}.csv"
            )

            val lines = mutableListOf<String>()
            lines.add("name,client,date,startTime,stopTime,durationMinutes,description")

            windowEntries
                .sortedBy { it.startMillis }
                .forEach { entry ->
                    val date = csvEscape(TimeFormatUtils.formatDate(entry.startMillis))
                    val start = csvEscape(TimeFormatUtils.formatTime(entry.startMillis))
                    val stop = csvEscape(TimeFormatUtils.formatTime(entry.stopMillis))
                    val duration = TimeFormatUtils.formatDurationMinutes(
                        startMillis = entry.startMillis,
                        stopMillis = entry.stopMillis
                    )
                    val name = csvEscape(client.userName.ifBlank { settings.userName })
                    val clientName = csvEscape(client.clientName)
                    val description = csvEscape(entry.description)

                    lines.add("$name,$clientName,$date,$start,$stop,$duration,$description")
                }

            file.writeText(lines.joinToString("\n"))
        }
    }

    fun calculateWindow(
        settings: TimeSettings,
        targetMillis: Long
    ): CsvWindow {
        val zone = ZoneId.systemDefault()
        val targetDate = Instant.ofEpochMilli(targetMillis).atZone(zone).toLocalDate()

        val unitDays = when (settings.durationUnit) {
            DurationUnit.DAYS -> max(1, settings.durationAmount)
            DurationUnit.WEEKS -> max(1, settings.durationAmount) * 7
        }

        val anchorDate = alignedAnchorDate(settings, zone)
        val daysBetween = ChronoUnit.DAYS.between(anchorDate, targetDate)

        val windowIndex = if (daysBetween >= 0) {
            daysBetween / unitDays
        } else {
            (daysBetween - (unitDays - 1)) / unitDays
        }

        val windowStartDate = anchorDate.plusDays(windowIndex * unitDays.toLong())
        val windowEndExclusiveDate = windowStartDate.plusDays(unitDays.toLong())
        val windowEndDisplayDate = windowEndExclusiveDate.minusDays(1)

        val startMillis = windowStartDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endExclusiveMillis = windowEndExclusiveDate.atStartOfDay(zone).toInstant().toEpochMilli()

        return CsvWindow(
            startMillis = startMillis,
            endExclusiveMillis = endExclusiveMillis,
            startDate = windowStartDate.toString(),
            endDate = windowEndDisplayDate.toString()
        )
    }

    private fun alignedAnchorDate(settings: TimeSettings, zone: ZoneId): LocalDate {
        val rawAnchorDate = Instant.ofEpochMilli(settings.anchorMillis).atZone(zone).toLocalDate()

        return when (settings.durationUnit) {
            DurationUnit.DAYS -> rawAnchorDate
            DurationUnit.WEEKS -> {
                val weekEnd = settings.weekEndDay.toJavaDayOfWeek()
                val weekStart = weekEnd.plus(1)
                moveToPreviousOrSame(rawAnchorDate, weekStart)
            }
        }
    }

    private fun moveToPreviousOrSame(date: LocalDate, targetDay: DayOfWeek): LocalDate {
        var current = date
        while (current.dayOfWeek != targetDay) {
            current = current.minusDays(1)
        }
        return current
    }

    private fun WeekEndDay.toJavaDayOfWeek(): DayOfWeek {
        return when (this) {
            WeekEndDay.SUNDAY -> DayOfWeek.SUNDAY
            WeekEndDay.MONDAY -> DayOfWeek.MONDAY
            WeekEndDay.TUESDAY -> DayOfWeek.TUESDAY
            WeekEndDay.WEDNESDAY -> DayOfWeek.WEDNESDAY
            WeekEndDay.THURSDAY -> DayOfWeek.THURSDAY
            WeekEndDay.FRIDAY -> DayOfWeek.FRIDAY
            WeekEndDay.SATURDAY -> DayOfWeek.SATURDAY
        }
    }

    private fun deleteExistingWindowFiles(context: Context, client: ClientProfile) {
        val safeClientName = sanitizeFileName(client.clientName)
        getClientFolder(context, client)
            .listFiles()
            ?.filter { file ->
                file.isFile &&
                        file.name.startsWith("timelog_${safeClientName}_") &&
                        file.name.endsWith(".csv")
            }
            ?.forEach { it.delete() }
    }

    fun getClientFolder(context: Context, client: ClientProfile): File {
        val relativeFolder = client.localFolder.ifBlank {
            "defaultfolder/${sanitizeFileName(client.clientName)}"
        }

        val folder = File(context.filesDir, relativeFolder)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun sanitizeFileName(value: String): String {
        return value
            .trim()
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "client" }
    }
}

data class CsvWindow(
    val startMillis: Long,
    val endExclusiveMillis: Long,
    val startDate: String,
    val endDate: String
)
