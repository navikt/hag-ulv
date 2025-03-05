package no.nav.helsearbeidsgiver.token

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.client

fun String.veksleTilAltinnToken(): String {
    val altinnToken =
        runBlocking {
            client
                .get("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten") {
                    bearerAuth(this@veksleTilAltinnToken)
                }.bodyAsText()
                .replace("\"", "")
        }
    println("Vekslet til Altinn-token: $altinnToken")
    return altinnToken
}
