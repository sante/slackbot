package com.xmartlabs.slackbot.model

import java.time.Duration

data class BambooWorkTimeReport(
    val bambooUser: BambooUser,
    val workingTime: Duration,
)
