package by.mksn.gae.easycurrbot.route

import by.mksn.gae.easycurrbot.AppConfig
import by.mksn.gae.easycurrbot.service.CombinedService
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import kotlinx.html.*
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.slf4j.LoggerFactory


private val LOG = LoggerFactory.getLogger(Application::class.java)!!


fun Routing.exchangeEndpoint(config: AppConfig, service: CombinedService) {
    post(config.routes.exchange) {
        try {
            val query = call.receive<String>()
            service.exchange.invalidateExchangeRates()
            val (inputQuery, inputError) = service.input.parse(query)
            when {
                inputQuery != null -> {
                    val exchangeResults = service.exchange.exchangeInputQuery(inputQuery)
                    call.respond(HttpStatusCode.OK, exchangeResults)
                }
                inputError != null -> {
                    call.respond(HttpStatusCode.BadRequest, inputError)
                }
            }
        } catch (e: Exception) {
            LOG.info("Unexpected error", e)
            call.respond(HttpStatusCode.InternalServerError)
        }

    }
}

fun HEAD.styles() {
    link(rel = "stylesheet", href = "//cdnjs.cloudflare.com/ajax/libs/skeleton/2.0.4/skeleton.css")
    link(rel = "stylesheet", href = "//cdnjs.cloudflare.com/ajax/libs/github-markdown-css/3.0.1/github-markdown.min.css")
    link(rel = "stylesheet", href = "https://afeld.github.io/emoji-css/emoji.css")
    link(rel = "shortcut icon", href = "/thumbs/favicon.ico", type = "image/x-icon")
    link(rel = "icon", href = "/thumbs/favicon.ico", type = "image/x-icon")
    style {
        unsafe {
            raw("""
                .body {
                    box-sizing: border-box;
                    margin: 0 auto;
                    min-width: 200px;
                    max-width: 980px;
                    padding: 45px;
                }

                .markdown-body code { border: none; }

                @media (max-width: 767px) {
                    .body {
                        padding: 15px;
                    }
                }
                """.trimIndent())
        }
    }
}

fun renderMarkdownHelp(help: String): String {
    val parser = Parser.builder().build()
    val renderer = HtmlRenderer.builder().softbreak("<br/>").build()
    return renderer.render(parser.parse(help))
}

fun Routing.rootPage(config: AppConfig) {
    val helpHtml = renderMarkdownHelp(config.strings.telegram.help)

    get("/") {
        call.respondHtml {
            head {
                title { +"EasyCurrBot" }
                styles()
            }
            body {
                div(classes = "body markdown-body") {
                    h2 { +"Easy Currency Bot" }
                    p {
                        +"Перед вами demo телеграм бота "
                        b { a(href = "https://t.me/easycurrbot", target = "_blank") { +"@easycurrbot" } }
                    }
                    form {
                        div(classes = "row") {
                            div(classes = "ten columns") {
                                input(classes = "u-full-width", type = InputType.text) {
                                    id = "query"
                                    value = "10злотых + 8 BYN"
                                    placeholder = "Введите запрос..."
                                }
                            }
                            div(classes = "two columns") {
                                input(classes = "u-full-width button-secondary", type = InputType.button) {
                                    id = "sendQuery"
                                    value = "GO"
                                }
                            }
                        }
                    }
                    pre {
                        code {
                            id = "result"
                        }
                    }
                    h2 { +"Описание бота:" }
                    unsafe { raw(helpHtml) }
                }
                script {
                    type = "text/javascript"
                    src = "//code.jquery.com/jquery-3.3.1.min.js"
                }
                script {
                    type = "text/javascript"
                    src = "/scripts/exchange.js"
                }
            }
        }
    }
}