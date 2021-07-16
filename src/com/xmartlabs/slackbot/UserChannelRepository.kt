package com.xmartlabs.slackbot

import com.slack.api.bolt.context.Context
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.conversations.ConversationsListRequest
import com.slack.api.methods.request.users.UsersListRequest
import com.slack.api.model.Conversation
import com.slack.api.model.ConversationType
import com.slack.api.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger

object UserChannelRepository {
    private const val SLACKBOT_UID = "USLACKBOT"

    private const val PAGE_LIMIT = 1000
    private var cachedUsers: List<User> = listOf()
    private var cachedChannels: List<Conversation> = listOf()

    suspend fun reloadCache(logger: Logger, client: MethodsClient) = withContext(Dispatchers.IO) {
        getRemoteUsers(logger, client)
        getRemoteConversations(logger, client)
    }

    private fun getRemoteUsers(ctx: Context): List<User> =
        getRemoteUsers(ctx.logger, ctx.client())

    private fun getRemoteUsers(logger: Logger, client: MethodsClient): List<User> {
        val users = mutableListOf<User>()
        var cursor: String? = null
        while (cursor != "") {
            val req = UsersListRequest.builder()
                .limit(PAGE_LIMIT)
                .let { if (cursor != null) it.cursor(cursor).build() else it.build() }
            val list = client.usersList(req)
            cursor = list?.responseMetadata?.nextCursor
            users += list?.members ?: listOf()
        }
        logger.info("Remote users fetched")
        logger.debug("User fetched: " + users.joinToString { "${it.name} - ${it.id}" })
        return users
            .also { cachedUsers = it }
    }

    fun getRemoteConversations(ctx: Context): List<Conversation> =
        getRemoteConversations(ctx.logger, ctx.client())

    fun getRemoteConversations(logger: Logger, client: MethodsClient): List<Conversation> {
        val channels = mutableListOf<Conversation>()
        var cursor: String? = null
        while (cursor != "") {
            val req = ConversationsListRequest.builder()
                .limit(PAGE_LIMIT)
                .types(listOf(ConversationType.PUBLIC_CHANNEL))
                .let { if (cursor != null) it.cursor(cursor).build() else it.build() }
            val list = client.conversationsList(req)
            cursor = list?.responseMetadata?.nextCursor
            channels += list?.channels ?: listOf()
        }
        logger.info("Remote channels fetched")
        logger.debug("Remote channels: " + channels.joinToString { "${it.name} - ${it.id}" })
        return channels
            .also { cachedChannels = it }
    }

    fun toConversationId(ctx: Context, channelName: String): String? {
        val name = channelName.replaceFirst("#", "").trim()
        return getRemoteConversations(ctx)
            .firstOrNull { it.name.equals(name, true) }
            ?.id
    }

    fun getUsers(ctx: Context) =
        cachedUsers.ifEmpty { getRemoteUsers(ctx) }

    fun getActiveUsers(ctx: Context) = getUsers(ctx)
        .asSequence()
        .filterNot { it.isBot }
        .filterNot { it.isAppUser }
        .filterNot { it.id == SLACKBOT_UID }
        .filterNot { it.isDeleted }
        .toList()

    fun getUser(ctx: Context, userId: String) =
        cachedUsers.firstOrNull { it.id == userId }
            ?: getRemoteUsers(ctx).firstOrNull { it.id == userId }

    fun getChannel(ctx: Context, channelId: String) =
        cachedChannels.firstOrNull { it.id == channelId }
            ?: getRemoteConversations(ctx).firstOrNull { it.id == channelId }

    @Synchronized
    fun toUserId(ctx: Context, membersNames: List<String>?): List<String> {
        if (membersNames.isNullOrEmpty()) return emptyList()

        val users by lazy { getRemoteUsers(ctx).also { cachedUsers = it } }

        return membersNames.map { it.replaceFirst("@", "").trim() }
            .mapNotNull { memberName ->
                cachedUsers.firstOrNull { it.name.equals(memberName, true) }
                    ?: users.firstOrNull { it.name.equals(memberName, true) }
            }
            .map { it.id }
    }

    fun toUserId(ctx: Context, userName: String): String? =
        toUserId(ctx, listOf(userName)).firstOrNull()
            .also { if (it == null) ctx.logger.warn("User not found $userName") }
}
