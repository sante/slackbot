package com.xmartlabs.slackbot

import com.slack.api.app_backend.slash_commands.response.SlashCommandResponse
import com.slack.api.bolt.App
import com.slack.api.bolt.context.builtin.SlashCommandContext
import com.slack.api.bolt.jetty.SlackAppServer
import com.slack.api.bolt.request.builtin.SlashCommandRequest
import com.slack.api.bolt.response.Response
import com.slack.api.bolt.response.ResponseTypes
import com.slack.api.model.event.MemberJoinedChannelEvent

private val PROTECTED_CHANNELS_NAMES = listOf("general", "announcements")
private const val PROTECTED_CHANNEL_MESSAGE =
    "Hi :wave:\nPublic visible messages shouldn't be sent in protected channels"
private const val DEFAULT_PORT = 3000

private const val WELCOME_CHANNEL = "random"
const val XL_BOT_NAME = "xlbot2" // ID: U025KD1C28K

fun main(args: Array<String>) {
    val app = App()
        .command("/xlbot") { req, ctx -> processCommand(req, ctx) }
        .command("/slackhelp") { req, ctx -> processCommand(req, ctx, visibleInChannel = true) }
        .command("/onboarding") { req, ctx -> sendOnboardingCommand(req, ctx) }

    app.event(MemberJoinedChannelEvent::class.java) { eventPayload, ctx ->
        val event = eventPayload.event
        val channels = UserChannelRepository.getConversations(ctx)
        val channel = channels
            .firstOrNull { it.id == event.channel }
        ctx.logger.debug("New member added to ${event.channel} - ${event.user}")
        if (channel?.name?.contains(WELCOME_CHANNEL, true) == true) {
            ctx.say {
                it.channel(event.channel)
                    .text(MessageManager.getOngoardingMessage(XL_BOT_NAME, listOf(event.user)))
            }
        }
        ctx.ack()
    }

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
            .text(command.answer(req.payload, ctx))
            .responseType(ResponseTypes.inChannel)
            .build()
        ctx.ack(response)
    }

private fun processCommand(
    req: SlashCommandRequest,
    ctx: SlashCommandContext,
    visibleInChannel: Boolean = false,
) = ctx.ack(
    SlashCommandResponse.builder()
        .text(CommandManager.processCommand(ctx, req.payload))
        .responseType(if (visibleInChannel) ResponseTypes.inChannel else ResponseTypes.ephemeral)
        .build()
)
