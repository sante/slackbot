package com.xmartlabs.slackbot.model

import com.xmartlabs.slackbot.data.sources.serializer.TogglBambooDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

data class BambooUser(
    val id: String,
    val displayName: String,
    val workEmail: String,
    val workingHours: Int,
    val hireDate: LocalDate,
)

@Serializable
data class BambooHrDirectoryUser(
    val id: String,
    val displayName: String,
    val workEmail: String?,
)

@Serializable
data class BambooHrUserCustomFields(
    val customWorkingHours: Int? = null,
    @Serializable(with = TogglBambooDateSerializer::class)
    val hireDate: LocalDate = LocalDate.MIN,
)
