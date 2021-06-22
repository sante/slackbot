package com.xmartlabs.slackbot

import com.slack.api.bolt.context.Context
import com.slack.api.methods.request.conversations.ConversationsListRequest
import com.slack.api.methods.request.users.UsersListRequest
import com.slack.api.model.Conversation
import com.slack.api.model.ConversationType
import com.slack.api.model.User

// TODO: Use memory to cache the response
object UserChannelRepository {
    private const val PAGE_LIMIT = 1000

    fun getUsers(ctx: Context): List<User> {
        val users = mutableListOf<User>()
        var cursor: String? = null
        while (cursor != "") {
            val rb =
                UsersListRequest.builder()
                    .limit(PAGE_LIMIT)
            val req = if (cursor != null) rb.cursor(cursor).build() else rb.build()
            val list = ctx.client().usersList(req)
            cursor = list?.responseMetadata?.nextCursor
            users += list?.members ?: listOf()
        }
        return users
    }

    fun getConversations(ctx: Context): List<Conversation> {
        val channels = mutableListOf<Conversation>()
        var cursor: String? = null
        while (cursor != "") {
            val rb =
                ConversationsListRequest.builder()
                    .limit(PAGE_LIMIT)
                    .types(listOf(ConversationType.PUBLIC_CHANNEL))
            val req = if (cursor != null) rb.cursor(cursor).build() else rb.build()
            val list = ctx.client().conversationsList(req)
            cursor = list?.responseMetadata?.nextCursor
            channels += list?.channels ?: listOf()
        }
        return channels
    }

    fun toConversationId(ctx: Context, channelName: String): String? {
        val name = channelName.replaceFirst("#", "").trim()
        return UserChannelRepository.getConversations(ctx)
            .firstOrNull { it.name.equals(name, true) }
            ?.id
    }

    fun toUserId(ctx: Context, membersNames: List<String>?): List<String> {
        if (membersNames.isNullOrEmpty()) return emptyList()

        val users = getUsers(ctx)
        ctx.logger.debug(
            "Request: " + membersNames.joinToString() + "\n" +
                    "User: " + users.map { it.name + " - " + it.id }.joinToString()
        )

        return membersNames.map { it.replaceFirst("@", "").trim() }
            .mapNotNull { memberName -> users.firstOrNull { it.name.equals(memberName, true) } }
            .map { it.id }
    }

    fun toUserId(ctx: Context, members: String): String =
        toUserId(ctx, listOf(members)).first()
}
