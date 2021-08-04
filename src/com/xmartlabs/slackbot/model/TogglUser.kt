package com.xmartlabs.slackbot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TogglUser(
    val active: Boolean?,
    val admin: Boolean?,
    val at: String?,
    @SerialName("avatar_file_name")
    val avatarFileName: String?,
    val email: String,
    @SerialName("group_ids")
    val groupIds: List<Int>?,
    val id: Long,
    val inactive: Boolean,
    @SerialName("invitation_code")
    val invitationCode: String?,
    @SerialName("invite_url")
    val inviteUrl: String?,
    @SerialName("is_direct")
    val isDirect: Boolean?,
    @SerialName("labour_cost")
    val labourCost: String?,
    val name: String?,
    val rate: String?,
    val timezone: String?,
    @SerialName("user_id")
    val userId: Long,
    @SerialName("workspace_id")
    val workspaceId: Int,
)
