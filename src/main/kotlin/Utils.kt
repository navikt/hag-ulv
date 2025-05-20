package no.nav.helsearbeidsgiver

import no.nav.helsearbeidsgiver.kubernetes.KUBE_CTL_CONTEXT_ER_ALLTID_DEV
import java.util.Locale
import kotlin.system.exitProcess

const val RESET = "\u001B[0m"
const val ORANGE = "\u001B[38;5;208m"
const val BRIGHT_CYAN = "\u001B[38;5;117m"
const val BRIGHT_RED = "\u001B[38;5;196m"
const val GREEN = "\u001B[32m"

val successTekst =
    """
        
    ${BRIGHT_CYAN}Token Server $KUBE_CTL_CONTEXT_ER_ALLTID_DEV: [${GREEN}online$RESET]$RESET    
    ${BRIGHT_CYAN}Kafka UI:$RESET   http://localhost:4242/kafka    
    ${BRIGHT_CYAN}Hent token:$RESET http://localhost:4242/token/hag-lps-api-client
    

    """.trimIndent().prependIndent(" ")

val gcloudErrorTekst =
    """
    
    ${BRIGHT_RED}ERROR:$RESET${BRIGHT_CYAN} gcloud er ikke autentisert! $RESET
    
    ${GREEN}FIKS:$RESET$BRIGHT_CYAN  gcloud auth login $RESET
    
    
    """.trimIndent().prependIndent(" ")

val portBruktErrorTekst =
    """
    
    ${BRIGHT_RED}ERROR:$RESET${BRIGHT_CYAN} port 4242 allerede i bruk $RESET
        
    ${GREEN}FIKS:$RESET$BRIGHT_CYAN  kill ${'$'}(lsof -t -i:4242) $RESET
    
    
    """.trimIndent().prependIndent(" ")

fun printStartupMelding(text: String = successTekst) {
    val paddingWidth = 11
    val asciiWidth = 39

    val linjeAscii = "/".repeat(asciiWidth + paddingWidth * 2 + 2).fargelegg(ORANGE)
    val topSpacing = ".\n".repeat(4).fargelegg(ORANGE)

    val ulvAscii =
        """
            
     _____  ___  _____ 
    |  |  |/ _ \|  ,__|             .
    |__|__|__|__|_____/            / V\
 ____ ____ ___ ____   _____      / `  /
|    |    \   |    \ /    /     <<   |
|    |    /   |\    Y    /     /     |
|    |   /|   |_\__     /    /       |
|_______/ |_______/____/   /    \ \ /
                          (      )| |
                  ________|   _/_ | |
                <__________\_____)\__)
     
    HAG Utvikler Løsnings Verktøy

        """.trimIndent().fargelegg(ORANGE).prependIndent(" ".repeat(paddingWidth))

    (topSpacing + linjeAscii + ulvAscii + text + linjeAscii).printLinjer()
}

fun cleanServiceName(name: String): String = name.replace(Regex("^[^-]*-|(?:-[^-]*){4}$"), "")

fun giBrukerAdvarselBrukDev() {
    val os = System.getProperty("os.name").lowercase(Locale.getDefault())

    val command =
        when {
            os.contains("mac") ->
                listOf(
                    "osascript",
                    "-e",
                    "display alert \"🛑 STOP! 🛑\" message \"Programmet må bare brukes i DEV miljø!\nProgrammer avsluttes\" as critical",
                )
            os.contains("win") ->
                listOf("powershell", "-command", "[System.Windows.MessageBox]::Show('Programmet må bare brukes i DEV miljø!', 'STOP!')")
            else -> return
        }

    ProcessBuilder(command).start()
    exitProcess(0)
}

fun String.printLinjer() = lines().forEach { println(it).also { Thread.sleep(40) } }

fun String.fargelegg(farge: String) = lines().joinToString("\n") { farge + it + RESET }

fun gcloudErAutentisert(): Boolean {
    val process = ProcessBuilder("gcloud", "auth", "list", "--format=json").start()
    val output = process.inputStream.bufferedReader().readText()
    return output.contains("\"status\": \"ACTIVE\"")
}

fun erPortBrukt(port: String): Boolean {
    val command =
        when {
            System.getProperty("os.name").lowercase().contains("win") -> listOf("cmd", "/c", "netstat -an | findstr :$port")
            else -> listOf("sh", "-c", "lsof -i :$port || netstat -an | grep :$port")
        }
    val process = ProcessBuilder(command).start()
    val output = process.inputStream.bufferedReader().readText()
    return output.isNotBlank()
}
