package no.nav.helsearbeidsgiver
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1SecretList
import io.kubernetes.client.util.Config
import java.net.SocketTimeoutException

data class KubeCtlSecret(
    val scope: String,
    val clientId: String,
    val issuer: String,
    val clientJwk: String,
)

fun Map<String, ByteArray>.toKubeCtlSecret(): KubeCtlSecret =
    KubeCtlSecret(
        scope = parseKubeCtlVerdi("MASKINPORTEN_SCOPES"),
        clientId = parseKubeCtlVerdi("MASKINPORTEN_CLIENT_ID"),
        issuer = parseKubeCtlVerdi("MASKINPORTEN_ISSUER"),
        clientJwk = parseKubeCtlVerdi("MASKINPORTEN_CLIENT_JWK"),
    )

private fun Map<String, ByteArray>.parseKubeCtlVerdi(nøkkel: String): String =
    this[nøkkel]?.decodeToString()
        ?: throw RuntimeException("Feil ved parsing, mangler nøkkel: $nøkkel\nFor kubectl json response: $this")

// object KubeCtlClientInstance {
//    var kubeCtlClient: KubeCtlClient? = null
//        get() = field ?: throw RuntimeException("kubeCtlClient ikke initialiser")
//        private set
//
//    fun startClient() {
//        kubeCtlClient = KubeCtlClient()
//    }
// }

object KubeCtlClient {
    private val apiClient: ApiClient
    private val api: CoreV1Api

    init {
        apiClient = Config.defaultClient()
        Configuration.setDefaultApiClient(apiClient)
        api = CoreV1Api()
    }

    fun getMaskinportenServiceNames(): List<String> {
        val secrets: V1SecretList

        secrets =
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

    fun getSecrets(serviceName: String): KubeCtlSecret {
        try {
            val response =
                api.readNamespacedSecret(
                    serviceName,
                    "helsearbeidsgiver",
                    null,
                )
            val secret = response.data?.toKubeCtlSecret() ?: throw RuntimeException("No data found in secret")
            SecretsCache.setValue(serviceName, secret)
            return secret
        } catch (e: Exception) {
            throw RuntimeException("Failed to get secret value: ${e.message}", e)
        }
    }
}

enum class KubeCtlStatus {
    UNAUTHORIZED,
    TIMEOUT,
    SUCCESS,
    UNKNOWN,
}

fun initialiserKubeCtlClient(): KubeCtlStatus {
    try {
        val kubeCtlClient = KubeCtlClient
        kubeCtlClient.getMaskinportenServiceNames()
    } catch (e: Exception) {
        println("Stack trace error: ${e.stackTrace}")
        println("Exception: ${e.message}")
        println("cause: ${e.cause}")
        if (e is ApiException && e.responseBody.isNotEmpty()) {
            println("Feil med kubectl:\n\u001B[1;31m${e.responseBody}\u001B[0m")
        }
        return when {
            e is SocketTimeoutException -> KubeCtlStatus.TIMEOUT
            e is ApiException && (e.code == 403 || e.code == 401) -> KubeCtlStatus.UNAUTHORIZED
            else -> KubeCtlStatus.UNKNOWN
        }
    }
    return KubeCtlStatus.SUCCESS
}
