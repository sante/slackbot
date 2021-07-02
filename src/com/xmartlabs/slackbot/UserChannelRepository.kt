package com.xmartlabs.slackbot

import com.slack.api.bolt.context.Context
import com.slack.api.methods.request.conversations.ConversationsListRequest
import com.slack.api.methods.request.users.UsersListRequest
import com.slack.api.model.Conversation
import com.slack.api.model.ConversationType
import com.slack.api.model.User

object UserChannelRepository {
    private const val PAGE_LIMIT = 1000
    private var cachedUsers: List<User> = listOf()

    private fun getRemoteUsers(ctx: Context): List<User> {
        val users = mutableListOf<User>()
        var cursor: String? = null
        while (cursor != "") {
            val req = UsersListRequest.builder()
                .limit(PAGE_LIMIT)
                .let { if (cursor != null) it.cursor(cursor).build() else it.build() }
            val list = ctx.client().usersList(req)
            cursor = list?.responseMetadata?.nextCursor
            users += list?.members ?: listOf()
        }
        ctx.logger.debug("User fetched: " + users.joinToString { "${it.name} - ${it.id}" })
        return users
    }

    fun getConversations(ctx: Context): List<Conversation> {
        val channels = mutableListOf<Conversation>()
        var cursor: String? = null
        while (cursor != "") {
            val req = ConversationsListRequest.builder()
                .limit(PAGE_LIMIT)
                .types(listOf(ConversationType.PUBLIC_CHANNEL))
                .let { if (cursor != null) it.cursor(cursor).build() else it.build() }
            val list = ctx.client().conversationsList(req)
            cursor = list?.responseMetadata?.nextCursor
            channels += list?.channels ?: listOf()
        }
        return channels
    }

    fun toConversationId(ctx: Context, channelName: String): String? {
        val name = channelName.replaceFirst("#", "").trim()
        return getConversations(ctx)
            .firstOrNull { it.name.equals(name, true) }
            ?.id
    }

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
