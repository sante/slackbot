package com.xmartlabs.slackbot.view

import com.slack.api.app_backend.views.payload.ViewSubmissionPayload
import com.slack.api.bolt.context.builtin.ViewSubmissionContext
import com.slack.api.model.Conversation
import com.slack.api.model.User
import com.slack.api.model.block.Blocks
import com.slack.api.model.block.element.StaticSelectElement
import com.slack.api.model.kotlin_extension.block.InputBlockBuilder
import com.slack.api.model.kotlin_extension.block.SectionBlockBuilder
import com.slack.api.model.kotlin_extension.block.withBlocks
import com.slack.api.model.kotlin_extension.view.blocks
import com.slack.api.model.view.View
import com.slack.api.model.view.ViewState
import com.slack.api.model.view.Views.view
import com.slack.api.model.view.Views.viewClose
import com.slack.api.model.view.Views.viewSubmit
import com.slack.api.model.view.Views.viewTitle
import com.xmartlabs.slackbot.extensions.getPlainTextValue
import com.xmartlabs.slackbot.extensions.getSelectedChannel
import com.xmartlabs.slackbot.extensions.getSelectedOptionValue
import com.xmartlabs.slackbot.extensions.getSelectedUsers
import com.xmartlabs.slackbot.model.DmAnnouncementRequest
import com.xmartlabs.slackbot.model.FilterMode
import com.xmartlabs.slackbot.model.ProcessedDmAnnouncementRequest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AnnouncementViewCreator {
    const val CREATE_ANNOUNCEMENT_MODAL_CALLBACK_ID = "request_announcement_id"
    const val CREATE_ANNOUNCEMENT_REQUEST_CALLBACK_ID = "create_announcement_request_id"
    const val CREATE_ANNOUNCEMENT_REQUEST_CONFIRMATION_CALLBACK_ID = "create_announcement_request_confirmation_id"
    const val NO_PERMISSION_CALLBACK_ID = "no_permission_id"

    private const val INCLUDE_USER_SELECT_VALUE = "include_users"
    private const val EXCLUDE_USER_SELECT_VALUE = "exclude_users"
    private const val TITLE_INPUT_MAX_LENGTH = 100
    private const val DETAILS_INPUT_MAX_LENGTH = 500

    private val titleInput = InputBlockBuilder().apply {
        blockId("title")
        label("Title")
        element {
            plainTextInput {
                actionId("title_action_id")
                maxLength(TITLE_INPUT_MAX_LENGTH)
            }
        }
    }.build()

    private val detailsInput = InputBlockBuilder().apply {
        blockId("details")
        label("Details")
        element {
            plainTextInput {
                actionId("details_action_id")
                multiline(true)
                maxLength(DETAILS_INPUT_MAX_LENGTH)
            }
        }
    }.build()

    private val userFilterModeInput = SectionBlockBuilder().apply {
        blockId("filter_user_mode_block_id")
        plainText("Select if you want to include or exclude some users to this announcement.")
        accessory {
            staticSelect {
                actionId("filter_user_mode_action_id")
                placeholder("Select mode")
                options {
                    option {
                        plainText("Include users")
                        value(INCLUDE_USER_SELECT_VALUE)
                    }
                    option {
                        plainText("Exclude users")
                        value(EXCLUDE_USER_SELECT_VALUE)
                    }
                }
            }
        }
    }
        .build()

    private val usersToFilterInput = InputBlockBuilder().apply {
        blockId("filter_users_block_id")
        label("Users to filter:")
        optional(true)
        element {
            multiUsersSelect {
                actionId("filter_users_action_id")
                placeholder("Select some users to be or not notified depending on the previous filter mode")
            }
        }
    }
        .build()

    private val channelSelectInput = InputBlockBuilder()
        .apply {
            blockId("filter_channel_block_id")
            label("Announce in channel:")
            optional(true)
            element {
                channelsSelect {
                    actionId("filter_channel_action_id")
                    placeholder("Select some users to be or not notified depending on the previous filter mode")
                }
            }
        }
        .build()

    val channelFilterBlockId: String
        get() = channelSelectInput.blockId
    val usersFilterBlockId: String
        get() = usersToFilterInput.blockId
    val userFilterModeInputActionId: String
        get() = (userFilterModeInput.accessory as StaticSelectElement).actionId

    fun createAnnouncementRequest(): View {
        return view { thisView ->
            thisView
                .callbackId(CREATE_ANNOUNCEMENT_REQUEST_CALLBACK_ID)
                .type("modal")
                .notifyOnClose(false)
                .title(viewTitle { it.type("plain_text").text("Create new Announcement").emoji(true) })
                .submit(viewSubmit { it.type("plain_text").text("Next").emoji(true) })
                .close(viewClose { it.type("plain_text").text("Cancel").emoji(true) })
                .blocks(listOf(
                    SectionBlockBuilder().apply { markdownText("Announcement data:") }.build(),
                    Blocks.divider(),
                    titleInput,
                    detailsInput,
                    Blocks.divider(),
                    SectionBlockBuilder().apply { markdownText("*Filter users:*") }.build(),
                    userFilterModeInput,
                    usersToFilterInput,
                    Blocks.divider(),
                    channelSelectInput
                ))
        }
    }

    fun createAnnouncementConfirmation(
        announcementRequest: DmAnnouncementRequest,
        usersToSend: List<User>,
        channelsToSend: List<Conversation>,
    ): View = view { thisView ->
        thisView
            .callbackId(CREATE_ANNOUNCEMENT_REQUEST_CONFIRMATION_CALLBACK_ID)
            .type("modal")
            .notifyOnClose(false)
            .title(viewTitle { it.type("plain_text").text("Confirm request").emoji(true) })
            .submit(viewSubmit { it.type("plain_text").text("Send").emoji(true) })
            .close(viewClose { it.type("plain_text").text("Cancel").emoji(true) })
            .privateMetadata(
                Json.encodeToString(ProcessedDmAnnouncementRequest(announcementRequest, usersToSend.map { it.id }))
            )
            .blocks(
                withBlocks {
                    section { markdownText("*Announcement Content:*") }
                    divider()
                } +
                        createAnnouncement(announcementRequest) +
                        withBlocks {
                            divider()
                            if (channelsToSend.isNotEmpty()) {
                                section {
                                    markdownText("*It will posted in:*\n <#${announcementRequest.announceInChannel}>")
                                }
                                divider()
                            }
                            if (usersToSend.isNotEmpty()) {
                                section {
                                    markdownText("*It will sent to:*\n " +
                                            usersToSend.joinToString(", ") { "<@${it.id}>" }
                                    )
                                }
                            }
                        }
            )
    }

    fun createNoPermissionsView(): View? = view { thisView ->
        thisView
            .callbackId(NO_PERMISSION_CALLBACK_ID)
            .type("modal")
            .notifyOnClose(false)
            .title(viewTitle { it.type("plain_text").text("Create Announcement").emoji(true) })
            .blocks { section { markdownText("Announcements is a restricted feature. :homer_sad:") } }
    }

    fun createAnnouncement(announcementRequest: DmAnnouncementRequest) = withBlocks {
        section {
            markdownText("*${announcementRequest.title}*")
        }
        divider()
        section {
            markdownText(announcementRequest.details)
        }
        context {
            elements {
                markdownText(text = ":memo: Posted by <@${announcementRequest.requester}>")
            }
        }
    }

    fun getDmAnnouncementRequestFromPayload(
        viewSubmissionPayload: ViewSubmissionPayload,
        ctx: ViewSubmissionContext,
    ): DmAnnouncementRequest {
        val values: MutableMap<String, MutableMap<String, ViewState.Value>> = viewSubmissionPayload.view.state.values
        val filterMode = if (values.getSelectedOptionValue(userFilterModeInput).equals(EXCLUDE_USER_SELECT_VALUE)) {
            FilterMode.EXCLUSIVE
        } else {
            FilterMode.INCLUSIVE
        }
        return DmAnnouncementRequest(
            title = values.getPlainTextValue(titleInput)!!,
            details = values.getPlainTextValue(detailsInput)!!,
            filterUsers = values.getSelectedUsers(usersToFilterInput),
            filterMode = filterMode,
            requester = ctx.requestUserId,
            announceInChannel = values.getSelectedChannel(channelSelectInput)
        )
    }

    fun getProcessedDmAnnouncementRequestFromPayload(viewSubmissionPayload: ViewSubmissionPayload) =
        Json.decodeFromString<ProcessedDmAnnouncementRequest>(viewSubmissionPayload.view.privateMetadata)
}
