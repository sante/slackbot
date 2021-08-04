package com.xmartlabs.slackbot.handlers

import com.slack.api.app_backend.slash_commands.response.SlashCommandResponse
import com.slack.api.bolt.context.builtin.SlashCommandContext
import com.slack.api.bolt.handler.builtin.SlashCommandHandler
import com.slack.api.bolt.request.builtin.SlashCommandRequest
import com.slack.api.bolt.response.Response
import com.slack.api.bolt.response.ResponseTypes
import com.xmartlabs.slackbot.manager.CommandManager
import com.xmartlabs.slackbot.Config

class OnboardingSlashCommandHandler : SlashCommandHandler {
    companion object {
        private const val PROTECTED_CHANNEL_MESSAGE =
            "Hi :wave:\nPublic visible messages shouldn't be sent in protected channels"
    }

    override fun apply(req: SlashCommandRequest, ctx: SlashCommandContext): Response =
        if (req.payload.channelName in Config.PROTECTED_CHANNELS_NAMES) {
            ctx.ack(PROTECTED_CHANNEL_MESSAGE)
        } else {
            val command = CommandManager.onboarding
            val response = SlashCommandResponse.builder()
                .text(command.answerText(req.payload?.text, ctx))
                .responseType(ResponseTypes.inChannel)
                .build()
            ctx.ack(response)
        }
}
