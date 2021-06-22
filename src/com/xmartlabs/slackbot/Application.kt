package com.xmartlabs.slackbot

import com.slack.api.app_backend.slash_commands.response.SlashCommandResponse
import com.slack.api.bolt.App
import com.slack.api.bolt.context.builtin.SlashCommandContext
import com.slack.api.bolt.jetty.SlackAppServer
import com.slack.api.bolt.request.builtin.SlashCommandRequest
import com.slack.api.bolt.response.Response
import com.slack.api.bolt.response.ResponseTypes

private val PROTECTED_CHANNELS_NAMES = listOf("general", "announcements")
private const val PROTECTED_CHANNEL_MESSAGE =
    "Hi :wave:\nPublic visible messages shouldn't be sent in protected channels"
private const val DEFAULT_PORT = 3000

fun main(args: Array<String>) {
    val app = App()
        .command("/xlbot") { req, ctx -> processCommand(req, ctx) }
        .command("/slackhelp") { req, ctx -> processCommand(req, ctx) }
        .command("/onboarding") { req, ctx -> sendOnboardingCommand(req, ctx) }

    val port = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT

    val server = SlackAppServer(app, "/slack/events", port)
    server.start() // http://localhost:3000/slack/events
}

fun sendOnboardingCommand(req: SlashCommandRequest, ctx: SlashCommandContext): Response =
    if (req.payload.channelName in PROTECTED_CHANNELS_NAMES) {
        ctx.ack(PROTECTED_CHANNEL_MESSAGE)
    } else {
        val command = CommandManager.onboarding
        val response = SlashCommandResponse.builder()
            .text(command.answer)
            .responseType(ResponseTypes.inChannel)
            .build()
        ctx.ack(response)
    }

private fun processCommand(
    req: SlashCommandRequest,
    ctx: SlashCommandContext
): Response? {
    val command = CommandManager.provideCommand(req.payload?.text)
    return ctx.ack(command.answer)
}
