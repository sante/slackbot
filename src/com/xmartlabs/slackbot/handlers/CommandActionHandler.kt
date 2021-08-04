package com.xmartlabs.slackbot.handlers

import com.slack.api.app_backend.interactive_components.response.ActionResponse
import com.slack.api.bolt.context.builtin.ActionContext
import com.slack.api.bolt.handler.builtin.BlockActionHandler
import com.slack.api.bolt.request.builtin.BlockActionRequest
import com.slack.api.bolt.response.Response
import com.slack.api.bolt.response.ResponseTypes
import com.xmartlabs.slackbot.Config
import com.xmartlabs.slackbot.Command
import com.xmartlabs.slackbot.manager.CommandManager
import com.xmartlabs.slackbot.view.XlBotCommandsViewCreator
import com.xmartlabs.slackbot.extensions.logIfError

class CommandActionHandler(private val command: Command) : BlockActionHandler {
    override fun apply(req: BlockActionRequest, ctx: ActionContext): Response {
        val visibleInChannel =
            Config.ACTION_VALUE_VISIBLE.equals(req.payload.actions?.get(0)?.value, ignoreCase = true)
        if (req.payload.responseUrl != null) {
            // Post a message to the same channel if it's a block in a message
            ctx.respond(
                ActionResponse.builder()
                    .text(command.answerText(null, ctx))
                    .responseType(if (visibleInChannel) ResponseTypes.inChannel else ResponseTypes.ephemeral)
                    // It's deleted because the visibility can be changed
                    .also { it.deleteOriginal(visibleInChannel) }
                    .also { it.replaceOriginal(!visibleInChannel) }
                    .build()
            )
        } else {
            val user = req.payload.user.id
            val appHomeView = XlBotCommandsViewCreator.createHomeView(
                ctx = ctx,
                userId = user,
                commandsWithAssociatedAction = CommandManager.commands,
                selectedCommand = command
            )
            // Update the App Home for the given user
            ctx.client().viewsPublish {
                it.userId(user)
                    .hash(req.payload.view?.hash) // To protect against possible race conditions
                    .view(appHomeView)
            }?.also(ctx.logger::logIfError)
        }
        return ctx.ack()
    }
}
