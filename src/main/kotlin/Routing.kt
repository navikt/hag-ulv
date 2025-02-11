package no.nav.helsearbeidsgiver

import io.ktor.client.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.network.sockets.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import java.io.File

fun Application.configureRouting() {
    routing {
        get("/token/{service-navn}/{ekstra-scope?}") {
            try {
                val serviceNavn = call.parameters["service-navn"] ?: throw BadRequestException("Mangler parameter Maskinporten service")

                val ekstraScope = call.parameters["ekstra-scope"]

                val token = getToken(serviceNavn, ekstraScope)

                call.respondText(token)
            } catch (e: SocketTimeoutException) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    "Timeout med KubeCTL. Dette kan forekomme om du gjør for mange JWK secret spørringer innen kort tid\nError message: ${e.message}",
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Error: ${e.message}")
            }
        }

        // Swagger nettside
        staticResources("/", "static")

        get("/swagger") {
            val file = File("src/main/resources/static/index.html")
            call.respondFile(file)
        }

        HttpMethod.DefaultMethods.forEach {
            route("/proxy/{url...}", it) {
                handle {
                    proxyKall()
                }
            }
        }
    }
}
