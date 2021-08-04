package com.xmartlabs.slackbot.manager

import com.slack.api.model.User
import com.xmartlabs.slackbot.Config
import com.xmartlabs.slackbot.extensions.toPrettyString
import com.xmartlabs.slackbot.logger
import com.xmartlabs.slackbot.model.TogglUserEntryReport
import com.xmartlabs.slackbot.repositories.ConversationSlackRepository
import com.xmartlabs.slackbot.repositories.TogglReportRepository
import com.xmartlabs.slackbot.repositories.UserSlackRepository
import java.time.Duration
import java.time.LocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinDuration

@OptIn(ExperimentalTime::class)
object TogglReportManager {
    suspend fun sendReport(
        reportType: ReportType,
        from: LocalDateTime,
        to: LocalDateTime,
    ) {
        val now = LocalDateTime.now()
        val users = UserSlackRepository.getUsers()
            .associateBy { it.profile.email }
        logger.info("Users: ${users.keys}")

        logger.info("Fetch toggl entries from: $from to $to")
        TogglReportRepository.getEntriesInWrongFormat(from, to)
            .filter { (_, untrackedTime) ->
                reportType == ReportType.MONTHLY ||
                        untrackedTime >= Config.TOGGL_WEEKLY_REPORT_MIN_UNTRACKED_DURATION_TO_NOTIFY
            }
            .mapNotNull { report ->
                val slackUser = users[report.togglUser.email]
                if (slackUser == null) {
                    logger.warn("Slack user not found. Toggl user: ${report.togglUser}")
                    null
                } else {
                    slackUser to report
                }
            }
            .also {
                val calculationTime = Duration.between(now, LocalDateTime.now())
                    .toKotlinDuration()
                logger.info("Toogle report generated (generation time $calculationTime)")
            }
            .onEach { (slackUser, togglReport) -> sendDMessageToUser(slackUser, togglReport, from, to) }
            .also { entries -> sendReportSummaryMessage(entries, now, from, to) }
    }

    private suspend fun sendDMessageToUser(
        slackUser: User,
        togglReport: TogglUserEntryReport,
        from: LocalDateTime,
        to: LocalDateTime,
    ) {
        val message = MessageManager.getInvalidTogglEntriesMessage(
            userId = slackUser.id,
            untrackedTime = togglReport.untrackedTime,
            from = from,
            to = to,
            reportUrl = togglReport.reportUrl,
        )
        UserSlackRepository.sendMessage(slackUser, message)
        logger.debug("Send toggle message to ${togglReport.togglUser.email}: $message")
    }

    private suspend fun sendReportSummaryMessage(
        entries: List<Pair<User, TogglUserEntryReport>>,
        startReportTime: LocalDateTime?,
        from: LocalDateTime,
        to: LocalDateTime,
    ) {
        if (Config.TOGGL_REPORTS_SLACK_CHANNEL_ID.isBlank()) {
            logger.info("Ignore weekly report, report channel was not defined.")
        } else {
            val entriesMessage = entries.joinToString("\n") { (slackUser, togglReport) ->
                "${slackUser.profile.email}: ${togglReport.untrackedTime.toPrettyString()}"
            }
            val reportDuration = Duration.between(startReportTime, LocalDateTime.now())
                .toKotlinDuration()
            val reportGeneratedInfo = "Reported generated " +
                    "from ${from.format(MessageManager.LOCAL_DATE_FORMATTER)} " +
                    "to ${to.format(MessageManager.LOCAL_DATE_FORMATTER)}."
            val message = "*Toggl report sent to:* :toggl_on: \n" +
                    "$entriesMessage\n\n$reportGeneratedInfo in $reportDuration"
            logger.info(message)
            ConversationSlackRepository.sendMessage(
                Config.TOGGL_REPORTS_SLACK_CHANNEL_ID,
                message
            )
        }
    }
}

enum class ReportType {
    MONTHLY,
    WEEKLY,
}
