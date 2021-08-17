package com.xmartlabs.slackbot.manager

import com.slack.api.model.User
import com.xmartlabs.slackbot.Config
import com.xmartlabs.slackbot.data.sources.BambooHrReportsRemoteSource
import com.xmartlabs.slackbot.extensions.isWorkDay
import com.xmartlabs.slackbot.extensions.max
import com.xmartlabs.slackbot.extensions.min
import com.xmartlabs.slackbot.extensions.toHourMinuteFormat
import com.xmartlabs.slackbot.extensions.toPrettyString
import com.xmartlabs.slackbot.extensions.toRegularFormat
import com.xmartlabs.slackbot.extensions.workingDates
import com.xmartlabs.slackbot.logger
import com.xmartlabs.slackbot.model.BambooTimeOff
import com.xmartlabs.slackbot.model.BambooUser
import com.xmartlabs.slackbot.model.BambooWorkTimeReport
import com.xmartlabs.slackbot.model.FullTogglUserEntryReport
import com.xmartlabs.slackbot.model.SimpleTogglUserEntryReport
import com.xmartlabs.slackbot.model.TogglUserEntryReport
import com.xmartlabs.slackbot.repositories.ConversationSlackRepository
import com.xmartlabs.slackbot.repositories.TogglReportRepository
import com.xmartlabs.slackbot.repositories.UserSlackRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.streams.toList
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object WorkTimeReportManager {
    suspend fun sendTimeReport(
        reportType: ReportType,
        notifyUserId: String,
        reportSort: ReportSort,
        fromDate: LocalDate,
        toDate: LocalDate,
    ) {
        kotlin.runCatching {
            logger.info("Generate ${reportType.reportName} report from $fromDate to $toDate")
            val reportEntries = when (reportType) {
                ReportType.WORKED_HOURS ->
                    generateWorkTimeReport<SimpleTogglUserEntryReport>(fromDate, toDate)
                ReportType.WRONG_TRACKED_ENTRIES_TIME ->
                    generateWorkTimeReport<FullTogglUserEntryReport>(fromDate, toDate)
            }
            val report = generateStringReport(reportType, reportSort, reportEntries)

            val fromFormatted = fromDate.format(MessageManager.LOCAL_DATE_FORMATTER)
            val toFormatted = toDate.format(MessageManager.LOCAL_DATE_FORMATTER)
            val message = "Report from $fromFormatted to $toFormatted \n$report"
            logger.info(message)
            ConversationSlackRepository.sendCsvFile(
                channelId = notifyUserId,
                fileName = "report_${fromDate.toRegularFormat()}_${toDate.toRegularFormat()}",
                title = "Report from $fromFormatted to $toFormatted",
                content = report
            )
        }.onFailure {
            logger.error("Error sending time report", it)
            ConversationSlackRepository.sendMessage(
                channelId = notifyUserId,
                text = "Error Generating Report. Error: ${it.toPrettyString()}"
            )
        }
    }

    suspend fun sendUntrackedTimeReport(
        reportType: ReportPeriodType,
        from: LocalDate,
        to: LocalDate,
    ) {
        val now = LocalDateTime.now()
        logger.info("Fetch toggl entries from: $from to $to")
        generateWorkTimeReport<FullTogglUserEntryReport>(from, to)
            .filter { report ->
                reportType == ReportPeriodType.MONTHLY ||
                        report.togglReport.wrongFormatTrackedTime >=
                        Config.TOGGL_WEEKLY_REPORT_MIN_UNTRACKED_DURATION_TO_NOTIFY
            }
            .onEach { report -> sendDMessageToUser(report.slackUser, report.togglReport, from, to) }
            .also { entries -> sendReportSummaryMessage(entries, now, from, to) }
    }

    private suspend inline fun <reified ToggleReport : TogglUserEntryReport> generateWorkTimeReport(
        fromDate: LocalDate,
        toDate: LocalDate,
    ): List<UserWorkTimeReport<ToggleReport>> = coroutineScope {
        val start = LocalDateTime.now()
        val bambooReportDeferred =
            async {
                getBambooReport(fromDate, toDate)
                    .associateBy { it.bambooUser.workEmail }
            }
        val usersDeferred = async {
            UserSlackRepository.getUsers()
                .associateBy { it.profile.email }
        }
        val togglReportDeferred: Deferred<List<ToggleReport>> = async {
            @Suppress("UNCHECKED_CAST")
            when (ToggleReport::class.java) {
                FullTogglUserEntryReport::class.java -> TogglReportRepository.generateFullReport(
                    fromDate.atTime(LocalTime.MIN), toDate.atTime(LocalTime.MAX)
                )
                SimpleTogglUserEntryReport::class.java -> TogglReportRepository.generateSimpleReport(fromDate, toDate)
                else -> throw IllegalStateException("type is not valid")
            } as List<ToggleReport>
        }
        val bambooReport = bambooReportDeferred.await()
        val slackUsers = usersDeferred.await()
        togglReportDeferred.await()
            .mapNotNull { togglReport ->
                val slackUser = slackUsers[togglReport.togglUser.email]
                if (slackUser == null) {
                    logger.warn("Slack user not found. Toggl user: ${togglReport.togglUser}")
                    null
                } else {
                    UserWorkTimeReport(
                        slackUser = slackUser,
                        togglReport = togglReport,
                        bambooReport = bambooReport[togglReport.togglUser.email]
                    )
                }
            }
            .also {
                val calculationTime = Duration.between(start, LocalDateTime.now())
                    .toPrettyString(true)
                val from = fromDate.format(MessageManager.LOCAL_DATE_FORMATTER)
                val to = toDate.format(MessageManager.LOCAL_DATE_FORMATTER)
                logger.info("Work hours report from $from to $to generated in $calculationTime)")
            }
    }

    private suspend fun getBambooReport(from: LocalDate, to: LocalDate): List<BambooWorkTimeReport> = coroutineScope {
        require(!from.isAfter(to))
        val usersDeferred = async { BambooHrReportsRemoteSource.getUsers() }
        val timeOffDeferred = async { BambooHrReportsRemoteSource.getTimeOff(from, to) }
        (usersDeferred.await() to timeOffDeferred.await())
            .let { (users, timeOffs) ->
                val timeOffMap: Map<String?, List<BambooTimeOff>> = timeOffs.groupBy { it.employeeId }
                users.map { user: BambooUser ->
                    val generalTimeOff = timeOffMap.getOrDefault(null, emptyList())
                        .filter { user.hireDate <= it.end }
                    user to (generalTimeOff + timeOffMap.getOrDefault(user.id, emptyList()))
                }
            }.map { (user, timeOffs) ->
                val fromWorkingDate = max(from, user.hireDate)
                val workingDates = if (fromWorkingDate.isAfter(to)) 0 else workingDates(fromWorkingDate, to)

                val timeOffDates = timeOffs
                    .flatMap { max(it.start, fromWorkingDate).datesUntil(min(it.end.plusDays(1), to)).toList() }
                    .distinct()
                    .filter { !it.isBefore(user.hireDate) }
                    .count { date -> date.isWorkDay() }
                BambooWorkTimeReport(
                    bambooUser = user,
                    workingTime = Duration.ofHours(
                        (workingDates - timeOffDates) * user.workingHours / Config.WORK_DAYS
                    ),
                )
            }.also { logger.info("Bamboo report $it") }
    }

    private suspend fun sendDMessageToUser(
        slackUser: User,
        togglReport: FullTogglUserEntryReport,
        from: LocalDate,
        to: LocalDate,
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
        entries: List<UserWorkTimeReport<FullTogglUserEntryReport>>,
        startReportTime: LocalDateTime?,
        from: LocalDate,
        to: LocalDate,
    ) {
        if (Config.TOGGL_REPORTS_SLACK_CHANNEL_ID.isBlank()) {
            logger.info("Ignore weekly report, report channel was not defined.")
        } else {
            val entriesMessage = entries
                .sortedBy { entry -> entry.togglReport.togglUser.name }
                .formatReports(ReportType.WRONG_TRACKED_ENTRIES_TIME, csv = false)
            val reportDuration = Duration.between(startReportTime, LocalDateTime.now())
                .toPrettyString(true)
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
        report: List<UserWorkTimeReport<*>>,
    ): String = report
        .filter { it.togglReport.duration(reportType) > Config.TOGGL_WEEKLY_REPORT_MIN_UNTRACKED_DURATION_TO_NOTIFY }
        .let { entries ->
            when (reportSort) {
                ReportSort.TIME -> entries.sortedBy { it.togglReport.duration(reportType) }
                ReportSort.ALPHA -> entries.sortedBy { it.togglReport.togglUser.name }
            }
        }
        .formatReports(reportType, csv = true)
}

private fun List<UserWorkTimeReport<*>>.formatReports(reportType: ReportType, csv: Boolean): String {
    val header = when {
        csv && reportType == ReportType.WORKED_HOURS -> "User Name, Worked Time, Worked difference\n"
        csv && reportType == ReportType.WRONG_TRACKED_ENTRIES_TIME ->
            "User Name, Invalid Worked Time, Worked difference\n"
        else -> ""
    }

    return header + joinToString("\n") { entry ->
        val workTime = entry.togglReport.duration(reportType)
        val workDifference = entry.bambooReport
            ?.workingTime
            ?.let { workTime - it }
            ?.toHourMinuteFormat()
            ?: "undefined"
        val start = if (csv) "" else "   • "
        when (reportType) {
            ReportType.WORKED_HOURS ->
                "$start${entry.togglReport.togglUser.name}, ${workTime.toHourMinuteFormat()}, $workDifference"
            ReportType.WRONG_TRACKED_ENTRIES_TIME ->
                "$start${entry.togglReport.togglUser.name}, ${workTime.toHourMinuteFormat()}"
        }
    }
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
    ReportType.WRONG_TRACKED_ENTRIES_TIME -> (this as FullTogglUserEntryReport).wrongFormatTrackedTime
}

private val ReportType.reportName: String
    get() = when (this) {
        ReportType.WORKED_HOURS -> "Worked Hours"
        ReportType.WRONG_TRACKED_ENTRIES_TIME -> "Wrong tracked entries"
    }

private class UserWorkTimeReport<ToggleReport : TogglUserEntryReport>(
    val slackUser: User,
    val togglReport: ToggleReport,
    val bambooReport: BambooWorkTimeReport?,
)
