package com.xmartlabs.slackbot.handlers

import com.slack.api.bolt.context.builtin.SlashCommandContext
import com.slack.api.bolt.handler.builtin.SlashCommandHandler
import com.slack.api.bolt.request.builtin.SlashCommandRequest
import com.slack.api.bolt.response.Response
import com.xmartlabs.slackbot.repositories.UserSlackRepository
import com.xmartlabs.slackbot.view.TogglReportViewCreator

class TogglReportSlashCommandHandler : SlashCommandHandler {
    companion object {
        private const val NO_PERMISSION_MESSAGE = "You have no permission to do that."
    }

    override fun apply(req: SlashCommandRequest, ctx: SlashCommandContext): Response =
        if (UserSlackRepository.hasAdminPrivileges(req.payload.userId)) {
            ctx.client().viewsOpen { request ->
                request
                    .triggerId(ctx.triggerId)
                    .view(TogglReportViewCreator.createTogglReportView())
            }
            ctx.ack()
        } else {
            ctx.ack(NO_PERMISSION_MESSAGE)
        }
}
