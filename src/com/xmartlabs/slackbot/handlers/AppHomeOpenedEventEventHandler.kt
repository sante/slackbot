package com.xmartlabs.slackbot.handlers

import com.slack.api.app_backend.events.payload.EventsApiPayload
import com.slack.api.bolt.context.builtin.EventContext
import com.slack.api.bolt.handler.BoltEventHandler
import com.slack.api.bolt.response.Response
import com.slack.api.model.event.AppHomeOpenedEvent
import com.xmartlabs.slackbot.extensions.logIfError
import com.xmartlabs.slackbot.repositories.UserSlackRepository
import com.xmartlabs.slackbot.view.XlBotCommandsViewCreator

class AppHomeOpenedEventEventHandler : BoltEventHandler<AppHomeOpenedEvent> {
    override fun apply(eventPayload: EventsApiPayload<AppHomeOpenedEvent>, ctx: EventContext): Response {
        val event = eventPayload.event
        ctx.logger.info("User opened app's home, ${UserSlackRepository.getUser(event.user)?.name}")
        val appHomeView = XlBotCommandsViewCreator.createHomeView(
            ctx = ctx,
            userId = event.user,
            isAdmin = UserSlackRepository.hasAdminPrivileges(event.user),
            selectedCommand = null
        )

        // Update the App Home for the given user
        ctx.client().viewsPublish {
            it.userId(event.user)
                .hash(event.view?.hash) // To protect against possible race conditions
                .view(appHomeView)
        }?.also(ctx.logger::logIfError)
        return ctx.ack()
    }
}
