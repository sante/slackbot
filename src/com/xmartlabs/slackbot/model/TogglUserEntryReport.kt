package com.xmartlabs.slackbot.model

import java.time.Duration

data class TogglUserEntryReport(
    val togglUser: TogglUser,
    val untrackedTime: Duration,
    val reportUrl: String,
)
