package no.nav.helsearbeidsgiver

import io.ktor.server.plugins.BadRequestException
import no.nav.helsearbeidsgiver.kubernetes.KubeCtlClient
import no.nav.helsearbeidsgiver.kubernetes.KubeSecret
import no.nav.helsearbeidsgiver.token.getAzureToken
import no.nav.helsearbeidsgiver.token.hentMaskinportenToken
import no.nav.helsearbeidsgiver.token.veksleTilAltinnToken

enum class SecretType {
    Maskinporten,
    Altinn,
    Azure,
    TokenX,
    Aiven,
}

data class TokenResponse(
    val token: String,
    val secretErCached: Boolean,
)

class TokenService(
    val secretType: SecretType,
) {
    fun hentTokenResponse(
        serviceNavn: String,
        parameter: String?,
    ): TokenResponse {
        val cachedSecret = SecretsCache.getValue(secretType, serviceNavn)

        val secret = cachedSecret ?: this.hentSecret(serviceNavn)

        val token = this.hentToken(secret, parameter)

        return TokenResponse(
            token = token,
            secretErCached = cachedSecret != null,
        )
    }

    private fun hentToken(
        secret: KubeSecret,
        parameter: String?,
    ): String =
        when (secretType) {
            SecretType.Maskinporten -> hentMaskinportenToken(secret)
            SecretType.Altinn -> hentMaskinportenToken(secret).veksleTilAltinnToken()
            SecretType.Azure -> getAzureToken(secret, parameter)

            else -> throw NotImplementedError("Har ikke implementert den type secret ${secretType.name}")
        }

    private fun hentSecret(serviceNavn: String): KubeSecret {
        val kubeCtlClient = KubeCtlClient

        val navnListe = kubeCtlClient.getServices(secretType)

        val targetServiceNavn =
            navnListe
                .find { it.contains(serviceNavn) }
                ?: throw BadRequestException("Fant ikke service. Må være en av disse:\n${navnListe.joinToString("\n")}")

        return KubeCtlClient.getSecrets(targetServiceNavn)
    }
}
