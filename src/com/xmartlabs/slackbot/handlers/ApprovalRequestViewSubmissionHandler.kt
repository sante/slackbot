package com.xmartlabs.slackbot.handlers

import com.slack.api.bolt.context.builtin.ViewSubmissionContext
import com.slack.api.bolt.handler.builtin.ViewSubmissionHandler
import com.slack.api.bolt.request.builtin.ViewSubmissionRequest
import com.slack.api.bolt.response.Response
import com.slack.api.model.User
import com.xmartlabs.slackbot.ANNOUNCEMENTS_ENABLED
import com.xmartlabs.slackbot.ANNOUNCEMENTS_PROTECTED_FEATURE
import com.xmartlabs.slackbot.USERS_WITH_ADMIN_PRIVILEGES
import com.xmartlabs.slackbot.UserChannelRepository
import com.xmartlabs.slackbot.extensions.MessageHelper
import com.xmartlabs.slackbot.extensions.logIfError
import com.xmartlabs.slackbot.model.DmAnnouncementRequest
import com.xmartlabs.slackbot.model.FilterMode
import com.xmartlabs.slackbot.view.AnnouncementViewCreator

class ApprovalRequestViewSubmissionHandler : ViewSubmissionHandler {
    override fun apply(viewSubmissionRequest: ViewSubmissionRequest, ctx: ViewSubmissionContext): Response {
        val announcementRequestFromPayload = AnnouncementViewCreator.fromPayload(viewSubmissionRequest.payload, ctx)
        // Return the ack here because otherwise a time out could be generated
        val ackResponse =
            if (announcementRequestFromPayload.filterMode == FilterMode.INCLUSIVE &&
                announcementRequestFromPayload.filterUsers.isNullOrEmpty()
            ) {
                ctx.ackWithErrors(
                    mapOf(
                        AnnouncementViewCreator.usersToFilterBlockId to
                                "The users filter is not valid. " +
                                "If you use the inclusive mode you have to add at least one user."
                    )
                )
            } else {
                ctx.ack()
            }
        val users = UserChannelRepository.getActiveUsers(ctx)
        val normalizedAnnouncement = announcementRequestFromPayload.copy(
            details = MessageHelper.normalizeMessage(
                message = announcementRequestFromPayload.details,
                usersCallback = { users },
                conversationsCallback = { UserChannelRepository.getConversations(ctx) }
            )
        )
        val announcerUserId = viewSubmissionRequest.payload.user.id
        val announcerUser = UserChannelRepository.getUser(ctx, announcerUserId)
        val client = ctx.client()
        if (ANNOUNCEMENTS_PROTECTED_FEATURE &&
            announcerUser?.isAdmin != true &&
            announcerUserId !in USERS_WITH_ADMIN_PRIVILEGES
        ) {
            client.chatPostMessage { req ->
                req.channel(announcerUserId)
                    .text("I'm afraid, but you have not permissions to perform this action. :homer_sad:")
            }?.also(ctx.logger::logIfError)
        } else {
            val usersToSend = users
                .let { userToSend ->
                    if (normalizedAnnouncement.filterMode == FilterMode.INCLUSIVE) {
                        userToSend.filter { it.id in normalizedAnnouncement.filterUsers ?: emptyList() }
                    } else {
                        userToSend.filterNot { it.id in normalizedAnnouncement.filterUsers ?: emptyList() }
                    }
                }
            if (usersToSend.isEmpty()) {
                client.chatPostMessage { req ->
                    req.channel(announcerUserId)
                        .text("You tried to create a announcement but the user filter is not valid. :homer_sad:")
                }?.also(ctx.logger::logIfError)
            } else {
                announceUsers(ctx, normalizedAnnouncement, usersToSend, announcerUserId)
            }
        }
        return ackResponse
    }

    private fun announceUsers(
        ctx: ViewSubmissionContext,
        announcement: DmAnnouncementRequest,
        usersToSend: List<User>,
        announcerUserId: String?,
    ) {
        val client = ctx.client()
        if (ANNOUNCEMENTS_ENABLED) {
            ctx.logger.info("Announcement is created: $announcement users: ${usersToSend.joinToString()}")
            usersToSend
                .forEach { user ->
                    val response = client.chatPostMessage { req ->
                        req.channel(user.id)
                            .blocks(AnnouncementViewCreator.createAnnouncement(announcement))
                            .text("`:loudspeaker: Announcement from: <@${announcement.requester}>`")
                    }
                    ctx.logger.logIfError(response) {
                        "Error sending announcement to ${user.id}:${user.name} $response"
                    }
                }
        } else {
            client.chatPostMessage { req ->
                req.channel(announcerUserId)
                    .text("`Announcements are not enabled.\n" +
                            "It would be sent to ${usersToSend.joinToString { it.name }}\n" +
                            "The content would be:\n")
            }
                ?.also(ctx.logger::logIfError)
            client.chatPostMessage { req ->
                req.channel(announcerUserId)
                    .blocks(AnnouncementViewCreator.createAnnouncement(announcement))
            }
                ?.also(ctx.logger::logIfError)
        }
    }
}
