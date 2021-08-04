package com.xmartlabs.slackbot.handlers

import com.slack.api.bolt.context.builtin.ViewSubmissionContext
import com.slack.api.bolt.handler.builtin.ViewSubmissionHandler
import com.slack.api.bolt.request.builtin.ViewSubmissionRequest
import com.slack.api.bolt.response.Response
import com.slack.api.model.kotlin_extension.block.withBlocks
import com.xmartlabs.slackbot.Config
import com.xmartlabs.slackbot.model.DmAnnouncementRequest
import com.xmartlabs.slackbot.repositories.UserSlackRepository
import com.xmartlabs.slackbot.view.AnnouncementViewCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnnouncementConfirmationViewSubmissionHandler : ViewSubmissionHandler {
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
        announcerUserId: String,
    ) = withContext(Dispatchers.IO) {
        if (Config.ANNOUNCEMENTS_ENABLED) {
            val usersWithError = mutableListOf<String>()
            ctx.logger.info("Announcement is created: $announcement users: ${channelsToSend.joinToString()}")
            val announcementToSend = AnnouncementViewCreator.createAnnouncement(announcement)
            channelsToSend
                .forEach { userId ->
                    UserSlackRepository.sendMessage(
                        userId,
                        "`:loudspeaker: Announcement from: <@${announcement.requester}>`",
                        announcementToSend
                    ).also { sent ->
                        if (!sent) usersWithError += userId
                    }
                }
            if (usersWithError.isNotEmpty()) {
                ctx.logger.error("Message was not sent to. $usersWithError")
                UserSlackRepository.sendMessage(
                    announcerUserId,
                    "Message couldn't be sent to some users.",
                    withBlocks {
                        section {
                            markdownText(
                                "Message couldn't be sent to: ${usersWithError.joinToString { "<@$it>" }} \n" +
                                        "Announcement:\n"
                            )
                        }
                    } + announcementToSend
                )
            } else {
                UserSlackRepository.sendMessage(
                    announcerUserId,
                    "Announcement was sent!",
                    withBlocks { section { markdownText("Announcement was sent!\nAnnouncement:\n") } } +
                            announcementToSend
                )
            }
        } else {
            UserSlackRepository.sendMessage(
                announcerUserId,
                "Announcements are not enabled",
            )
        }
    }
}
