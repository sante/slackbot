package com.xmartlabs.slackbot.handlers

import com.slack.api.app_backend.events.payload.EventsApiPayload
import com.slack.api.bolt.context.builtin.EventContext
import com.slack.api.bolt.handler.BoltEventHandler
import com.slack.api.bolt.response.Response
import com.slack.api.model.event.MemberJoinedChannelEvent
import com.xmartlabs.slackbot.BOT_USER_ID
import com.xmartlabs.slackbot.MessageManager
import com.xmartlabs.slackbot.UserChannelRepository
import com.xmartlabs.slackbot.WELCOME_CHANNEL

class MemberJoinedChannelEventHandler : BoltEventHandler<MemberJoinedChannelEvent> {
    override fun apply(eventPayload: EventsApiPayload<MemberJoinedChannelEvent>, ctx: EventContext): Response {
        val event = eventPayload.event
        val user = UserChannelRepository.getUser(ctx, event.user)
        if (user?.isBot == true) {
            ctx.logger.info("Onboarding message ignored, ${user.name}:${event.user} is a bot user")
        } else {
            val channels = UserChannelRepository.getConversations(ctx)
            val channel = channels
                .firstOrNull { it.id == event.channel }
            ctx.logger.info("New member added to ${event.channel} - ${event.user}")
            if (channel?.name?.contains(WELCOME_CHANNEL, true) == true) {
                ctx.say {
                    it.channel(event.channel)
                        .text(MessageManager.getOngoardingMessage(BOT_USER_ID, listOf(event.user)))
                }
            }
        }
        return ctx.ack()
    }
}
