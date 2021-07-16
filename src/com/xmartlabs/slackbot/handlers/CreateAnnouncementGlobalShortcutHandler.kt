package com.xmartlabs.slackbot.handlers

import com.slack.api.bolt.context.builtin.GlobalShortcutContext
import com.slack.api.bolt.handler.builtin.GlobalShortcutHandler
import com.slack.api.bolt.request.builtin.GlobalShortcutRequest
import com.slack.api.bolt.response.Response
import com.xmartlabs.slackbot.ANNOUNCEMENTS_PROTECTED_FEATURE
import com.xmartlabs.slackbot.USERS_WITH_ADMIN_PRIVILEGES
import com.xmartlabs.slackbot.UserChannelRepository
import com.xmartlabs.slackbot.extensions.logIfError
import com.xmartlabs.slackbot.view.AnnouncementViewCreator

class CreateAnnouncementGlobalShortcutHandler : GlobalShortcutHandler {
    override fun apply(req: GlobalShortcutRequest, ctx: GlobalShortcutContext): Response {
        val announcerUser = UserChannelRepository.getUser(ctx, req.payload.user.id)
        val view = if (ANNOUNCEMENTS_PROTECTED_FEATURE &&
            announcerUser?.isAdmin != true &&
            announcerUser?.id !in USERS_WITH_ADMIN_PRIVILEGES
        ) {
            AnnouncementViewCreator.createNoPermissionsView()
        } else {
            AnnouncementViewCreator.createAnnouncementRequest()
        }
        ctx.client().viewsOpen {
            it.triggerId(ctx.triggerId)
                .view(view)
        }.also { ctx.logger.logIfError(it) }
        return ctx.ack()
    }
}
