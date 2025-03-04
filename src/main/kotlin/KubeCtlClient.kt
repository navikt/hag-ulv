package no.nav.helsearbeidsgiver
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1SecretList
import io.kubernetes.client.util.Config
import io.kubernetes.client.util.KubeConfig
import java.io.File
import java.io.FileReader
import java.util.*

// OBS! VÆR FORSIKTIG MED Å ENDRE DENNE VARIABELEN
val KUBE_CTL_CONTEXT = "dev-gcp"

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

fun hentDevGcpKubeConfig(): ApiClient {
    val kubeConfigFile = findConfigInHomeDir() ?: throw Exception("KubeConfigFile ikke funnet")

    val reader = FileReader(kubeConfigFile)
    val kubeConfig = KubeConfig.loadKubeConfig(reader)

    kubeConfig.setContext(KUBE_CTL_CONTEXT)
    println("KubeCtl kontekst er satt til: $KUBE_CTL_CONTEXT")

    val apiClient = Config.fromConfig(kubeConfig)
    return apiClient
}

// -----------------------------------------------------------------------
//           Denne koden er tatt fra Kubernetes Java klient
//           Repo: https://github.com/kubernetes-client/java
//           Lisens: Apache License Version 2.0
// ------------------------------------------------------------------------

fun findHomeDir(): File? {
    val envHome = System.getenv("HOME")
    if (envHome != null && envHome.length > 0) {
        val config = File(envHome)
        if (config.exists()) {
            return config
        }
    }

    if (System.getProperty("os.name").lowercase(Locale.getDefault()).startsWith("windows")) {
        val homeDrive = System.getenv("HOMEDRIVE")
        val homePath = System.getenv("HOMEPATH")
        if (homeDrive != null && homeDrive.length > 0 && homePath != null && homePath.length > 0) {
            val homeDir = File(File(homeDrive), homePath)
            if (homeDir.exists()) {
                return homeDir
            }
        }

        val userProfile = System.getenv("USERPROFILE")
        if (userProfile != null && userProfile.length > 0) {
            val profileDir = File(userProfile)
            if (profileDir.exists()) {
                return profileDir
            }
        }
    }

    return null
}

fun findConfigInHomeDir(): File? {
    val homeDir = findHomeDir()
    if (homeDir != null) {
        val config = File(File(homeDir, ".kube"), "config")
        if (config.exists()) {
            return config
        }
    }
    println("Could not find ~/.kube/config")
    return null
}
