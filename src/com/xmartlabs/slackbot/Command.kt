package com.xmartlabs.slackbot

import com.slack.api.app_backend.slash_commands.response.SlashCommandResponse
import com.slack.api.bolt.context.Context
import com.slack.api.bolt.response.ResponseTypes
import java.util.Locale

class Command(
    vararg val keys: String,
    val title: String = capitalizeFirstLetter(keys) ?: "",
    val description: String? = null,
    val visible: Boolean = true,
    val answerResponse: (command: String?, ctx: Context, visibleInChannel: Boolean) -> SlashCommandResponse =
        { text, ctx, visibleInChannel ->
            SlashCommandResponse.builder().text(answerText(text, ctx))
                .responseType(if (visibleInChannel) ResponseTypes.inChannel else ResponseTypes.ephemeral)
                .build()
        },
    val answerText: (command: String?, ctx: Context) -> String,
)

val Command.buttonActionId
    get() = "button-action-${keys.firstOrNull()}"

private fun capitalizeFirstLetter(keys: Array<out String>) = keys.firstOrNull()
    ?.replaceFirstChar { key -> if (key.isLowerCase()) key.titlecase(Locale.getDefault()) else key.toString() }
