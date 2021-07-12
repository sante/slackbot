package com.xmartlabs.slackbot.view

import com.slack.api.app_backend.views.payload.ViewSubmissionPayload
import com.slack.api.bolt.context.builtin.ViewSubmissionContext
import com.slack.api.model.block.Blocks
import com.slack.api.model.kotlin_extension.block.InputBlockBuilder
import com.slack.api.model.kotlin_extension.block.SectionBlockBuilder
import com.slack.api.model.kotlin_extension.block.withBlocks
import com.slack.api.model.view.View
import com.slack.api.model.view.ViewState
import com.slack.api.model.view.Views.view
import com.slack.api.model.view.Views.viewClose
import com.slack.api.model.view.Views.viewSubmit
import com.slack.api.model.view.Views.viewTitle
import com.xmartlabs.slackbot.extensions.getPlainTextValue
import com.xmartlabs.slackbot.extensions.getSelectedOptionValue
import com.xmartlabs.slackbot.extensions.getSelectedUsers
import com.xmartlabs.slackbot.model.DmAnnouncementRequest
import com.xmartlabs.slackbot.model.FilterMode

object AnnouncementViewCreator {
    const val CREATE_ANNOUNCEMENT_MODAL_CALLBACK_ID = "request_announcement_id"
    const val CREATE_ANNOUNCEMENT_REQUEST_CALLBACK_ID = "create_announcement_request_id"

    private const val INCLUDE_USER_SELECT_VALUE = "include_users"
    private const val EXCLUDE_USER_SELECT_VALUE = "exclude_users"
    private const val TITLE_INPUT_MAX_LENGTH = 100
    private const val DETAILS_INPUT_MAX_LENGTH = 500

    private val titleInput = InputBlockBuilder().apply {
        blockId("title")
        label("Title")
        element {
            plainTextInput {
                actionId("title_id")
                maxLength(TITLE_INPUT_MAX_LENGTH)
            }
        }
    }.build()

    private val detailsInput = InputBlockBuilder().apply {
        blockId("details")
        label("Details")
        element {
            plainTextInput {
                actionId("details_id")
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
                actionId("filter_users_id")
                placeholder("Select some users to be or not notified depending on the previous filter mode")
            }
        }
    }
        .build()

    val usersToFilterBlockId: String
        get() = usersToFilterInput.blockId

    fun createAnnouncementRequest(): View {
        return view { thisView ->
            thisView
                .callbackId(CREATE_ANNOUNCEMENT_REQUEST_CALLBACK_ID)
                .type("modal")
                .notifyOnClose(true)
                .title(viewTitle { it.type("plain_text").text("Create new Announcement").emoji(true) })
                .submit(viewSubmit { it.type("plain_text").text("Request").emoji(true) })
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
                ))
        }
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

    fun fromPayload(viewSubmissionPayload: ViewSubmissionPayload, ctx: ViewSubmissionContext): DmAnnouncementRequest {
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
        )
    }
}
