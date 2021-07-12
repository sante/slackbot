package com.xmartlabs.slackbot.extensions

import com.slack.api.model.Conversation
import com.slack.api.model.User

object MessageHelper {
    @Suppress("ComplexRedundantLet")
    fun normalizeMessage(
        message: String,
        usersCallback: () -> List<User>,
        conversationsCallback: () -> List<Conversation>,
    ) = replaceUsersInMessage(message, usersCallback)
        .let { newMessage -> replaceChannelsInMessage(newMessage, conversationsCallback) }

    @Suppress("ComplexRedundantLet")
    fun normalizeMessage(
        message: String,
        users: List<User>,
        conversations: List<Conversation>,
    ) = replaceUsersInMessage(message) {
        users
    }.let { newMessage ->
        replaceChannelsInMessage(newMessage) {
            conversations
        }
    }

    private inline fun replaceUsersInMessage(
        message: String,
        userCallback: () -> List<User>,
    ): String {
        var newMessage = message
        val usersInMessage = message.split(" ")
            .filter { it.startsWith("@") }
        if (usersInMessage.isNotEmpty()) {
            val users = userCallback()
            usersInMessage.forEach { userName ->
                val userId = users.find {
                    val normalizedUsername = userName.removePrefix("@")
                    normalizedUsername.equals(it.name, ignoreCase = true) ||
                            normalizedUsername.equals(it.profile.displayName, ignoreCase = true) ||
                            normalizedUsername.equals(it.profile.displayNameNormalized, ignoreCase = true)
                }?.id
                if (userId != null) {
                    newMessage = newMessage.replace(userName, "<@$userId>")
                }
            }
        }
        return newMessage
    }

    private inline fun replaceChannelsInMessage(
        message: String,
        channelsCallback: () -> List<Conversation>,
    ): String {
        var newMessage = message
        val channelsInMessage = message.split(" ")
            .filter { it.startsWith("#") }
        if (channelsInMessage.isNotEmpty()) {
            val usersIds = channelsCallback()
                .associate { it.name.lowercase() to it.id }
            channelsInMessage.forEach { userName ->
                val channelId = usersIds[userName.removePrefix("#").lowercase()]
                if (channelId != null) {
                    newMessage = newMessage.replace(userName, "<#$channelId>")
                }
            }
        }
        return newMessage
    }
}
