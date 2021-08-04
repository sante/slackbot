package com.xmartlabs.slackbot

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.OffsetTime

@Suppress("MagicNumber")
object Config {
    val PROTECTED_CHANNELS_NAMES = listOf("general", "announcements")

    val SLACK_TOKEN: String = System.getenv("SLACK_BOT_TOKEN")

    val PORT = System.getenv("PORT")?.toIntOrNull() ?: 3000
    val BOT_USER_ID = System.getenv("BOT_USER_ID") ?: "U025KD1C28K"
    val XL_PASSWORD = System.getenv("XL_PASSWORD") ?: "*********"
    val XL_GUEST_PASSWORD = System.getenv("XL_GUEST_PASSWORD") ?: "*********"
    const val ACTION_VALUE_VISIBLE = "visible-in-channel"

    val USERS_WITH_ADMIN_PRIVILEGES =
        System.getenv("USERS_WITH_ADMIN_PRIVILEGES")?.split(",") ?: emptyList()
    val ANNOUNCEMENTS_ENABLED = System.getenv("ANNOUNCEMENTS_ENABLED")?.toBoolean() ?: false
    val ANNOUNCEMENTS_PROTECTED_FEATURE = System.getenv("ANNOUNCEMENTS_PROTECTED_FEATURE")?.toBoolean() ?: true

    val WELCOME_CHANNEL = System.getenv("WELCOME_CHANNEL_NAME") ?: "random"

    val TOGGL_REPORTS_ENABLED = System.getenv("TOGGL_REPORTS_ENABLED")?.toBoolean() ?: false
    val TOGGL_XL_ORGANIZATION = System.getenv("TOGGL_XL_ORGANIZATION")?.toLong() ?: -1
    val TOGGL_XL_WORKSPACE = System.getenv("TOGGL_XL_WORKSPACE")?.toLong() ?: -1
    val TOGGL_API_KEY = System.getenv("TOGGL_API_KEY") ?: ""
    val TOGGL_USER_AGENT = System.getenv("TOGGL_USER_AGENT") ?: ""
    val TOGGL_REPORTS_SLACK_CHANNEL_ID = System.getenv("TOGGL_REPORTS_SLACK_CHANNEL_ID") ?: ""

    // MONDAY, WEDNESDAY, FRIDAY
    val TOGGL_DAYS_TO_REMIND = (System.getenv("TOGGL_DAYS_TO_REMIND") ?: "2,3")
        .split(",")
        .map(String::toInt)
        .map(DayOfWeek::of)
    val TOGGL_TIME_TO_REMIND: LocalTime = (System.getenv("TOGGL_TIME_TO_REMIND") ?: "11:00:00-03:00")
        .let { OffsetTime.parse(it).toLocalTime() }
    val TOGGL_EXCLUDE_ACTIVE_ENTRIES = System.getenv("TOGGL_EXCLUDE_ACTIVE_ENTRIES")?.toBoolean() ?: true
    val TOGGL_WEEKLY_REPORT_DAYS_TO_CHECK = System.getenv("TOGGL_WEEKLY_REPORT_DAYS_TO_CHECK")?.toInt() ?: 7
    val TOGGLE_WEEKLY_REPORT_EXCLUDE_CURRENT_WEEK =
        System.getenv("TOGGLE_WEEKLY_REPORT_EXCLUDE_CURRENT_WEEK")?.toBoolean() ?: true
    val TOGGL_WEEKLY_REPORT_MIN_UNTRACKED_DURATION_TO_NOTIFY: Duration =
        (System.getenv("TOGGL_MIN_WEEKLY_UNTRACKED_DURATION_TO_NOTIFY") ?: "00:10:00")
            .let { minDurationTime ->
                Duration.between(
                    LocalTime.MIN,
                    LocalTime.parse(minDurationTime)
                )
            }

    init {
        // Check slack keys
        requireNotNull(System.getenv("SLACK_BOT_TOKEN")) {
            "SLACK_BOT_TOKEN is missing"
        }
        requireNotNull(System.getenv("SLACK_SIGNING_SECRET")) {
            "SLACK_SIGNING_SECRET is missing"
        }
    }
}
