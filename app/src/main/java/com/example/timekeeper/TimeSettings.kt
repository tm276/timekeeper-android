package com.example.timekeeper

enum class DurationUnit {
    DAYS,
    WEEKS
}

enum class WeekEndDay {
    SUNDAY,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY
}

data class TimeSettings(
    val anchorMillis: Long,
    val durationAmount: Int,
    val durationUnit: DurationUnit,
    val userName: String = "",
    val weekEndDay: WeekEndDay = WeekEndDay.SUNDAY
) {
    companion object {
        fun default(): TimeSettings {
            return TimeSettings(
                anchorMillis = System.currentTimeMillis(),
                durationAmount = 1,
                durationUnit = DurationUnit.WEEKS,
                userName = "",
                weekEndDay = WeekEndDay.SUNDAY
            )
        }
    }
}
