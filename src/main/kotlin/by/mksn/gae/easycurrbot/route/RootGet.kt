package by.mksn.gae.easycurrbot.route

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.routing.Route
import io.ktor.routing.get
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.p
import kotlinx.html.title

/**
 * @author Mikhail Snitavets
 */
fun Route.rootGet() = get("/") {
    call.respondHtml {
        head {
            title { +"Welcome Page" }
        }
        body {
            p {
                +"What are you looking here?"
            }
        }
    }
}