package com.xmartlabs.slackbot.repositories

import com.xmartlabs.slackbot.Config
import com.xmartlabs.slackbot.data.sources.TogglReportsRemoteSource
import com.xmartlabs.slackbot.data.sources.UserTogglRemoteSource
import com.xmartlabs.slackbot.model.TogglUserEntryReport
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

object TogglReportRepository {
    private val logger = LoggerFactory.getLogger(TogglReportRepository::class.java)

    suspend fun generateReport(
        since: LocalDateTime,
        until: LocalDateTime,
        excludeActiveEntries: Boolean = Config.TOGGL_EXCLUDE_ACTIVE_ENTRIES,
    ): List<TogglUserEntryReport> = coroutineScope {
        kotlin.runCatching {
            val tasksWithoutProjects = async {
                TogglReportsRemoteSource.getTasks(since, until)
            }
            val togglUsers = UserTogglRemoteSource.getTogglUsers()
                .associateBy { it.userId }
            tasksWithoutProjects.await()
                .filter { if (excludeActiveEntries) it.end != null else true }
                .groupBy { it.userId }
                .toList()
                .map { (id, entries) ->
                    val user = togglUsers[id]!!
                    TogglUserEntryReport(
                        user,
                        entries,
                        TogglReportsRemoteSource.generateReportUrl(user, since, until)
                    )
                }
        }.onFailure { logger.error("Error fetching toggl data", it) }
            .getOrNull() ?: listOf()
    }
}
