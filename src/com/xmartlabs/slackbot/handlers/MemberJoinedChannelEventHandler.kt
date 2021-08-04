package com.xmartlabs.slackbot.handlers

import com.slack.api.app_backend.events.payload.EventsApiPayload
import com.slack.api.bolt.context.builtin.EventContext
import com.slack.api.bolt.handler.BoltEventHandler
import com.slack.api.bolt.response.Response
import com.slack.api.model.event.MemberJoinedChannelEvent
import com.xmartlabs.slackbot.Config
import com.xmartlabs.slackbot.manager.MessageManager
import com.xmartlabs.slackbot.repositories.ConversationSlackRepository
import com.xmartlabs.slackbot.repositories.UserSlackRepository

class MemberJoinedChannelEventHandler : BoltEventHandler<MemberJoinedChannelEvent> {
    override fun apply(eventPayload: EventsApiPayload<MemberJoinedChannelEvent>, ctx: EventContext): Response {
        val event = eventPayload.event
        val user = UserSlackRepository.getUser(event.user)
        if (user?.isBot == true) {
            ctx.logger.info("Onboarding message ignored, ${user.name}:${event.user} is a bot user")
        } else {
            val channels = ConversationSlackRepository.getRemoteEntities()
            val channel = channels
                .firstOrNull { it.id == event.channel }
            ctx.logger.info("New member added to ${event.channel} - ${event.user}")
            if (channel?.name?.contains(Config.WELCOME_CHANNEL, true) == true) {
                ctx.say {
                    it.channel(event.channel)
                        .text(MessageManager.getOngoardingMessage(Config.BOT_USER_ID, listOf(event.user)))
                }
            }
        }
        return ctx.ack()
    }
}
