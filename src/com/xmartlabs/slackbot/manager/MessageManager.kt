package com.xmartlabs.slackbot.manager

import com.xmartlabs.slackbot.extensions.isLastWorkingDayOfTheMonth
import com.xmartlabs.slackbot.extensions.toPrettyString
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Suppress("MaxLineLength")
object MessageManager {
    val LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd LLLL")
    val LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.US)

    fun getOngoardingMessage(xlBotUserId: String, newMembersIds: List<String>?): String {
        val joinedIds = newMembersIds?.joinToString(" ") { "<@$it>" }
        val peopleWithSpace = if (joinedIds.isNullOrBlank()) "" else "$joinedIds "
        return """

                ${peopleWithSpace}Welcome to Xmartlabs!:wave: :xl: We are very happy for having you onboard :muscle::muscle::muscle:  

                <https://www.notion.so/xmartlabs/Onboarding-c092b413380341948aabffa17bd85647 | Go to the Onboarding to know which are your next steps!>                  
                    
                Additionally, please: 
                • Add a Profile picture to Slack & Bamboo :camera: :star: 
                • <https://www.notion.so/xmartlabs/Setup-Calendars-URLs-40a4c5506a03429dbdccea169646a8a3 | Add calendar URLs :calendar:>
                • Shot :absenta: (in next XL after)
                
                Regards <@$xlBotUserId> :slack:
                """.trimIndent()
    }

    fun getInvalidTogglEntriesMessage(
        userId: String,
        untrackedTime: Duration,
        from: LocalDateTime,
        to: LocalDateTime,
        reportUrl: String,
    ) = if (to.toLocalDate().isLastWorkingDayOfTheMonth()) {
        getInvalidTogglEntriesMonthlyMessage(userId, untrackedTime, from, to, reportUrl)
    } else {
        getInvalidTogglEntriesWeeklyMessage(userId, untrackedTime, from, to, reportUrl)
    }

    private fun getInvalidTogglEntriesWeeklyMessage(
        userId: String,
        untrackedTime: Duration,
        from: LocalDateTime,
        to: LocalDateTime,
        reportUrl: String,
    ) = """
        Hi <@$userId>, you have *${untrackedTime.toPrettyString()}* tracked toggl entries with an invalid format (no project assigned or an empty description).
        Report generated from ${from.format(LOCAL_DATE_FORMATTER)} to ${to.format(LOCAL_DATE_FORMATTER)}.
        It's important to keep your toggl up to date.
        Please, take a look at <$reportUrl | yours entries>. 
        
        If you have any questions, you can use `/xlbot toggl` slack command.
    """.trimIndent()

    private fun getInvalidTogglEntriesMonthlyMessage(
        userId: String,
        untrackedTime: Duration,
        from: LocalDateTime,
        to: LocalDateTime,
        reportUrl: String,
    ) = """
        :warning::warning::warning:
        Hi <@$userId>, *we're closing the month* and you have *${untrackedTime.toPrettyString()}* tracked toggl entries with an invalid format (no project assigned or an empty description).
        Report generated from ${from.format(LOCAL_DATE_FORMATTER)} to ${to.format(LOCAL_DATE_FORMATTER)}.
        It's important to keep your toggl up to date.
        Please, take a look at <$reportUrl | yours entries>. 
        
        If you have any questions, you can use `/xlbot toggl` slack command.
    """.trimIndent()
}
