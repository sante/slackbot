package com.xmartlabs.slackbot.handlers

import com.slack.api.bolt.context.builtin.SlashCommandContext
import com.slack.api.bolt.handler.builtin.SlashCommandHandler
import com.slack.api.bolt.request.builtin.SlashCommandRequest
import com.slack.api.bolt.response.Response
import com.xmartlabs.slackbot.manager.TogglReportManager
import com.xmartlabs.slackbot.repositories.UserSlackRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDate

class TogglReportSlashCommandHandler : SlashCommandHandler {
    companion object {
        private const val GENERATING_MESSAGE = "Generating toggl report..."
        private const val NO_PERMISSION = "You have no permission to do that."
    }

    override fun apply(req: SlashCommandRequest, ctx: SlashCommandContext): Response =
        if (UserSlackRepository.hasAdminPrivileges(req.payload.userId)) {
            GlobalScope.launch {
                TogglReportManager.sendWeeklyWorkingTimeReport(req.payload.userId, LocalDate.now().minusWeeks(1))
            }
            ctx.ack(GENERATING_MESSAGE)
        } else {
            ctx.ack(NO_PERMISSION)
        }
}
