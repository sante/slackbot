package com.xmartlabs.slackbot.manager

import com.slack.api.model.User
import com.xmartlabs.slackbot.Config
import com.xmartlabs.slackbot.logger
import com.xmartlabs.slackbot.model.TogglUserEntryReport
import com.xmartlabs.slackbot.model.workTime
import com.xmartlabs.slackbot.model.wrongFormatTrackedTime
import com.xmartlabs.slackbot.repositories.ConversationSlackRepository
import com.xmartlabs.slackbot.repositories.TogglReportRepository
import com.xmartlabs.slackbot.repositories.UserSlackRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinDuration

@OptIn(ExperimentalTime::class)
object TogglReportManager {
    suspend fun sendTimeReport(
        reportType: ReportType,
        notifyUserId: String,
        reportSort: ReportSort,
        fromDate: LocalDate,
        toDate: LocalDate,
    ) {
        logger.info("Generate ${reportType.reportName} report from $fromDate to $toDate")
        val report = generateStringReport(
            reportType,
            reportSort,
            TogglReportRepository.generateReport(fromDate.atTime(LocalTime.MIN), toDate.atTime(LocalTime.MAX))
        )
        val fromFormatted = fromDate.format(MessageManager.LOCAL_DATE_FORMATTER)
        val toFormatted = toDate.format(MessageManager.LOCAL_DATE_FORMATTER)
        val message = "Report from $fromFormatted to $toFormatted \n$report"
        logger.info(message)
        UserSlackRepository.sendMessage(notifyUserId, message)
    }

    suspend fun sendUntrackedTimeReport(
        reportType: ReportPeriodType,
        from: LocalDateTime,
        to: LocalDateTime,
    ) {
        val now = LocalDateTime.now()
        val users = UserSlackRepository.getUsers()
            .associateBy { it.profile.email }
        logger.info("Users: ${users.keys}")

        logger.info("Fetch toggl entries from: $from to $to")
        TogglReportRepository.generateReport(from, to)
            .filter { report ->
                reportType == ReportPeriodType.MONTHLY ||
                        report.wrongFormatTrackedTime >= Config.TOGGL_WEEKLY_REPORT_MIN_UNTRACKED_DURATION_TO_NOTIFY
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
            untrackedTime = togglReport.wrongFormatTrackedTime,
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
            val entriesMessage = entries
                .map { (_, entry) -> entry }
                .sortedBy { entry -> entry.togglUser.name }
                .formatReports(ReportType.WRONG_TRACKED_ENTRIES_TIME)
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

    private fun generateStringReport(
        reportType: ReportType,
        reportSort: ReportSort,
        report: List<TogglUserEntryReport>,
    ): String = report
        .filter { it.duration(reportType) > Config.TOGGL_WEEKLY_REPORT_MIN_UNTRACKED_DURATION_TO_NOTIFY }
        .let { entries ->
            when (reportSort) {
                ReportSort.TIME -> entries.sortedBy { it.duration(reportType) }
                ReportSort.ALPHA -> entries.sortedBy { it.togglUser.name }
            }
        }
        .formatReports(reportType)
}

@Suppress("MagicNumber")
private fun List<TogglUserEntryReport>.formatReports(reportType: ReportType) = joinToString("\n") { entry ->
    val (hours, minutes) = entry.duration(reportType)
        .let { workTime -> workTime.toHours() to workTime.toMinutesPart() }
    "   • ${entry.togglUser.name}, $hours:${if (minutes < 10) "0$minutes" else minutes}"
}

enum class ReportPeriodType {
    MONTHLY,
    WEEKLY,
}

enum class ReportSort {
    ALPHA,
    TIME,
}

enum class ReportType {
    WORKED_HOURS,
    WRONG_TRACKED_ENTRIES_TIME,
}

private fun TogglUserEntryReport.duration(reportType: ReportType) = when (reportType) {
    ReportType.WORKED_HOURS -> workTime
    ReportType.WRONG_TRACKED_ENTRIES_TIME -> wrongFormatTrackedTime
}

private val ReportType.reportName: String
    get() = when (this) {
        ReportType.WORKED_HOURS -> "Worked Hours"
        ReportType.WRONG_TRACKED_ENTRIES_TIME -> "Wrong tracked entries"
    }
