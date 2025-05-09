package no.nav.helsearbeidsgiver

import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respondText
import io.ktor.util.pipeline.PipelineContext
import no.nav.helsearbeidsgiver.kubernetes.KubeCtlClient
import no.nav.helsearbeidsgiver.kubernetes.KubeSecret
import no.nav.helsearbeidsgiver.kubernetes.rawBytevalue
import no.nav.helsearbeidsgiver.kubernetes.value
import java.io.File
import java.nio.file.Paths

const val DOCKER_IMAGE = "ghcr.io/kafbat/kafka-ui"
const val DOCKER_KEYSTORE_PATH = "/tmp/client.keystore.p12"
const val DOCKER_TRUSTSTORE_PATH = "/tmp/client.truststore.jks"
const val DOCKER_CONFIG_PATH = "/tmp/config.yml"

fun lagLokalFil(
    navn: String,
    data: ByteArray,
): String {
    val kafkaUiDir = Paths.get(File(System.getProperty("user.dir")).absolutePath, "kafka-ui").toFile()
    if (!kafkaUiDir.exists()) {
        kafkaUiDir.mkdir()
    }

    val file = File(kafkaUiDir, navn)
    file.writeBytes(data)
    file.deleteOnExit()
    return file.absolutePath
}

suspend fun PipelineContext<Unit, ApplicationCall>.handleKafkaUiResponse() {
    try {
        val serviceNavn = call.parameters["service-navn"] ?: throw BadRequestException("Mangler parameter")
        println("MOTATT REQUEST >$serviceNavn<")
        val secret = TokenService(SecretType.Aiven).hentSecret(serviceNavn = serviceNavn)

        val imageErInstallert = imageEksisterer(DOCKER_IMAGE)
        println("Image installert: $imageErInstallert")

        call.respondText(kafkaHtmlSide(htmlLoading(serviceNavn, imageErInstallert)), ContentType.Text.Html)

        startKafkaUi(secret)
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

fun startKafkaUi(secret: KubeSecret) {
    println("-- STARTER KAFKA UI --")
    val keystorePath = "client.keystore.p12".let { key -> lagLokalFil(key, secret.rawBytevalue(key)) }
    val truststorePath = "client.truststore.jks".let { key -> lagLokalFil(key, secret.rawBytevalue(key)) }

    val kafkaBrokers = secret.value("KAFKA_BROKERS")
    val configInnhold = generereKafkaUiConfig(bootstrapServers = kafkaBrokers)
    val configPath = lagLokalFil("config.yml", configInnhold.toByteArray())

    val cmd: List<String> =
        listOf(
            "docker run -d -it -p 8080:8080".split(" "),
            listOf("-v", "$keystorePath:$DOCKER_KEYSTORE_PATH:ro"),
            listOf("-v", "$truststorePath:$DOCKER_TRUSTSTORE_PATH:ro"),
            listOf("-v", "$configPath:$DOCKER_CONFIG_PATH"),
            listOf("-e", "spring.config.additional-location=$DOCKER_CONFIG_PATH"),
            listOf("-e", "JAVA_TOOL_OPTIONS:\"-XX:UseSVE=0\""),
            listOf(DOCKER_IMAGE),
        ).flatten()

    DockerManager.startContainer(cmd)
}

fun generereKafkaUiConfig(bootstrapServers: String): String =
    """
    kafka:
      clusters:
        - bootstrap-servers: "$bootstrapServers"
          name: "kafka-ui" 
          properties:
            security.protocol: "SSL"
            ssl:
              truststore:
                location: $DOCKER_TRUSTSTORE_PATH
                password: "changeme"
              keystore:
                location: $DOCKER_KEYSTORE_PATH
                password: "changeme"
                type: PKCS12
    """.trimIndent()

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

fun String?.tilKafkaUiUrl(): String = "http://localhost:8080/ui/clusters/kafka-ui/all-topics/$this/messages?limit=100&mode=LATEST"

fun htmlLoading(
    serviceNavn: String,
    imageInstallert: Boolean,
): String {
    val loadingTimeMs = if (imageInstallert) 20 * 1000 else 50 * 1000
    val redirectUrl =
        when {
            serviceNavn.contains("lps-api") -> "teamsykmelding.syfo-sendt-sykmelding".tilKafkaUiUrl()
            serviceNavn.startsWith("im") -> "helsearbeidsgiver.rapid".tilKafkaUiUrl()
            else -> "http://localhost:8080/ui/clusters/kafka-ui/all-topics?perPage=25&q=helsearbeidsgiver"
        }
    return """
        <div>Starter Kafka UI med rettigheter for <strong>$serviceNavn</strong></div>
        ${if (imageInstallert) "" else "<br><div>Docker image ikke funnet og blir nå nedlastet i bakgrunnen.</div>"}
        <br><div>Du blir videresendt om ${loadingTimeMs / 1000} sekunder.</div>
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
            <h3>HAG Utvikler Løsnings Verktøy</h3>
            <h1>Kafka UI</h1>
            $customHtmlContent
        </body>
    </html>
    """.trimIndent()
