package no.nav.helsearbeidsgiver

import no.nav.helsearbeidsgiver.kubernetes.KUBE_CTL_CONTEXT_ER_ALLTID_DEV
import java.util.Locale
import kotlin.system.exitProcess

const val reset = "\u001B[0m"
const val orange = "\u001B[38;5;208m"
const val brightCyan = "\u001B[38;5;117m"

fun printStartupMelding() {
    val paddingWidth = 11
    val asciiWidth = 39

    val linjeAscii = "/".repeat(asciiWidth + paddingWidth * 2 + 2).fargelegg(orange) + "\n"
    val topSpacing = ".\n".repeat(4).fargelegg(orange)

    val ulvAscii =
        """
                                    .
                                   / V\
 ____ ____ ___ ____   _____      / `  /
|    |    \   |    \ /    /     <<   |
|    |    /   |\    Y    /     /     |
|    |   /|   |_\__     /    /       |
|_______/ |_______/____/   /    \ \ /
                          (      )| |
                  ________|   _/_ | |
                <__________\_____)\__)
     
    HAG Utvikler Løsnings Verktøy

        """.trimIndent().fargelegg(orange).prependIndent(" ".repeat(paddingWidth))

    val successTekst =
        """
            
        ${brightCyan}Token Server $KUBE_CTL_CONTEXT_ER_ALLTID_DEV: [online]$reset    
        
        ${brightCyan}Kafka UI:$reset   http://localhost:4242/kafka
                
        ${brightCyan}Hent token:$reset http://localhost:4242/token/hag-lps-api-client
        
        
        """.trimIndent().prependIndent(" ")

    (topSpacing + linjeAscii + ulvAscii + successTekst + linjeAscii).printLinjer()
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

fun String.printLinjer() = lines().forEach { println(it).also { Thread.sleep(70) } }

fun String.fargelegg(farge: String) = lines().joinToString("\n") { farge + it + reset }
