package no.nav.helsearbeidsgiver

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking

fun String.veksleTilAltinnToken(): String {
    val httpClient =
        HttpClient(Apache5) {
            expectSuccess = true
        }

    val altinnToken = runBlocking {
        httpClient
            .get("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten") {
                bearerAuth(this@veksleTilAltinnToken)
            }.bodyAsText()
            .replace("\"", "")
    }
    println("Vekslet til Altinn-token: $altinnToken")
    return altinnToken
}
