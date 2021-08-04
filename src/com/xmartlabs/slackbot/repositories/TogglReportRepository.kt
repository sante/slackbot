package com.xmartlabs.slackbot.repositories

import com.xmartlabs.slackbot.Config
import com.xmartlabs.slackbot.data.sources.TogglReportsRemoteSource
import com.xmartlabs.slackbot.data.sources.UserTogglRemoteSource
import com.xmartlabs.slackbot.model.TogglUserEntryReport
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import kotlin.time.ExperimentalTime

object TogglReportRepository {
    private val logger = LoggerFactory.getLogger(TogglReportRepository::class.java)

    @OptIn(ExperimentalTime::class)
    suspend fun getEntriesInWrongFormat(
        since: LocalDateTime,
        until: LocalDateTime,
        excludeActiveEntries: Boolean = Config.TOGGL_EXCLUDE_ACTIVE_ENTRIES,
    ): List<TogglUserEntryReport> = coroutineScope {
        kotlin.runCatching {
            val tasksWithoutProjects = async {
                TogglReportsRemoteSource.getTasksWithoutProjects(since, until)
            }
            val togglUsers = UserTogglRemoteSource.getTogglUsers()
                .associateBy { it.userId }
            tasksWithoutProjects.await()
                .filter { if (excludeActiveEntries) it.end != null else true }
                .groupBy { it.userId }
                .mapValues { it.value.sumOf { timeEntry -> timeEntry.duration } }
                .toList()
                .map { (id, duration) ->
                    val user = togglUsers[id]!!
                    TogglUserEntryReport(
                        user,
                        Duration.ofMillis(duration),
                        TogglReportsRemoteSource.generateReportUrl(user, since, until)
                    )
                }
        }.onFailure { logger.error("Error fetching toggl data", it) }
            .getOrNull() ?: listOf()
    }
}
