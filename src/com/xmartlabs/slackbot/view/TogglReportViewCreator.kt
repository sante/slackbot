package com.xmartlabs.slackbot.view

import com.slack.api.app_backend.views.payload.ViewSubmissionPayload
import com.slack.api.model.kotlin_extension.block.dsl.LayoutBlockDsl
import com.slack.api.model.kotlin_extension.block.withBlocks
import com.slack.api.model.view.View
import com.slack.api.model.view.ViewState
import com.slack.api.model.view.Views.view
import com.slack.api.model.view.Views.viewClose
import com.slack.api.model.view.Views.viewSubmit
import com.slack.api.model.view.Views.viewTitle
import com.xmartlabs.slackbot.extensions.formatUsingSlackFormatter
import com.xmartlabs.slackbot.extensions.getSelectedDate
import com.xmartlabs.slackbot.extensions.getSelectedOptionValue
import com.xmartlabs.slackbot.manager.ReportSort
import com.xmartlabs.slackbot.manager.ReportType
import com.xmartlabs.slackbot.model.ToggleReportRequest
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object TogglReportViewCreator {
    const val CREATE_TOGGL_REPORT_CALLBACK_ID = "CREATE_TOGGL_REPORT_CALLBACK_ID"

    private const val DATE_PICKER_BLOCK_ID = "DATE_PICKER_BLOCK_ID"
    private const val DATE_PICKER_START_DATE_ACTION_ID = "DATE_PICKER_START_DATE_ACTION_ID"
    private const val DATE_PICKER_END_DATE_ACTION_ID = "DATE_PICKER_END_DATE_ACTION_ID"

    private const val REPORT_TYPE_BLOCK_ID = "REPORT_TYPE_BLOCK_ID"
    private const val REPORT_TYPE_ACTION_ID = "REPORT_TYPE_ACTION_ID"

    private const val SORT_TYPE_BLOCK_ID = "SORT_TYPE_BLOCK_ID"
    private const val SORT_TYPE_ACTION_ID = "SORT_TYPE_ACTION_ID"

    val actionIds = listOf(
        DATE_PICKER_START_DATE_ACTION_ID,
        DATE_PICKER_END_DATE_ACTION_ID,
        REPORT_TYPE_ACTION_ID,
        SORT_TYPE_ACTION_ID,
    )

    fun createTogglReportView(): View = view { thisView ->
        val from = LocalDate.now().minusWeeks(1)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val to = from.plusWeeks(1)
            .minusDays(1)
        thisView
            .callbackId(CREATE_TOGGL_REPORT_CALLBACK_ID)
            .type("modal")
            .notifyOnClose(false)
            .title(viewTitle { it.type("plain_text").text("Generate Toggl report:").emoji(true) })
            .submit(viewSubmit { it.type("plain_text").text("Generate").emoji(true) })
            .close(viewClose { it.type("plain_text").text("Cancel").emoji(true) })
            .blocks(
                withBlocks {
                    datePickerActions(from, to)
                    sectionTypeSelector()
                    sortSelector()
                }
            )
    }

    private fun LayoutBlockDsl.sortSelector() {
        section {
            blockId(SORT_TYPE_BLOCK_ID)
            plainText("Select order")
            accessory {
                staticSelect {
                    actionId(SORT_TYPE_ACTION_ID)
                    initialOption {
                        plainText("Alphabetical")
                        value(ReportSort.ALPHA.name)
                    }
                    options {
                        option {
                            plainText("Alphabetical")
                            value(ReportSort.ALPHA.name)
                        }
                        option {
                            plainText("Time")
                            value(ReportSort.TIME.name)
                        }
                    }
                }
            }
        }
    }

    private fun LayoutBlockDsl.sectionTypeSelector() {
        section {
            blockId(REPORT_TYPE_BLOCK_ID)
            plainText("Select type")
            accessory {
                staticSelect {
                    actionId(REPORT_TYPE_ACTION_ID)
                    initialOption {
                        plainText("Worked hours report")
                        value(ReportType.WORKED_HOURS.name)
                    }
                    options {
                        option {
                            plainText("Worked hours report")
                            value(ReportType.WORKED_HOURS.name)
                        }
                        option {
                            plainText("Invalid entries report")
                            value(ReportType.WRONG_TRACKED_ENTRIES_TIME.name)
                        }
                    }
                }
            }
        }
    }

    private fun LayoutBlockDsl.datePickerActions(
        from: LocalDate,
        to: LocalDate,
    ) {
        actions {
            blockId(DATE_PICKER_BLOCK_ID)
            elements {
                datePicker {
                    initialDate(from.formatUsingSlackFormatter())
                    placeholder("Start date:")
                    actionId(DATE_PICKER_START_DATE_ACTION_ID)
                }
            }
            elements {
                datePicker {
                    initialDate(to.formatUsingSlackFormatter())
                    placeholder("End date:")
                    actionId(DATE_PICKER_END_DATE_ACTION_ID)
                }
            }
        }
    }

    fun getToggleReportRequestFromPayload(
        viewSubmissionPayload: ViewSubmissionPayload,
    ): ToggleReportRequest {
        val values: MutableMap<String, MutableMap<String, ViewState.Value>> = viewSubmissionPayload.view.state.values
        val selectedDate = values.getSelectedDate(DATE_PICKER_BLOCK_ID, DATE_PICKER_START_DATE_ACTION_ID)
        return ToggleReportRequest(
            from = LocalDate.parse(selectedDate!!),
            to = LocalDate.parse(values.getSelectedDate(DATE_PICKER_BLOCK_ID, DATE_PICKER_END_DATE_ACTION_ID)!!),
            sort = ReportSort.valueOf(values.getSelectedOptionValue(SORT_TYPE_BLOCK_ID, SORT_TYPE_ACTION_ID)!!),
            type = ReportType.valueOf(values.getSelectedOptionValue(REPORT_TYPE_BLOCK_ID, REPORT_TYPE_ACTION_ID)!!),
        )
    }
}
