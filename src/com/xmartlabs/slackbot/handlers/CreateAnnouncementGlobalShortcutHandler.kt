package com.xmartlabs.slackbot.handlers

import com.slack.api.bolt.context.builtin.GlobalShortcutContext
import com.slack.api.bolt.handler.builtin.GlobalShortcutHandler
import com.slack.api.bolt.request.builtin.GlobalShortcutRequest
import com.slack.api.bolt.response.Response
import com.xmartlabs.slackbot.Config
import com.xmartlabs.slackbot.extensions.logIfError
import com.xmartlabs.slackbot.repositories.UserSlackRepository
import com.xmartlabs.slackbot.view.AnnouncementViewCreator

class CreateAnnouncementGlobalShortcutHandler : GlobalShortcutHandler {
    override fun apply(req: GlobalShortcutRequest, ctx: GlobalShortcutContext): Response {
        val announcerUser = UserSlackRepository.getUser(req.payload.user.id)
        val view = if (Config.ANNOUNCEMENTS_PROTECTED_FEATURE &&
            announcerUser?.isAdmin != true &&
            announcerUser?.id !in Config.USERS_WITH_ADMIN_PRIVILEGES
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
