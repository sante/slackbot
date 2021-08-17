package com.xmartlabs.slackbot.model

import com.xmartlabs.slackbot.data.sources.serializer.TogglBambooDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class BambooTimeOff(
    val id: String,
    val type: String,
    val employeeId: String? = null,
    val name: String,
    @Serializable(with = TogglBambooDateSerializer::class)
    val start: LocalDate,
    @Serializable(with = TogglBambooDateSerializer::class)
    val end: LocalDate,
)
