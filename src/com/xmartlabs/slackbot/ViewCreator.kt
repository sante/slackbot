package com.xmartlabs.slackbot

import com.slack.api.bolt.context.Context
import com.slack.api.model.kotlin_extension.view.blocks
import com.slack.api.model.view.View
import com.slack.api.model.view.Views.view

object ViewCreator {
    private const val NUMBER_OF_COLUMNS = 5

    fun createHomeView(
        ctx: Context,
        userId: String,
        selectedCommand: Command? = null,
        commandsWithAssociatedAction: List<Command> = CommandManager.commands,
    ): View = view { viewBuilder ->
        viewBuilder
            .type("home")
            .blocks {
                section {
                    markdownText(
                        """
                         Hi <@$userId>, I'm here to help you! :xl:
                         """.trimIndent()
                    )
                }
                divider()
                commandsWithAssociatedAction
                    .withIndex()
                    .groupBy { it.index / NUMBER_OF_COLUMNS }
                    .forEach { (_, rawCommands) ->
                        actions {
                            rawCommands
                                .forEach { (_, command) ->
                                    ctx.logger.debug("Adding button ${command.title}")
                                    button {
                                        actionId(command.buttonActionId)
                                        text(command.title, emoji = true)
                                        value(command.keys.first())
                                    }
                                }
                        }
                    }

                if (selectedCommand != null) {
                    divider()
                    section {
                        markdownText(
                            selectedCommand.answerText(null, ctx)
                        )
                    }
                }
            }
    }
}
