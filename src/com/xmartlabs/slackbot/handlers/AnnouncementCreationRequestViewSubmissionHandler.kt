package com.xmartlabs.slackbot.handlers

import com.slack.api.bolt.context.builtin.ViewSubmissionContext
import com.slack.api.bolt.handler.builtin.ViewSubmissionHandler
import com.slack.api.bolt.request.builtin.ViewSubmissionRequest
import com.slack.api.bolt.response.Response
import com.xmartlabs.slackbot.ANNOUNCEMENTS_PROTECTED_FEATURE
import com.xmartlabs.slackbot.BOT_USER_ID
import com.xmartlabs.slackbot.USERS_WITH_ADMIN_PRIVILEGES
import com.xmartlabs.slackbot.UserChannelRepository
import com.xmartlabs.slackbot.extensions.MessageHelper
import com.xmartlabs.slackbot.model.FilterMode
import com.xmartlabs.slackbot.view.AnnouncementViewCreator

class AnnouncementCreationRequestViewSubmissionHandler : ViewSubmissionHandler {
    override fun apply(viewSubmissionRequest: ViewSubmissionRequest, ctx: ViewSubmissionContext): Response {
        val announcementRequestFromPayload =
            AnnouncementViewCreator.getDmAnnouncementRequestFromPayload(viewSubmissionRequest.payload, ctx)
        // Return the ack here because otherwise a time out could be generated
        val users = UserChannelRepository.getActiveUsers(ctx)
        val normalizedAnnouncement = announcementRequestFromPayload.copy(
            details = MessageHelper.normalizeMessage(
                message = announcementRequestFromPayload.details,
                usersCallback = { users },
                conversationsCallback = { UserChannelRepository.getRemoteConversations(ctx) }
            )
        )
        val channel = announcementRequestFromPayload.announceInChannel
            ?.let { UserChannelRepository.getChannel(ctx, it) }
        ctx.logger.debug("channel ${announcementRequestFromPayload.announceInChannel} $channel")
        ctx.logger.debug("payload ${viewSubmissionRequest.payload}")
        if (channel?.isMember == false && channel.isPrivate) {
            return ctx.ackWithErrors(
                mapOf(
                    AnnouncementViewCreator.channelFilterBlockId to
                            "Yo have to invite <@$BOT_USER_ID> to <#${channel.id}> before send the announcement."
                )
            )
        }

        val announcerUserId = viewSubmissionRequest.payload.user.id
        val announcerUser = UserChannelRepository.getUser(ctx, announcerUserId)
        require(!ANNOUNCEMENTS_PROTECTED_FEATURE ||
                announcerUser?.isAdmin == true ||
                announcerUserId in USERS_WITH_ADMIN_PRIVILEGES
        )
        val usersToSend = users
            .let { userToSend ->
                if (normalizedAnnouncement.filterMode == FilterMode.INCLUSIVE) {
                    userToSend.filter { it.id in normalizedAnnouncement.filterUsers ?: emptyList() }
                } else {
                    userToSend.filterNot { it.id in normalizedAnnouncement.filterUsers ?: emptyList() }
                }
            }
        return if (usersToSend.isEmpty() && channel == null) {
            ctx.ackWithErrors(
                mapOf(
                    AnnouncementViewCreator.usersFilterBlockId to
                            "The users filter is not valid. " +
                            "If you use the inclusive mode you have to add at least one user."
                )
            )
        } else {
            ctx.ackWithUpdate(AnnouncementViewCreator
                .createAnnouncementConfirmation(normalizedAnnouncement, usersToSend, listOfNotNull(channel)))
        }
    }
}
