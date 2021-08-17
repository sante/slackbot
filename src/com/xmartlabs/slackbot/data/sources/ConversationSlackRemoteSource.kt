package com.xmartlabs.slackbot.data.sources

import com.slack.api.methods.request.conversations.ConversationsListRequest
import com.slack.api.model.Conversation
import com.slack.api.model.ConversationType
import com.slack.api.model.block.LayoutBlock
import com.xmartlabs.slackbot.extensions.logIfError
import com.xmartlabs.slackbot.extensions.withLockusingStabilizationDelay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object ConversationSlackRemoteSource : SlackRemoteSource<Conversation>() {
    @Suppress("MagicNumber")
    private val NOTIFY_DELAY_IN_MILLIS = Duration.milliseconds(500)
    private val sentMessageMutex = Mutex()

    override fun getRemoteEntities(): List<Conversation> {
        val channels = mutableListOf<Conversation>()
        var cursor: String? = null
        do {
            val req = ConversationsListRequest.builder()
                .limit(PAGE_LIMIT)
                .types(listOf(ConversationType.PUBLIC_CHANNEL))
                .let { if (cursor != null) it.cursor(cursor).build() else it.build() }
            val listResponse = slackMethods.conversationsList(req)
            if (!listResponse.isOk) {
                throw IllegalStateException("Error getting remote channels. Error: ${listResponse.error}")
            }
            cursor = listResponse?.responseMetadata?.nextCursor
            channels += listResponse?.channels ?: listOf()
        } while (!cursor.isNullOrBlank())
        defaultLogger.info("Remote channels fetched")
        defaultLogger.debug("Remote channels: " + channels.joinToString { "${it.name} - ${it.id}" })
        return channels
    }

    suspend fun sendMessage(channelId: String, text: String, blocks: List<LayoutBlock>? = null): Boolean =
        sentMessageMutex.withLockusingStabilizationDelay(NOTIFY_DELAY_IN_MILLIS) {
            withContext(Dispatchers.IO) {
                val response = slackMethods.chatPostMessage { req ->
                    req.channel(channelId)
                        .blocks(blocks)
                        .text(text)
                }
                if (response.isOk) {
                    defaultLogger.info("Send to: $channelId")
                    true
                } else {
                    defaultLogger.logIfError(response) {
                        "Error sending announcement to $channelId $response"
                    }
                    false
                }
            }
        }

    suspend fun sendCsvFile(channelId: String, fileName: String, title: String, content: String): Boolean =
        sentMessageMutex.withLockusingStabilizationDelay(NOTIFY_DELAY_IN_MILLIS) {
            withContext(Dispatchers.IO) {
                val response = slackMethods.filesUpload { req ->
                    req.content(content)
                        .title(title)
                        .filename("$fileName.csv")
                        .filetype("csv")
                        .channels(listOf(channelId))
                }
                if (response.isOk) {
                    defaultLogger.info("Send to: $channelId")
                    true
                } else {
                    defaultLogger.logIfError(response) {
                        "Error sending file to $channelId $response"
                    }
                    false
                }
            }
        }
}
