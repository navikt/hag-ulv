package no.nav.helsearbeidsgiver
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1SecretList
import io.kubernetes.client.util.Config
import java.util.*

data class KubeCtlSecret(
    val scope: String,
    val clientId: String,
    val issuer: String,
    val clientJwk: String,
)

fun decodeBase64(string: String): String = String(Base64.getDecoder().decode(string))

fun Map<String, ByteArray>.toKubeCtlSecret(): KubeCtlSecret =
    KubeCtlSecret(
        scope = this["MASKINPORTEN_SCOPES"]?.decodeToString() ?: "",
        clientId = this["MASKINPORTEN_CLIENT_ID"]?.decodeToString() ?: "",
        issuer = this["MASKINPORTEN_ISSUER"]?.decodeToString() ?: "",
        clientJwk = this["MASKINPORTEN_CLIENT_JWK"]?.decodeToString() ?: "",
    )

class KubeCtlClient {
    private val apiClient: ApiClient = Config.defaultClient()
    private val api: CoreV1Api

    init {
        Configuration.setDefaultApiClient(apiClient)
        api = CoreV1Api()
    }

    fun getMaskinportenServiceNames(): List<String> {
        val secrets: V1SecretList

        try {
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

//            secrets.items.forEach { println(it.metadata?.name) }

            return secrets.items
                .map { it.metadata?.name.toString() }
//                .filter { it.contains("maskinporten") }
        } catch (e: Exception) {
            println("Error fetching secrets: ${e.message}")
            e.printStackTrace()
        }
        return emptyList()
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
