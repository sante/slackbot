package com.xmartlabs.slackbot.handlers

import com.slack.api.bolt.context.builtin.SlashCommandContext
import com.slack.api.bolt.handler.builtin.SlashCommandHandler
import com.slack.api.bolt.request.builtin.SlashCommandRequest
import com.slack.api.bolt.response.Response
import com.xmartlabs.slackbot.manager.CommandManager

class ProcessXlBotHelpCommandCommandHandler(private val visibleInChannel: Boolean) : SlashCommandHandler {
    override fun apply(req: SlashCommandRequest, ctx: SlashCommandContext): Response {
        ctx.logger.info("User request command, ${req.payload?.userName} - ${req.payload?.text}")
        return ctx.ack(CommandManager.processTextCommand(ctx, req.payload, visibleInChannel))
    }
}
