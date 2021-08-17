package com.xmartlabs.slackbot.repositories

import com.xmartlabs.slackbot.Config
import com.xmartlabs.slackbot.data.sources.TogglReportsRemoteSource
import com.xmartlabs.slackbot.data.sources.UserTogglRemoteSource
import com.xmartlabs.slackbot.model.FullTogglUserEntryReport
import com.xmartlabs.slackbot.model.SimpleTogglUserEntryReport
import com.xmartlabs.slackbot.model.ToggleSummarySubGroupType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

object TogglReportRepository {
    private val logger = LoggerFactory.getLogger(TogglReportRepository::class.java)

    suspend fun generateFullReport(
        since: LocalDateTime,
        until: LocalDateTime,
        excludeActiveEntries: Boolean = Config.TOGGL_EXCLUDE_ACTIVE_ENTRIES,
    ): List<FullTogglUserEntryReport> = coroutineScope {
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
                    FullTogglUserEntryReport(
                        user,
                        entries,
                        TogglReportsRemoteSource.generateReportUrl(user, since.toLocalDate(), until.toLocalDate())
                    )
                }
        }.onFailure { logger.error("Error fetching toggl data", it) }
            .getOrNull() ?: listOf()
    }

    suspend fun generateSimpleReport(
        since: LocalDate,
        until: LocalDate,
    ): List<SimpleTogglUserEntryReport> = coroutineScope {
        kotlin.runCatching {
            val entriesSummaryDeferred = async {
                UserTogglRemoteSource.getTogglUserSummary(since, until, ToggleSummarySubGroupType.PROJECT)
            }
            val togglUsers = UserTogglRemoteSource.getTogglUsers()

            val userTime: Map<Long, Duration> = entriesSummaryDeferred.await()
                .groups
                .associateBy { it.id.toLong() }
                .mapValues { (_, entries) -> Duration.ofSeconds(entries.subGroups.sumOf { it.seconds ?: 0 }.toLong()) }
            togglUsers
                .map { user ->
                    SimpleTogglUserEntryReport(
                        togglUser = user,
                        workTime = userTime.getOrDefault(user.userId, Duration.ZERO),
                        reportUrl = TogglReportsRemoteSource.generateReportUrl(user, since, until)
                    )
                }
        }.onFailure { logger.error("Error fetching toggl data", it) }
            .getOrNull() ?: listOf()
    }
}
