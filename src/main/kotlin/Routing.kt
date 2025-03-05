package no.nav.helsearbeidsgiver

import io.ktor.http.*
import io.ktor.network.sockets.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import java.io.File

fun Application.configureRouting() {
    routing {
        get("/token/{service-navn}") {
            handleGetTokenResponse(SecretType.Maskinporten)
        }

        get("/altinn-token/{service-navn}") {
            handleGetTokenResponse(SecretType.Altinn)
        }

        get("/azure-token/{service-navn}/{parameter?}") {
            handleGetTokenResponse(SecretType.Azure)
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

private suspend fun PipelineContext<Unit, ApplicationCall>.handleGetTokenResponse(secretType: SecretType) {
    try {
        val serviceNavn =
            call.parameters["service-navn"]
                ?: throw BadRequestException("Mangler parameter Maskinporten service")

        val parameter = call.parameters["parameter"]

        val tokenResponse = TokenService(secretType).hentTokenResponse(serviceNavn, parameter)

        call.response.header("Cache-Control", if (tokenResponse.erCached) "max-age=infinity" else "no-cache")

        call.respondText(tokenResponse.token)
    } catch (e: SocketTimeoutException) {
        call.respond(
            HttpStatusCode.Unauthorized,
            "Timeout med KubeCTL. Dette kan forekomme om du gjør for mange JWK secret spørringer innen kort tid\nError message: ${e.message}",
        )
    } catch (e: Exception) {
        call.respond(HttpStatusCode.BadRequest, "Error: ${e.message}")
    }
}
