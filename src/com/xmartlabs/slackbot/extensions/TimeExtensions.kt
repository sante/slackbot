package com.xmartlabs.slackbot.extensions

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaDuration
import kotlin.time.Duration as KotlinDuration

@OptIn(ExperimentalTime::class)
fun KotlinDuration.toPrettyString(): String = toJavaDuration().toPrettyString()

fun Duration.toPrettyString(includeSeconds: Boolean = false): String {
    val parts: MutableList<String> = ArrayList()
    val days = toDaysPart()
    if (days > 0) {
        parts.add(plural(days, "day"))
    }
    val hours = toHoursPart()
    if (hours > 0 || parts.isNotEmpty()) {
        parts.add(plural(hours.toLong(), "hour"))
    }
    val minutes = toMinutesPart()
    if (minutes > 0 || parts.isNotEmpty()) {
        parts.add(plural(minutes.toLong(), "minute"))
    }
    if (includeSeconds) {
        val seconds = toSecondsPart()
        parts.add(plural(seconds.toLong(), "second"))
    }
    return java.lang.String.join(", ", parts)
}

private fun plural(num: Long, unit: String): String {
    return num.toString() + " " + unit + if (num == 1L) "" else "s"
}

fun LocalDate.isLastWorkingDayOfTheMonth() = this == LocalDate.now(ZoneId.of("America/Montevideo"))
    .toLastWorkingDayOfTheMonth()

fun LocalDate.toLastWorkingDayOfTheMonth(): LocalDate = with(TemporalAdjusters.lastDayOfMonth())
    .with { temporal ->
        when (temporal[ChronoField.DAY_OF_WEEK]) {
            DayOfWeek.SATURDAY.value -> temporal.minus(1, ChronoUnit.DAYS)
            DayOfWeek.SUNDAY.value -> temporal.minus(2, ChronoUnit.DAYS)
            else -> temporal
        }
    }
