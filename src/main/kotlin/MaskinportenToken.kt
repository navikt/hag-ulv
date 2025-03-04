package no.nav.helsearbeidsgiver

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.maskinporten.MaskinportenClient
import no.nav.helsearbeidsgiver.maskinporten.MaskinportenClientConfigJwk

private fun KubeSecret.tilMaskinportenConfig() =
    MaskinportenClientConfigJwk(
        issuer = this.value("MASKINPORTEN_ISSUER"),
        scope = this.value("MASKINPORTEN_SCOPES"), // her kan du definere custom scopes
        clientId = this.value("MASKINPORTEN_CLIENT_ID"),
        endpoint = "https://test.maskinporten.no/token",
        clientJwk = this.value("MASKINPORTEN_CLIENT_JWK"),
        // additionalClaims = getSystemBrukerClaim("315587336"),
    )

private fun getToken(secret: KubeSecret) =
    runBlocking {
        MaskinportenClient(secret.tilMaskinportenConfig())
            .fetchNewAccessToken()
            .tokenResponse
            .accessToken
    }

fun hentMaskinportenToken(secret: KubeSecret): String {
    try {
        val token = getToken(secret)
        println("Hentet token: $token")
        return token
    } catch (e: Exception) {
        println(e)
        return "ERROR: ${e.message}"
    }
}
