package com.xmartlabs.slackbot.data.sources

import com.xmartlabs.slackbot.Config
import io.ktor.client.HttpClient
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.providers.BasicAuthCredentials
import io.ktor.client.features.auth.providers.basic
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.Logging
import io.rocketbase.toggl.api.TogglReportApiBuilder
import kotlinx.serialization.json.Json as JsonBuilder

object TogglApi {
    val togglReportApi = TogglReportApiBuilder()
        .apiToken(Config.TOGGL_API_KEY)
        .userAgent(Config.TOGGL_USER_AGENT)
        .workspaceId(Config.TOGGL_XL_WORKSPACE)
        .build()

    val client = HttpClient {
        install(Auth) {
            basic {
                sendWithoutRequest { true }
                credentials {
                    BasicAuthCredentials(
                        username = Config.TOGGL_API_KEY,
                        password = "api_token"
                    )
                }
            }
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer(JsonBuilder {
                prettyPrint = true
                isLenient = true
            })
        }
        install(Logging)
    }
}
