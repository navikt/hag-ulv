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

// enum class KubeCtlStatus {
//    UNAUTHORIZED,
//    TIMEOUT,
//    SUCCESS,
//    UNKNOWN,
// }
//
// fun initialiserKubeCtlClient(): KubeCtlStatus {
//    try {
//        val kubeCtlClient = KubeCtlClient
//        kubeCtlClient.getMaskinportenServices()
//    } catch (e: Exception) {
//        println("Stack trace error: ${e.stackTrace}")
//        println("Exception: ${e.message}")
//        println("cause: ${e.cause}")
//        if (e is ApiException && e.responseBody.isNotEmpty()) {
//            println("Feil med kubectl:\n\u001B[1;31m${e.responseBody}\u001B[0m")
//        }
//        return when {
//            e is SocketTimeoutException -> KubeCtlStatus.TIMEOUT
//            e is ApiException && (e.code == 403 || e.code == 401) -> KubeCtlStatus.UNAUTHORIZED
//            else -> KubeCtlStatus.UNKNOWN
//        }
//    }
//    return KubeCtlStatus.SUCCESS
// }
