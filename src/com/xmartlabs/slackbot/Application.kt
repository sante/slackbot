package com.xmartlabs.slackbot

import com.slack.api.bolt.App
import com.slack.api.bolt.jetty.SlackAppServer
import com.slack.api.model.event.AppHomeOpenedEvent
import com.slack.api.model.event.MemberJoinedChannelEvent
import com.xmartlabs.slackbot.handlers.ActionCommandActionHandler
import com.xmartlabs.slackbot.handlers.AnnouncementConfirmationViewSubmissionHandler
import com.xmartlabs.slackbot.handlers.AnnouncementCreationRequestViewSubmissionHandler
import com.xmartlabs.slackbot.handlers.AppHomeOpenedEventEventHandler
import com.xmartlabs.slackbot.handlers.CreateAnnouncementGlobalShortcutHandler
import com.xmartlabs.slackbot.handlers.MemberJoinedChannelEventHandler
import com.xmartlabs.slackbot.handlers.OnboardingSlashCommandHandler
import com.xmartlabs.slackbot.handlers.ProcessXlBotHelpCommandCommandHandler
import com.xmartlabs.slackbot.handlers.TextCommandActionHandler
import com.xmartlabs.slackbot.handlers.TogglReportSlashCommandHandler
import com.xmartlabs.slackbot.handlers.TogglReportViewSubmissionHandler
import com.xmartlabs.slackbot.manager.CommandManager
import com.xmartlabs.slackbot.repositories.ConversationSlackRepository
import com.xmartlabs.slackbot.repositories.UserSlackRepository
import com.xmartlabs.slackbot.usecases.RemindInvalidEntryTogglUseCase
import com.xmartlabs.slackbot.view.AnnouncementViewCreator
import com.xmartlabs.slackbot.view.TogglReportViewCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("XlBotLogger")

fun main() {
    val app = App()
        .command("/xlbot", ProcessXlBotHelpCommandCommandHandler(visibleInChannel = false))
        .command("/xlbot-visible", ProcessXlBotHelpCommandCommandHandler(visibleInChannel = true))
        .command("/onboarding", OnboardingSlashCommandHandler())
        .command("/toggl-report", TogglReportSlashCommandHandler())

    handleMemberJoinedChannelEvent(app)
    handleAppOpenedEvent(app)
    handleViews(app)
    setupTooglReminders()
    prefetchData()
    val server = SlackAppServer(app, "/slack/events", Config.PORT)
    server.start() // http://localhost:3000/slack/events
}

fun prefetchData() {
    GlobalScope.launch(Dispatchers.IO) {
        kotlin.runCatching {
            ConversationSlackRepository.reloadCache()
            UserSlackRepository.reloadCache()
        }
            .onFailure { logger.error("Error preloading data", it) }
    }
}

fun setupTooglReminders() {
    if (Config.TOGGL_REPORTS_ENABLED) {
        GlobalScope.launch(Dispatchers.IO) {
            kotlin.runCatching { RemindInvalidEntryTogglUseCase().execute() }
                .onFailure { logger.error("Error sending toggl reminders", it) }
        }
    } else {
        logger.info("Toggl reports are not enabled")
    }
}

private fun handleViews(app: App) {
    // Handles global shortcut requests
    app.globalShortcut(
        AnnouncementViewCreator.CREATE_ANNOUNCEMENT_MODAL_CALLBACK_ID,
        CreateAnnouncementGlobalShortcutHandler()
    )
    app.viewSubmission(
        AnnouncementViewCreator.CREATE_ANNOUNCEMENT_REQUEST_CALLBACK_ID,
        AnnouncementCreationRequestViewSubmissionHandler()
    )
    app.viewSubmission(
        AnnouncementViewCreator.CREATE_ANNOUNCEMENT_REQUEST_CONFIRMATION_CALLBACK_ID,
        AnnouncementConfirmationViewSubmissionHandler()
    )
    app.blockAction(AnnouncementViewCreator.userFilterModeInputActionId) { _, req -> req.ack() }
    app.viewSubmission(
        TogglReportViewCreator.CREATE_TOGGL_REPORT_CALLBACK_ID,
        TogglReportViewSubmissionHandler()
    )
    TogglReportViewCreator.actionIds.forEach {
        app.blockAction(it) { _, req -> req.ack() }
    }
}

private fun handleAppOpenedEvent(app: App) {
    app.event(AppHomeOpenedEvent::class.java, AppHomeOpenedEventEventHandler())
    CommandManager.commands
        .filterIsInstance<TextCommand>()
        .forEach { command -> app.blockAction(command.buttonActionId, TextCommandActionHandler(command)) }
    CommandManager.adminCommands
        .filterIsInstance<ActionCommand>()
        .forEach { command -> app.blockAction(command.buttonActionId, ActionCommandActionHandler(command)) }
}

private fun handleMemberJoinedChannelEvent(app: App) =
    app.event(MemberJoinedChannelEvent::class.java, MemberJoinedChannelEventHandler())
