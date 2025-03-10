package no.nav.helsearbeidsgiver.kubernetes
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1SecretList
import no.nav.helsearbeidsgiver.SecretType
import no.nav.helsearbeidsgiver.SecretsCache
import java.util.*

typealias KubeSecret = Map<String, String>

fun Map<String, String>.value(key: String): String =
    this[key] ?: throw RuntimeException("Feil ved parsing, mangler nøkkel: ${key}\nFor kubectl json response: $this")

fun SecretType.getNameString() =
    when (this) {
        SecretType.Maskinporten -> "maskinporten"
        SecretType.Altinn -> "maskinporten"
        SecretType.Azure -> "azure"
        SecretType.TokenX -> "tokenx"
        SecretType.Aiven -> "aiven"
    }

object KubeCtlClient {
    private val apiClient: ApiClient
    private val api: CoreV1Api

    init {
        apiClient = hentDevGcpKubeConfig()
        Configuration.setDefaultApiClient(apiClient)
        api = CoreV1Api()
    }

    private fun getServiceNames(): List<String> {
        val secrets: V1SecretList =
            api.listNamespacedSecret(
                "helsearbeidsgiver",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
            )

        return secrets.items
            .map { it.metadata?.name.toString() }
    }

    private fun getServiceWithName(name: String): List<String> = getServiceNames().filter { it.contains(name) }

    fun getServices(secretType: SecretType) = getServiceWithName(secretType.getNameString())

    fun getSecrets(serviceName: String): KubeSecret {
        try {
            val response =
                api.readNamespacedSecret(
                    serviceName,
                    "helsearbeidsgiver",
                    null,
                )
            val secret = response.data?.mapValues { it.value.decodeToString() } ?: throw RuntimeException("No data found in secret")
            SecretsCache.setValue(serviceName, secret)
            return secret
        } catch (e: Exception) {
            throw RuntimeException("Failed to get secret value: ${e.message}", e)
        }
    }
}
