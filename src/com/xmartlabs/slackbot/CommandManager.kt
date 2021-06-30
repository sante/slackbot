package com.xmartlabs.slackbot

import com.slack.api.app_backend.slash_commands.payload.SlashCommandPayload
import com.slack.api.app_backend.slash_commands.response.SlashCommandResponse
import com.slack.api.bolt.context.Context
import com.slack.api.bolt.response.ResponseTypes
import com.slack.api.model.kotlin_extension.block.withBlocks

@Suppress("MaxLineLength")
object CommandManager {
    val onboarding =
        Command(
            "onboarding", "setup process",
            title = "Onboarding :wave: :xl:",
            description = "Do you know what you have to do when you onboard to :xl: ?"
        ) { payloadText, context ->
            val peoplePayloadText = getMembersFromCommandText(context, payloadText)
            MessageManager.getOngoardingMessage(UserChannelRepository.toUserId(context, XL_BOT_NAME), peoplePayloadText)
        }

    val commands = listOf(
        Command(
            "anniversary",
            title = "Anniversary :tada: :birthday:",
            description = "What happens in my anniversary/birthday? :tada: :birthday:"
        ) { _, _ ->
            """
                
                *Anniversary* :tada: :birthday:
                
                • *Aniversarios y 3 meses en la empresa:* 
                    • Cada mes contactamos a los que cumplen su aniversario y 3 meses en la empresa. 
                    • Entre ustedes se organizan y eligen una forma de celebrar con todos (masitas, helado, pizza, etc) :masitas: :ice_cream: :pizza: :cookie:. Puede ser lo que ustedes quieran y cuándo quieran 
                    • En los aniversarios la empresa les organiza una comida, regalitos y sorpresas por definir :eyes: 
                    
                • *Cumpleaños:* 
                    • La empresa le regala al cumpleañero un desayuno :coffee: :croissant: :chocolate_bar: 
                    • A fin de mes se hace un festejo para todos los cumpleañeros! La empresa compra tortas, bebidas y algo para picar. :birthday: :pizza:
                """.trimIndent()
        },
        Command(
            "calendar",
            title = "Calendars setup :calendar:",
            description = "Who is in PTO? When is the next lightning talk? :calendar:",
        ) { _, _ ->
            """
                
                *Calendars* :calendar:
                    - <https://www.notion.so/xmartlabs/Setup-Calendars-URLs-40a4c5506a03429dbdccea169646a8a3 | Calendar Setup>
                """.trimIndent()
        },
        Command(
            "lightning",
            title = "Lightning talks :flashlight:",
            description = "WTF is a lightning talk :zap:?"
        ) { _, _ ->
            """
                
                *Lightning talks* :flashlight:: 
                Hi! The following is useful information about our lightning talks :zap:, 30 min talks where someone exposes something interesting he/she found.

                • Lightning talks are added to the Eventos XL calendar :calendar: You will find the Zoom link meeting in there too :zoom:
                • Do you want to share something interesting with the team? <https://docs.google.com/forms/d/e/1FAIpQLSfvXceDAb9Eijkzkvi7-WUvvAHDh48NYEEIRDkTds6zZeXaUw/viewform | Please follow this link:> :memo:
                • We record every lightning talk so don’t care if you are not able to join that day. <https://www.notion.so/xmartlabs/Lightning-Talks-f21bb5e19823415fba3dcd2efc42d4ac | You can find all the recorded lightning talks here:>  :movie_camera:
                • Remember to track the talk in Toggl (Lightning talk -> Xmartlabs) :toggl_on:
                """.trimIndent()
        },
        Command(
            "recycling",
            title = "Recycling :recycle:",
            description = "Recycling help! :recycle:"
        ) { _, _ ->
            """

                *Recycling* :recycle:

                Cosas que parecen reciclables pero no lo son 
                • *Mezclados (Negro)*: 
                    • Paquetes de yerba 
                    • Paquetes de café
                    • Boletos y tickets
                • *Reciclables varios (verde):*
                    • Caja de leche (Tetrapak)
                    • Envoltorios de galletas (Todos)
                    • Doypack (Envases de mayonesa, etc)
                • *Envases plásticos (Amarillo):*
                    • Botellas de plástico
                    • Cualquier plástico que tenga el símbolo :recycle: 1 - PET o :recycle: 2 - PEAD
                • *Papel y cartón (Azul):*
                    • Cajas de te, edulcorante
                    • Papel limpio y seco
                • *Compostable (Rojo):*
                    • Yerba
                    • Café
                    • Sobrecitos de té (con la bolsita y cuerda)
                    • Cascaras de frutas y verduras
                    • Restos de comida
                    • Servilletas de papel usadas
                
                Recordá que todo lo reciclable tiene que estar limpio y seco :recycle:!
                """.trimIndent()
        },
        Command(
            "toggl",
            title = "Toggl :toggl_on:",
            description = "Where should I track this? :toggl_on:"
        ) { _, _ ->
            """
                
                *Toggl* :toggl_on:
                
                • ¿Entrevistas? _Seleccion y entrevistas -> Xmartlabs_
                • ¿Reviews? _Team Reviews -> Xmartlabs_
                • ¿Lightning talks? _Lightning talk -> Xmartlabs_
                • ¿1:1 con PM? _En tu proyecto._
                • ¿1:1 con tu TL? _En tu equipo, Team X -> Xmartlabs._
                • ¿Iniciativas? _Consulta a tu TL si la iniciativa en la que estás trabajando tiene un proyecto específico ya creado. Si es así, va en ese proyecto. En caso de que no, va dentro de tu equipo._
                • ¿Code review o soporte a un compañero de otro proyecto? _Se trackea dentro de ese proyecto. En caso de que no lo veas en Toggl, comunicate con el PM de ese proyecto para que te agregue al team :)_
                
                En caso de tener alguna duda, consultá al equipo de operaciones o RRHH.
              
                """.trimIndent()
        },
        Command(
            "wifi pass",
            title = "Wifi pass :signal_strength:",
            description = "Do you know Xmartlabs' office WIFI password? :signal_strength: :key:"
        ) { _, _ ->
            """
                *Wifi pass* :signal_strength: :key:
                Internal: Xmartlabs33, Guests: xlinvitado
            """.trimIndent()
        },
    ) + onboarding

    private val default = Command(
        title = "Help Command",
        answerResponse = { _, context, _ ->
            SlashCommandResponse.builder()
                .blocks(withBlocks {
                    section {
                        markdownText(
                            "Hi :wave:! Check XL useful <@${
                                UserChannelRepository.toUserId(
                                    context,
                                    XL_BOT_NAME
                                )
                            }> commands! :slack:"
                        )
                    }

                    commands.forEach { command ->
                        section {
                            button {
                                actionId(command.buttonActionId)
                                text(command.title, emoji = true)
                                value(command.keys.first())
                            }
                            if (!command.description.isNullOrBlank()) {
                                markdownText("• ${command.description}", true)
                            }
                        }
                    }
                })
                .responseType(ResponseTypes.ephemeral) // Force it
                .build()
        },
        answerText = { _, context ->
            val options = commands.joinToString(" \n") { command ->
                "• *${command.title}*: $${command.description}"
            }
            val botUser = UserChannelRepository.toUserId(context, XL_BOT_NAME)
            "\nHi :wave:! Check XL useful <@$botUser> commands! :slack:\n\n$options"
        },
    )

    init {
        require(
            commands
                .flatMap { it.keys.asList() }
                .groupBy { it.lowercase() }
                .values
                .map { it.size }
                .maxOrNull() == 1
        ) {
            "Duplicate commands are not allowed"
        }
    }

    private fun String.sanitizeKey() = replace(" ", "_")
        .lowercase()

    fun processCommand(ctx: Context, payload: SlashCommandPayload?, visibleInChannel: Boolean): SlashCommandResponse = (
            payload?.text?.let { userKey ->
                commands
                    .firstOrNull { command ->
                        command.keys.any { commandKey ->
                            userKey.sanitizeKey().contains(commandKey.sanitizeKey())
                        }
                    }
            } ?: default)
        .answerResponse(payload?.text, ctx, visibleInChannel)
}

private fun getMembersFromCommandText(ctx: Context, peopleCommandText: String?): List<String>? =
    peopleCommandText
        ?.split("@")
        ?.map { it.trim() }
        ?.filterNot { it.isBlank() }
        ?.let { UserChannelRepository.toUserId(ctx, it) }
