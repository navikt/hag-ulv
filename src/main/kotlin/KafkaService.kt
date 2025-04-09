package no.nav.helsearbeidsgiver

import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respondText
import io.ktor.util.pipeline.PipelineContext
import no.nav.helsearbeidsgiver.KafkaService.startKafkaUi
import no.nav.helsearbeidsgiver.kubernetes.KubeCtlClient
import no.nav.helsearbeidsgiver.kubernetes.KubeSecret
import no.nav.helsearbeidsgiver.kubernetes.rawBytevalue
import no.nav.helsearbeidsgiver.kubernetes.value

suspend fun PipelineContext<Unit, ApplicationCall>.handleKafkaUiResponse() {
    try {
        val serviceNavn = call.parameters["service-navn"] ?: throw BadRequestException("Mangler parameter")
        val secret = TokenService(SecretType.Aiven).hentSecret(serviceNavn = serviceNavn)

        startKafkaUi(secret)

        call.respondText(kafkaHtmlSide(htmlLoading(serviceNavn)), ContentType.Text.Html)
    } catch (e: BadRequestException) {
        val servicer =
            KubeCtlClient
                .getServices(SecretType.Aiven)
                .map { cleanServiceName(it) }
                .toSet()
                .toList()
                .reversed()

        call.respondText(kafkaHtmlSide(htmlVelg(servicer)), ContentType.Text.Html)
    }
}

val configName = "kafka-ui-application-local.yml"
var runningProcess: Process? = null

object KafkaService {
    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("⏻ Skrur av kafka-ui prosess")
                runningProcess?.destroy()
                runningProcess?.waitFor()
            },
        )
    }

    fun startKafkaUi(secret: KubeSecret) {
        val jarPath = resolveAbsolutePath("kafka/kafka-ui-api-v0.7.2.jar")

        val keystorePath = "client.keystore.p12".let { key -> lagTempFil(key, secret.rawBytevalue(key)) }
        val truststorePath = "client.truststore.jks".let { key -> lagTempFil(key, secret.rawBytevalue(key)) }

        println("Keystore path: $keystorePath")
        println("Truststore path: $truststorePath")
        println("kafka brokers: ${secret.value("KAFKA_BROKERS")}")

        val configContent =
            generereKafkaUiConfig(
                bootstrapServers = secret.value("KAFKA_BROKERS"),
                keystorePath = keystorePath,
                truststorePath = truststorePath,
            )

        val configPath = lagTempFil(configName, configContent)

        try {
            runningProcess?.destroy()
            runningProcess?.waitFor()
        } catch (e: InterruptedException) {
            println("Interrupted while running")
        }

        runningProcess = null

        runningProcess =
            ProcessBuilder(
                "java",
                "-Dspring.config.additional-location=$configPath",
                "-jar",
                jarPath,
                "-Dspring.security.headers.frame-options.enabled=false",
            ).inheritIO().start()
    }
}

fun generereKafkaUiConfig(
    bootstrapServers: String,
    keystorePath: String,
    truststorePath: String,
): String {
    val configContent =
        """
        kafka:
          clusters:
            - bootstrap-servers: "$bootstrapServers"  # Fill in the bootstrap servers
              properties:
                security.protocol: "SSL"  # Fill in the security protocol (e.g., SSL)
                ssl:
                  truststore:
                    location: $truststorePath
                    password: "changeme"  # Reference property or environment variable
                  keystore:
                    location: $keystorePath
                    password: "changeme"  # Reference property or environment variable
                    type: PKCS12
        """.trimIndent()

    return configContent
}

// ---------------------------- HTML rendering funksoner ----------------------------
fun htmlVelg(servicer: List<String>) =
    """
        <div>Velg tjenesten som har nødvendige Kafka tillatelser:</div>
                <ul>
                    ${
        servicer
            .map { """<li><a href="/kafka/$it" target="_blank">$it</a></li>""" }
            .joinToString("\n")
    }
                </ul>
    """.trimIndent()

fun String?.tilKafkaUiUrl(): String =
    "http://localhost:8080/ui/clusters/Default/all-topics/$this/messages?seekDirection=BACKWARD&seekType=LATEST"

fun htmlLoading(serviceNavn: String): String {
    val loadingTimeMs = 4000
    val redirectUrl =
        when {
            serviceNavn.contains("lps-api") -> "teamsykmelding.syfo-sendt-sykmelding".tilKafkaUiUrl()
            serviceNavn.startsWith("im") -> "helsearbeidsgiver.rapid".tilKafkaUiUrl()
            else -> "http://localhost:8080/ui/clusters/Default/all-topics?perPage=25&q=helsearbeidsgiver"
        }
    return """
        <div>Starter Kafka UI med rettigheter for <strong>$serviceNavn</strong></div>
         <div class="spinner"></div>
         <script>
            setTimeout(function() {
               history.pushState(null, null, window.location.href);
               window.location.href = "$redirectUrl"; 
           }, $loadingTimeMs);
         </script>
        """.trimIndent()
}

fun kafkaHtmlSide(customHtmlContent: String): String =
    """
    <html>
        <head>
            <meta charset="UTF-8">
            <title>HAG Kafka UI</title>
             <style>
            body {
                font-family: sans-serif;
                color: #333;
                margin: 0;
                padding: 20px;
                display: flex;
                flex-direction: column;
                align-items: center;
                min-height: 100vh;
            }
            .spinner {
                border: 4px solid #f3f3f3;
                border-top: 4px solid #555;
                border-radius: 50%;
                width: 30px;
                height: 30px;
                animation: spin 1s linear infinite;
                margin: 20px auto;
            }
            ul {
                list-style-type: none;
                padding-left: 0;
                margin: 20px 0;
            }
            li {
                margin: 10px 0;
            }
            a {
                color: #00448d;
                text-decoration: none;
            }
            a:hover {
                text-decoration: underline;
            }
            @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
            }
        </style>

        </head>
       
        <body>
            <h3>HAG Utvikler Verktøy</h3>
            <h1>Kafka UI</h1>
            $customHtmlContent
        </body>
    </html>
    """.trimIndent()
