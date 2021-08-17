package com.xmartlabs.slackbot.data.sources

import com.xmartlabs.slackbot.Config
import com.xmartlabs.slackbot.model.TogglUser
import com.xmartlabs.slackbot.model.ToggleSummaryRequest
import com.xmartlabs.slackbot.model.ToggleSummaryResponse
import com.xmartlabs.slackbot.model.ToggleSummarySubGroupType
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.time.LocalDate

object UserTogglRemoteSource {
    private val GET_USERS_URL =
        "https://track.toggl.com/api/v9/organizations/${Config.TOGGL_XL_ORGANIZATION}/workspaces/" +
                "${Config.TOGGL_XL_WORKSPACE}/workspace_users"

    private val GET_SUMMARY =
        "https://track.toggl.com/reports/api/v3/workspace/${Config.TOGGL_XL_WORKSPACE}/summary/time_entries"

    suspend fun getTogglUsers(): List<TogglUser> = TogglApi.client.get(GET_USERS_URL)

    suspend fun getTogglUserSummary(
        startDate: LocalDate,
        endDate: LocalDate,
        subGrouping: ToggleSummarySubGroupType,
    ): ToggleSummaryResponse = TogglApi.client.post(GET_SUMMARY) {
        contentType(ContentType.Application.Json)
        body = ToggleSummaryRequest(
            startDate = startDate,
            endDate = endDate,
            subGrouping = subGrouping.serialName,
        )
    }
}
