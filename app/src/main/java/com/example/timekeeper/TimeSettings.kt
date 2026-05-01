package com.example.timekeeper

enum class DurationUnit {
    DAYS,
    WEEKS
}

data class TimeSettings(
    val anchorMillis: Long,
    val durationAmount: Int,
    val durationUnit: DurationUnit
) {
    companion object {
        fun default(): TimeSettings {
            return TimeSettings(
                anchorMillis = System.currentTimeMillis(),
                durationAmount = 7,
                durationUnit = DurationUnit.DAYS
            )
        }
    }
}