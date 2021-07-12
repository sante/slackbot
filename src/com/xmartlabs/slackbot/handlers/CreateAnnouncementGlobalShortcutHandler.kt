package com.xmartlabs.slackbot.handlers

import com.slack.api.bolt.context.builtin.GlobalShortcutContext
import com.slack.api.bolt.handler.builtin.GlobalShortcutHandler
import com.slack.api.bolt.request.builtin.GlobalShortcutRequest
import com.slack.api.bolt.response.Response
import com.xmartlabs.slackbot.view.AnnouncementViewCreator
import com.xmartlabs.slackbot.extensions.logIfError

class CreateAnnouncementGlobalShortcutHandler : GlobalShortcutHandler {
    override fun apply(req: GlobalShortcutRequest, ctx: GlobalShortcutContext): Response {
         ctx.client().viewsOpen {
            it.triggerId(ctx.triggerId)
                .view(AnnouncementViewCreator.createAnnouncementRequest())
        }.also { ctx.logger.logIfError(it) }
        return ctx.ack()
    }
}
