package com.xmartlabs.slackbot

@Suppress("MaxLineLength")
object MessageManager {
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
}
