package no.nav.helsearbeidsgiver

import io.ktor.client.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

suspend fun PipelineContext<Unit, ApplicationCall>.proxyKall() {
    try {
        val encodedUrl = URLEncoder.encode(call.request.uri.substringAfter("/proxy/"), StandardCharsets.UTF_8)
        val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8) ?: throw BadRequestException("Mangler parameter URL")
        val response: HttpResponse =
            client.request(url) {
                method = call.request.httpMethod
                headers {
                    call.request.headers
                        .toMap()
                        .filter { it.key != "Host" }
                        .filter { it.key == "Authorization" || it.key == "Accept" } // Dette er alt som trengs for Dialogporten
                        .forEach { (key, value) -> append(key, value[0]) }
                }
            }

        val responseBody = response.bodyAsText()
        call.response.status(response.status)
        call.respondText(responseBody, response.contentType())
    } catch (e: Exception) {
        println("Error: ${e.message}")
        return call.respondText("Error: ${e.message}")
    }
    call.respondText("Ukjent feil")

}// test token eyJhbGciOiJSU verdi