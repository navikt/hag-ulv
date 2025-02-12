package no.nav.helsearbeidsgiver

import io.ktor.http.*
import io.ktor.network.sockets.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*

fun PipelineContext<Unit, ApplicationCall>.getSecret(serviceNavn: String): KubeCtlSecret {
    val cachedSecret = SecretsCache.getValue(serviceNavn)
    call.response.header("Cache-Control", if (cachedSecret == null) "no-cache" else "max-age=infinity")

    if (cachedSecret != null) {
        return cachedSecret
    }

    val kubeCtlClient = KubeCtlClient

    val navnListe = kubeCtlClient.getMaskinportenServiceNames().filter { it.contains("maskinporten") }

    val targetServiceNavn =
        navnListe
            .find { it.contains(serviceNavn) }
            ?: throw BadRequestException("Fant ikke service. Må være en av disse:\n${navnListe.joinToString("\n")}")

    val secret = kubeCtlClient.getSecrets(targetServiceNavn)
    return secret
}

fun PipelineContext<Unit, ApplicationCall>.getToken(
    serviceNavn: String,
    ekstraScope: String? = null,
): String {
    val secret = getSecret(serviceNavn)

    return hentMaskinportenToken(secret, ekstraScope)
}
