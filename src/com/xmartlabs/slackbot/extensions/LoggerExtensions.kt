package com.xmartlabs.slackbot.extensions

import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.methods.response.views.ViewsOpenResponse
import com.slack.api.methods.response.views.ViewsPublishResponse
import org.slf4j.Logger

inline fun Logger.logIfError(
    response: ViewsPublishResponse,
    message: () -> String = { "Update home error: $response" },
) {
    if (!response.isOk) warn(message())
}

inline fun Logger.logIfError(
    response: ChatPostMessageResponse,
    message: () -> String = { "Error posting message: $response" },
) {
    if (!response.isOk) warn(message())
}

inline fun Logger.logIfError(
    response: ViewsOpenResponse,
    message: () -> String = { "Error opening a modal view: $response" },
) {
    if (!response.isOk) warn(message())
}
