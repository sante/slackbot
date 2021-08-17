package com.xmartlabs.slackbot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ToggleSummaryResponse(
    val groups: List<ToggleSummaryGroup>,
)

@Serializable
data class ToggleSummaryGroup(
    val id: String,
    @SerialName("sub_groups")
    val subGroups: List<ToggleSummarySubGroup>,
)

@Serializable
data class ToggleSummarySubGroup(
    val id: String? = null,
    val title: String? = null,
    val seconds: Int?,
    val ids: List<Int>?,
)
