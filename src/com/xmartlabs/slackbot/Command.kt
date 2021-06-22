package com.xmartlabs.slackbot

import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload
import com.slack.api.bolt.context.Context

class Command(vararg val keys: String, val answer: (SlashCommandPayload?, ctx: Context) -> String)
