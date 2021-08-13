package com.xmartlabs.slackbot.handlers

import com.slack.api.bolt.context.builtin.ViewSubmissionContext
import com.slack.api.bolt.handler.builtin.ViewSubmissionHandler
import com.slack.api.bolt.request.builtin.ViewSubmissionRequest
import com.slack.api.bolt.response.Response
import com.xmartlabs.slackbot.manager.TogglReportManager
import com.xmartlabs.slackbot.view.TogglReportViewCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TogglReportViewSubmissionHandler : ViewSubmissionHandler {
    override fun apply(req: ViewSubmissionRequest, ctx: ViewSubmissionContext): Response {
        val request = TogglReportViewCreator.getToggleReportRequestFromPayload(req.payload)
        GlobalScope.launch(Dispatchers.IO) {
            TogglReportManager.sendTimeReport(
                reportType = request.type,
                notifyUserId = req.payload.user.id,
                reportSort = request.sort,
                fromDate = request.from,
                toDate = request.to,
            )
        }
        return ctx.ack()
    }
}
