package com.xmartlabs.slackbot.model

import kotlinx.serialization.Serializable

@Serializable
data class BambooEmployeeDirectoryResponse(val employees: List<BambooHrDirectoryUser>)
