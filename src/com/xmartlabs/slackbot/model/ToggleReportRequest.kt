package com.xmartlabs.slackbot.model

import com.xmartlabs.slackbot.manager.ReportSort
import com.xmartlabs.slackbot.manager.ReportType
import java.time.LocalDate

data class ToggleReportRequest(
    val from: LocalDate,
    val to: LocalDate,
    val sort: ReportSort,
    val type: ReportType,
)
