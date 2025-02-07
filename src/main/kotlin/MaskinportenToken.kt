package no.nav.helsearbeidsgiver

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.maskinporten.MaskinportenClient
import no.nav.helsearbeidsgiver.maskinporten.MaskinportenClientConfigJwk

fun hentMaskinportenToken(
    secret: KubeCtlSecret,
    ekstraScope: String? = null,
): String {
    val scope = secret.scope + (if (ekstraScope != null) " $ekstraScope" else "")
    val maskinportenClient =
        MaskinportenClient(
            maskinportenClientConfig =
                MaskinportenClientConfigJwk(
                    issuer = secret.issuer,
                    scope = scope,
                    clientId = secret.clientId,
                    endpoint = "https://test.maskinporten.no/token",
                    clientJwk = secret.clientJwk,
                    // additionalClaims = getSystemBrukerClaim("315587336"),
                ),
        )

    try {
        val token = runBlocking { maskinportenClient.fetchNewAccessToken().tokenResponse.accessToken }
        println("Hentet token: $token")
        return token
    } catch (e: Exception) {
        println(e)
        return "ERROR: ${e.message}"
    }
}
