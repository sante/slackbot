package com.xmartlabs.slackbot.model

import kotlinx.serialization.Serializable

@Serializable
data class DmAnnouncementRequest(
    val title: String,
    val details: String,
    val filterMode: FilterMode,
    val filterUsers: List<String>?,
    val announceInChannel: String?,
    val requester: String,
)

@Serializable
data class ProcessedDmAnnouncementRequest(
    val dmAnnouncementRequest: DmAnnouncementRequest,
    val usersToSend: List<String>,
)

@Serializable
enum class FilterMode {
    INCLUSIVE,
    EXCLUSIVE,
}
