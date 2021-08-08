package com.xmartlabs.slackbot.model

import io.rocketbase.toggl.api.model.TimeEntry
import java.time.Duration

data class TogglUserEntryReport(
    val togglUser: TogglUser,
    val entries: List<TimeEntry>,
    val reportUrl: String,
)

val TogglUserEntryReport.wrongFormatEntries: List<TimeEntry>
    get() = entries.filter { (it.projectId ?: 0L) == 0L || it.description.isNullOrBlank() }

val TogglUserEntryReport.wrongFormatTrackedTime: Duration
    get() = Duration.ofMillis(wrongFormatEntries.sumOf { it.duration })

val TogglUserEntryReport.workTime: Duration
    get() = Duration.ofMillis(entries.sumOf { it.duration })
