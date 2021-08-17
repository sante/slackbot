package com.xmartlabs.slackbot.model

import io.rocketbase.toggl.api.model.TimeEntry
import java.time.Duration

sealed interface TogglUserEntryReport {
    val togglUser: TogglUser
    val reportUrl: String
    val workTime: Duration
}

data class FullTogglUserEntryReport(
    override val togglUser: TogglUser,
    val entries: List<TimeEntry>,
    override val reportUrl: String,
) : TogglUserEntryReport {
    private val wrongFormatEntries: List<TimeEntry> =
        entries.filter { (it.projectId ?: 0L) == 0L || it.description.isNullOrBlank() }
    val wrongFormatTrackedTime: Duration = Duration.ofMillis(wrongFormatEntries.sumOf { it.duration })
    override val workTime: Duration = Duration.ofMillis(entries.sumOf { it.duration })
}

class SimpleTogglUserEntryReport(
    override val togglUser: TogglUser,
    override val reportUrl: String,
    override val workTime: Duration,
) : TogglUserEntryReport
