package com.xmartlabs.slackbot

import com.slack.api.app_backend.slash_commands.response.SlashCommandResponse
import com.slack.api.bolt.context.Context
import com.slack.api.bolt.response.ResponseTypes
import java.util.Locale

interface Command {
    val mainKey: String
    val title: String
    val description: String?
    val visible: Boolean
}

class TextCommand(
    vararg val keys: String,
    override val title: String = capitalizeFirstLetter(keys) ?: "",
    override val description: String? = null,
    override val visible: Boolean = true,
    val answerResponse: (command: String?, ctx: Context, visibleInChannel: Boolean) -> SlashCommandResponse =
        { text, ctx, visibleInChannel ->
            SlashCommandResponse.builder().text(answerText(text, ctx))
                .responseType(if (visibleInChannel) ResponseTypes.inChannel else ResponseTypes.ephemeral)
                .build()
        },
    val answerText: (command: String?, ctx: Context) -> String,
) : Command {
    override val mainKey: String
        get() = keys.first()

    override fun equals(other: Any?) = (other is TextCommand) && keys.contentEquals(other.keys)

    override fun hashCode(): Int = keys.hashCode()
}

data class ActionCommand(
    override val mainKey: String,
    override val title: String,
    override val description: String?,
    override val visible: Boolean,
) : Command

val Command.buttonActionId
    get() = "button-action-$mainKey"

private fun capitalizeFirstLetter(keys: Array<out String>) = keys.firstOrNull()
    ?.replaceFirstChar { key -> if (key.isLowerCase()) key.titlecase(Locale.getDefault()) else key.toString() }
