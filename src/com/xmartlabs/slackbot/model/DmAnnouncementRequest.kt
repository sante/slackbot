package com.xmartlabs.slackbot.model

data class DmAnnouncementRequest(
    val title: String,
    val details: String,
    val filterMode: FilterMode,
    val filterUsers: List<String>?,
    val requester: String,
)

enum class FilterMode {
    INCLUSIVE,
    EXCLUSIVE,
}
