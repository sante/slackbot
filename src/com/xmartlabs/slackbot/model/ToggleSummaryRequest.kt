package com.xmartlabs.slackbot.model

import com.xmartlabs.slackbot.data.sources.serializer.TogglBambooDateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class ToggleSummaryRequest(
    @SerialName("start_date")
    @Serializable(with = TogglBambooDateSerializer::class)
    val startDate: LocalDate,
    @SerialName("end_date")
    @Serializable(with = TogglBambooDateSerializer::class)
    val endDate: LocalDate,
    @SerialName("grouping")
    val grouping: String = "users",
    @SerialName("sub_grouping")
    val subGrouping: String,
    @SerialName("include_time_entry_ids")
    val includeTimeEntryIds: Boolean = true,
)

enum class ToggleSummarySubGroupType(val serialName: String) {
    PROJECT("projects"),
    TIME_ENTRY("time_entries")
}
