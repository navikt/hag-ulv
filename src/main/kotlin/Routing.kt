package no.nav.helsearbeidsgiver

import io.ktor.http.*
import io.ktor.network.sockets.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/token/{service-navn}/{ekstra-scope?}") {
            try {
                val serviceNavn =
                    call.parameters["service-navn"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Mangler parameter Maskinporten service")

                val ekstraScope = call.parameters["ekstra-scope"]

                val token = getToken(serviceNavn, ekstraScope)

                call.respondText(token)
            } catch (e: SocketTimeoutException) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    "Timeout med KubeCTL. Dette kan forekomme om du gjør for mange JWK secret spørringer innen kort tid\nError message: ${e.message}",
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Catch error: ${e.message}")
            }
        }
    }
}
