package com.xmartlabs.slackbot.data.sources

import com.xmartlabs.slackbot.Config
import com.xmartlabs.slackbot.model.TogglUser
import io.ktor.client.request.get

object UserTogglRemoteSource {
    private val GET_USERS_URL =
        "https://track.toggl.com/api/v9/organizations/${Config.TOGGL_XL_ORGANIZATION}/workspaces/" +
                "${Config.TOGGL_XL_WORKSPACE}/workspace_users"

    suspend fun getTogglUsers(): List<TogglUser> = TogglApi.client.get(GET_USERS_URL)
}
