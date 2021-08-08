package com.xmartlabs.slackbot.repositories

import com.slack.api.model.User
import com.slack.api.model.block.LayoutBlock
import com.xmartlabs.slackbot.Config
import com.xmartlabs.slackbot.data.sources.ConversationSlackRemoteSource
import com.xmartlabs.slackbot.data.sources.SlackRemoteSource
import com.xmartlabs.slackbot.data.sources.UserSlackRemoteSource
import com.xmartlabs.slackbot.logger

object UserSlackRepository : SlackEntityRepository<User>() {
    private const val SLACKBOT_UID = "USLACKBOT"

    override val remoteSource: SlackRemoteSource<User>
        get() = UserSlackRemoteSource

    fun getUsers() = getEntities()

    fun getActiveUsers() = getUsers()
        .asSequence()
        .filterNot { it.isBot }
        .filterNot { it.isAppUser }
        .filterNot { it.id == SLACKBOT_UID }
        .filterNot { it.isDeleted }
        .toList()

    fun getUser(userId: String) = getEntity { it.id == userId }

    @Synchronized
    fun toUserId(membersNames: List<String>?): List<String> {
        if (membersNames.isNullOrEmpty()) return emptyList()

        val users by lazy { getAndCacheRemoteEntities() }

        return membersNames.map { it.replaceFirst("@", "").trim() }
            .mapNotNull { memberName ->
                cachedEntities.firstOrNull { it.name.equals(memberName, true) }
                    ?: users.firstOrNull { it.name.equals(memberName, true) }
            }
            .map { it.id }
    }

    fun toUserId(userName: String): String? =
        toUserId(listOf(userName)).firstOrNull()
            .also { if (it == null) logger.warn("User not found $userName") }

    suspend fun sendMessage(user: User, text: String, blocks: List<LayoutBlock>? = null) =
        sendMessage(user.id, text, blocks)

    suspend fun sendMessage(userId: String, text: String, blocks: List<LayoutBlock>? = null) =
        ConversationSlackRemoteSource.sendMessage(userId, text, blocks)

    fun hasAdminPrivileges(userId: String): Boolean =
        userId in Config.USERS_WITH_ADMIN_PRIVILEGES || getUser(userId)?.isAdmin == true
}
