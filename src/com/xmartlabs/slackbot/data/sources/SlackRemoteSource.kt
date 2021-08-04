package com.xmartlabs.slackbot.data.sources

import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.xmartlabs.slackbot.Config
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("UnnecessaryAbstractClass")
abstract class SlackRemoteSource<T> {
    companion object {
        const val PAGE_LIMIT = 1000

        @JvmStatic
        protected val slackMethods: MethodsClient = Slack.getInstance().methods(Config.SLACK_TOKEN)

        @JvmStatic
        protected val defaultLogger: Logger = LoggerFactory.getLogger(SlackRemoteSource::class.java)
    }

    abstract fun getRemoteEntities(): List<T>
}
