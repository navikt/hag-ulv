package no.nav.helsearbeidsgiver

import io.ktor.client.*
import io.ktor.client.engine.apache5.Apache5
import io.ktor.network.sockets.*
import io.ktor.server.application.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.PrintStream

val client = HttpClient(Apache5)

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

fun Application.module() {
    configureRouting()

    environment.monitor.subscribe(ApplicationStarted) {
        val kubeCtlClient = KubeCtlClient()
        val status = kubeCtlClient.getStatus()

        System.setErr(PrintStream(FileOutputStream("/dev/null")))

        GlobalScope.launch {
            delay(1000)

            printStartupMelding(status)
        }
    }
}

private fun printStartupMelding(status: KubeCtlStatus) {
    val reset = "\u001B[0m"
    val bold = "\u001B[1m"
    val blue = "\u001B[34m"
    val green = "\u001B[32m"
    val cyan = "\u001B[36m"
    val red = "\u001B[31m"
    val line = "-".repeat(60)

    fun centerText(
        text: String,
        width: Int = 60,
    ): String {
        val padding = (width - text.length) / 2
        return " ".repeat(padding) + text + " ".repeat(padding)
    }

    fun KubeCtlStatus.feilTekst(): String =
        when {
            this == KubeCtlStatus.UNAUTHORIZED -> "Ikke autorisert - Logg på med: ${bold}${green}gcloud auth login$reset"
            this == KubeCtlStatus.TIMEOUT -> "Timeout mot kubectl - Er du pålogget med ${bold}${green}NAIS$reset?"
            else -> "Ukjent feil - Se på logs for flere detaljer"
        }

    val feilTekst = """
$red🛑 Serveren ble ikke startet$reset    

${red}${bold}Feil ved oppstart:$reset    
   ${status.feilTekst()}
    """

    val successTekst = """
${green}Server online$reset    
        
${green}${bold}Hent token:$reset    
  http://localhost:4242/token/maskinporten-hag-lps-api-client
      
${green}${bold}Swagger:$reset
  http://localhost:4242/swagger
    """

    var result = green + bold + "Hent token:" + reset + "\n"
    result += "  http://localhost:4242/token/maskinporten-hag-lps-api-client" + "\n"
    result += "\n"
    result += green + bold + "Swagger:" + reset + "\n"
    result += "  http://localhost:4242/swagger" + "\n"

    println("\n".repeat(2))
    println(blue + line + reset)
    println(bold + cyan + centerText("🔑 Maskinporten Token Server 🔑") + reset)

    println(if (status == KubeCtlStatus.SUCCESS) successTekst else feilTekst)

    println(blue + line + reset)
}
