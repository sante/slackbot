package com.xmartlabs.slackbot

enum class Command(val key: String) {
    WIFI_PASS("wifi pass"),
    TOGGL("toggl"),
    RECYCLING("recycling"),
    ANNIVERSARY("anniversary"),
    CALENDAR_URL("calendar"),
    SETUP_PROCESS("setup"),
    LIGHTNING("lightning"),
    BALA("bala"),
    ;

    companion object {
        fun fromKey(key: String?) =
            if (key.isNullOrBlank()) null else values().firstOrNull { key.contains(it.key, true) }
    }
}
