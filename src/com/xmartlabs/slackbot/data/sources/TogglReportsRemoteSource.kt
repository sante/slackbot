package com.xmartlabs.slackbot.data.sources

import com.xmartlabs.slackbot.model.TogglUser
import com.xmartlabs.slackbot.Config
import io.rocketbase.toggl.api.model.TimeEntry
import io.rocketbase.toggl.api.util.FetchAllDetailed
import java.time.LocalDateTime

object TogglReportsRemoteSource {
    fun generateReportUrl(togglUser: TogglUser, from: LocalDateTime, to: LocalDateTime) =
        "https://track.toggl.com/reports/summary/${Config.TOGGL_XL_WORKSPACE}" +
                "/from/${from.toLocalDate().toTogglApiFormat()}" +
                "/to/${to.toLocalDate().toTogglApiFormat()}" +
                "/users/${togglUser.userId}"

    fun getTasksWithoutProjects(since: LocalDateTime, until: LocalDateTime): List<TimeEntry> {
        val userWithoutProjects = TogglApi.togglReportApi.detailed()
            .since(since)
            .until(until)
        return FetchAllDetailed.getAll(userWithoutProjects)
            .filter { (it.projectId ?: 0L) == 0L || it.description.isNullOrBlank() }
    }
}
