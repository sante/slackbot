package com.xmartlabs.slackbot.handlers

import com.slack.api.bolt.context.builtin.ActionContext
import com.slack.api.bolt.handler.builtin.BlockActionHandler
import com.slack.api.bolt.request.builtin.BlockActionRequest
import com.slack.api.bolt.response.Response
import com.xmartlabs.slackbot.ActionCommand
import com.xmartlabs.slackbot.manager.CommandManager
import com.xmartlabs.slackbot.view.AnnouncementViewCreator
import com.xmartlabs.slackbot.view.TogglReportViewCreator

class ActionCommandActionHandler(private val command: ActionCommand) : BlockActionHandler {
    override fun apply(req: BlockActionRequest, ctx: ActionContext): Response {
        ctx.client().viewsOpen { openViewRequest ->
            openViewRequest
                .triggerId(ctx.triggerId)
                .view(
                    when (command) {
                        CommandManager.announcementCommand -> AnnouncementViewCreator.createAnnouncementRequest()
                        CommandManager.toggleReportCommand -> TogglReportViewCreator.createTogglReportView()
                        else -> throw IllegalStateException("Command is not valid $command")
                    }
                )
        }
        return ctx.ack()
    }
}
