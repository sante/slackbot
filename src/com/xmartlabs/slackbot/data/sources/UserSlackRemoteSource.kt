package com.xmartlabs.slackbot.data.sources

import com.slack.api.methods.request.users.UsersListRequest
import com.slack.api.model.User

object UserSlackRemoteSource : SlackRemoteSource<User>() {
    override fun getRemoteEntities(): List<User> {
        val users = mutableListOf<User>()
        var cursor: String? = null
        do {
            val req = UsersListRequest.builder()
                .limit(PAGE_LIMIT)
                .let { if (cursor != null) it.cursor(cursor).build() else it.build() }
            val listResponse = slackMethods.usersList(req)
            if (!listResponse.isOk) {
                throw IllegalStateException("Error getting remote users. Error: ${listResponse.error}")
            }
            cursor = listResponse?.responseMetadata?.nextCursor
            users += listResponse?.members ?: listOf()
        } while (!cursor.isNullOrBlank())
        defaultLogger.info("Remote users fetched")
        defaultLogger.debug("User fetched: " + users.joinToString { "${it.name} - ${it.id}" })
        return users
    }
}
