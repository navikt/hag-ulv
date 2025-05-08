package no.nav.helsearbeidsgiver

import no.nav.helsearbeidsgiver.kubernetes.KUBE_CTL_CONTEXT_ER_ALLTID_DEV
import java.util.Locale
import kotlin.system.exitProcess

fun printStartupMelding() {
    val reset = "\u001B[0m"
    val orange = "\u001B[38;5;208m"
    val brightCyan = "\u001B[38;5;117m"

    val paddingWidth = 11
    val asciiWidth = 39

    val linjeAscii = orange + "/".repeat(asciiWidth + paddingWidth * 2 + 2) + reset

    println("\n\n")
    println(linjeAscii)
    println(
        """
        $orange
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
        $reset
        """.trimIndent().prependIndent(" ".repeat(paddingWidth)),
    )
    val successTekst =
        """
        ${brightCyan}Token Server $KUBE_CTL_CONTEXT_ER_ALLTID_DEV: [online]$reset    
        
        ${brightCyan}Kafka UI:$reset   http://localhost:4242/kafka
                
        ${brightCyan}Hent token:$reset http://localhost:4242/token/hag-lps-api-client
        
        """.trimIndent()
    println(successTekst.prependIndent(" "))
    println(linjeAscii)
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
