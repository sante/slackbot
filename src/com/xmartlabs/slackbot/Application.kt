package com.xmartlabs.slackbot

import com.slack.api.bolt.App
import com.slack.api.bolt.context.builtin.SlashCommandContext
import com.slack.api.bolt.jetty.SlackAppServer
import com.slack.api.bolt.request.builtin.SlashCommandRequest
import com.slack.api.bolt.response.Response


fun main(args: Array<String>) {
    val app = App()
        .command("/xlbot") { req, ctx -> processCommand(req, ctx) }
        .command("/slackhelp") { req, ctx -> processCommand(req, ctx) }

    val port = System.getenv("PORT")?.toIntOrNull() ?: 3000

    val server = SlackAppServer(app, "/slack/events", port)
    server.start() // http://localhost:3000/slack/events
}

private fun processCommand(
    req: SlashCommandRequest,
    ctx: SlashCommandContext
): Response? {
    val answer = when (Command.fromKey(req.payload?.text)) {
        null -> """
                Hi :wave:! Check XL useful bots! :slack:
                • *toggl* -> Where should I track this? :toggl_on:
                • *wifi pass* -> Do you know Xmartlabs' office WIFI password? :signal_strength: :key:
                • *recycling*-> Recycling help! :recycle:
                • *anniversary* -> What happens in my anniversary/birthday? :tada: :birthday:
                • *calendar urls?*-> Who is in TPO? When is the next lightning talk? :calendar:
                • *setup process* -> Do you know what you have to do when you onboard to :xl: ?
                • *lightning talks*-> WTF is a lightning talk :zap:?
                
                
                This bot *WAS NOT* made by @bala
                """.trimIndent()
        Command.WIFI_PASS -> """

                *Wifi pass* :signal_strength: :key::
                Internal: Xmartlabs33, Guests: xlinvitado
                """.trimIndent()
        Command.TOGGL -> """
                
                *Toggl* :toggl_on: :
                
                • ¿Entrevistas? _Seleccion y entrevistas -> Xmartlabs_
                • ¿Reviews? _Team Reviews -> Xmartlabs_
                • ¿Lightning talks? _Lightning talk -> Xmartlabs_
                • ¿1:1 con PM? _En tu proyecto._
                • ¿1:1 con tu TL? _En tu equipo, Team X -> Xmartlabs._
                • ¿Iniciativas? _Consulta a tu TL si la iniciativa en la que estás trabajando tiene un proyecto específico ya creado. Si es así, va en ese proyecto. En caso de que no, va dentro de tu equipo._
                • ¿Code review o soporte a un compañero de otro proyecto? _Se trackea dentro de ese proyecto. En caso de que no lo veas en Toggl, comunicate con el PM de ese proyecto para que te agregue al team :)_
                
                En caso de tener alguna duda, consultá al equipo de operaciones o RRHH.
              
                """.trimIndent()
        Command.RECYCLING -> """

                *Recycling* :recycle::

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
        Command.ANNIVERSARY -> """
                
                *Anniversary* :tada: :birthday::
                
                • *Aniversarios y 3 meses en la empresa:* 
                    • Cada mes contactamos a los que cumplen su aniversario y 3 meses en la empresa. 
                    • Entre ustedes se organizan y eligen una forma de celebrar con todos (masitas, helado, pizza, etc) :masitas: :ice_cream: :pizza: :cookie:. Puede ser lo que ustedes quieran y cuándo quieran 
                    • En los aniversarios la empresa les organiza una comida, regalitos y sorpresas por definir :eyes: 
                    
                • *Cumpleaños:* 
                    • La empresa le regala al cumpleañero un desayuno :coffee: :croissant: :chocolate_bar: 
                    • A fin de mes se hace un festejo para todos los cumpleañeros! La empresa compra tortas, bebidas y algo para picar. :birthday: :pizza:
                """.trimIndent()
        Command.CALENDAR_URL -> """
                
                *Calendars* :calendar: :
                    - https://www.notion.so/xmartlabs/Setup-Calendars-URLs-40a4c5506a03429dbdccea169646a8a3
                """.trimIndent()
        Command.SETUP_PROCESS -> """

                Welcome to Xmartlabs!:wave: :xl: We are very happy for having you onboard :muscle::muscle::muscle:  
                 
                Go to the Onboarding to know which are your next steps! 
                https://www.notion.so/xmartlabs/Onboarding-c092b413380341948aabffa17bd85647 
                    
                Additionally, please: 
                • Add a Profile picture to Slack & Bamboo :camera: :star: 
                • Add calendar URLs :calendar:  -> https://www.notion.so/xmartlabs/Setup-Calendars-URLs-40a4c5506a03429dbdccea169646a8a3 
                • Shot :absenta: (in next XL after)
                """.trimIndent()
        Command.LIGHTNING -> """
                
                *Lightning talks* :flashlight:: 
                The following is useful information about our lightning talks :zap:, 30 min talks where someone exposes something interesting he/she found.
                
                •  Lightning talks are added to the Eventos XL calendar :calendar: You will find the Zoom link meeting in there too :zoom:
                •  Do you want to share something interesting with the team? Please follow this link :memo:
                •  We record every lightning talk so don’t care if you are not able to join that day. You can find all the recorded lightning talks here :movie_camera:
                •  Remember to track the talk in Toggl (Lightning talk -> Xmartlabs) :toggl_on:
                """.trimIndent()
        Command.BALA -> ":wave: bala! :bala:"
    }
    return ctx.ack(answer)
}

