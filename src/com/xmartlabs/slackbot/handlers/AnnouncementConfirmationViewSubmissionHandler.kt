package com.xmartlabs.slackbot.handlers

import com.slack.api.bolt.context.builtin.ViewSubmissionContext
import com.slack.api.bolt.handler.builtin.ViewSubmissionHandler
import com.slack.api.bolt.request.builtin.ViewSubmissionRequest
import com.slack.api.bolt.response.Response
import com.slack.api.model.kotlin_extension.block.withBlocks
import com.xmartlabs.slackbot.ANNOUNCEMENTS_ENABLED
import com.xmartlabs.slackbot.extensions.logIfError
import com.xmartlabs.slackbot.model.DmAnnouncementRequest
import com.xmartlabs.slackbot.view.AnnouncementViewCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnnouncementConfirmationViewSubmissionHandler : ViewSubmissionHandler {
    companion object {
        private const val NOTIFY_DELAY_IN_MILLIS = 500L
    }

    override fun apply(viewSubmissionRequest: ViewSubmissionRequest, ctx: ViewSubmissionContext): Response {
        val (announcement, users) =
            AnnouncementViewCreator.getProcessedDmAnnouncementRequestFromPayload(viewSubmissionRequest.payload)
        val announcerUserId = viewSubmissionRequest.payload.user.id

        GlobalScope.launch {
            val channels = listOfNotNull(announcement.announceInChannel)
            announce(ctx, announcement, channels + users, announcerUserId)
        }
        return ctx.ack()
    }

    private suspend fun announce(
        ctx: ViewSubmissionContext,
        announcement: DmAnnouncementRequest,
        channelsToSend: List<String>,
        announcerUserId: String?,
    ) = withContext(Dispatchers.IO) {
        val client = ctx.client()
        if (ANNOUNCEMENTS_ENABLED) {
            val usersWithError = mutableListOf<String>()
            ctx.logger.info("Announcement is created: $announcement users: ${channelsToSend.joinToString()}")
            val announcementToSend = AnnouncementViewCreator.createAnnouncement(announcement)
            channelsToSend
                .forEach { userId ->
                    val response = client.chatPostMessage { req ->
                        req.channel(userId)
                            .blocks(announcementToSend)
                            .text("`:loudspeaker: Announcement from: <@${announcement.requester}>`")
                    }
                    if (response.isOk) {
                        ctx.logger.info("Send to: $userId")
                    } else {
                        ctx.logger.logIfError(response) {
                            "Error sending announcement to $userId $response"
                        }
                        usersWithError += userId
                    }
                    delay(NOTIFY_DELAY_IN_MILLIS)
                }
            if (usersWithError.isNotEmpty()) {
                ctx.logger.error("Message was not sent to. $usersWithError")
                client.chatPostMessage { req ->
                    req.channel(announcerUserId)
                        .blocks(
                            withBlocks {
                                section {
                                    markdownText(
                                        "Message couldn't be sent to: ${usersWithError.joinToString { "<@$it>" }} \n" +
                                                "Announcement:\n"
                                    )
                                }
                            } + announcementToSend
                        )
                        .text("Message couldn't be sent to some ussers.")
                }
                    ?.also(ctx.logger::logIfError)
            } else {
                client.chatPostMessage { req ->
                    req.channel(announcerUserId)
                        .text("Announcement was sent!")
                        .blocks(
                            withBlocks { section { markdownText("Announcement was sent!\nAnnouncement:\n") } } +
                                    announcementToSend
                        )
                }
                    ?.also(ctx.logger::logIfError)
            }
        } else {
            client.chatPostMessage { req ->
                req.channel(announcerUserId)
                    .text("Announcements are not enabled")
            }
                ?.also(ctx.logger::logIfError)
        }
    }
}
