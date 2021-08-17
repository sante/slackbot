package com.xmartlabs.slackbot.data.sources

import com.xmartlabs.slackbot.Config
import com.xmartlabs.slackbot.extensions.toTogglApiFormat
import com.xmartlabs.slackbot.model.TogglUser
import io.rocketbase.toggl.api.model.TimeEntry
import io.rocketbase.toggl.api.util.FetchAllDetailed
import java.time.LocalDate
import java.time.LocalDateTime

object TogglReportsRemoteSource {
    fun generateReportUrl(togglUser: TogglUser, from: LocalDate, to: LocalDate) =
        "https://track.toggl.com/reports/summary/${Config.TOGGL_XL_WORKSPACE}" +
                "/from/${from.toTogglApiFormat()}" +
                "/to/${to.toTogglApiFormat()}" +
                "/users/${togglUser.userId}"

    fun getTasks(since: LocalDateTime, until: LocalDateTime): List<TimeEntry> {
        val userWithoutProjects = TogglApi.togglReportApi.detailed()
            .since(since)
            .until(until)
        return FetchAllDetailed.getAll(userWithoutProjects)
    }
}
