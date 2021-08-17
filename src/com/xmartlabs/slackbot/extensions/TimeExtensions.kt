@file:Suppress("TooManyFunctions")
package com.xmartlabs.slackbot.extensions

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.absoluteValue

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

fun LocalDate.formatUsingSlackFormatter(): String = toRegularFormat()

fun LocalDate.toTogglApiFormat(): String = toRegularFormat()

fun LocalDate.toRegularFormat(): String = format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

fun LocalDate.isWorkDay(): Boolean = !setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(dayOfWeek)

fun workingDates(start: LocalDate, end: LocalDate): Long {
    return start.datesUntil(end)
        .filter { date -> date.isWorkDay() }
        .count()
}

fun min(localDate1: LocalDate, localDate2: LocalDate) = if (localDate1.isBefore(localDate2)) localDate1 else localDate2

fun max(localDate1: LocalDate, localDate2: LocalDate) = if (localDate1.isAfter(localDate2)) localDate1 else localDate2

@Suppress("MagicNumber")
fun Duration.toHourMinuteFormat(): String {
    val hours = toHours()
        .absoluteValue
        .toString()
    val minutes = toMinutesPart()
        .absoluteValue
        .let { minutes -> if (minutes < 10) "0$minutes" else minutes }
    val sign = if (this < Duration.ZERO) "-" else ""
    return "$sign$hours:$minutes"
}
