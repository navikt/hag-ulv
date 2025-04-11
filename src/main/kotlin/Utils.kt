package no.nav.helsearbeidsgiver

import no.nav.helsearbeidsgiver.kubernetes.KUBE_CTL_CONTEXT_ER_ALLTID_DEV
import java.io.File
import java.util.Locale
import kotlin.system.exitProcess

fun printStartupMelding() {
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

    val successTekst = """
${green}Server online med context: [$KUBE_CTL_CONTEXT_ER_ALLTID_DEV]$reset    
        
${green}${bold}Hent token:$reset    
  http://localhost:4242/token/maskinporten-hag-lps-api-client
      
${green}${bold}Kafka UI:$reset
http://localhost:4242/kafka"""

    println("\n".repeat(2))
    println(blue + line + reset)
    print(bold + cyan + centerText("🔑 Maskinporten Token Server 🔑") + reset)

    println(successTekst)

    println(blue + line + reset)
}

fun resolveAbsolutePath(path: String): String {
    val baseDir =
        File(
            object {}
                .javaClass.protectionDomain.codeSource.location
                .toURI(),
        )

    // Going up from build level
    val repoRootDir = baseDir.parentFile.parentFile.parentFile.parentFile

    return File(repoRootDir, path).canonicalFile.absolutePath
}

fun lagTempFil(
    fileName: String,
    content: ByteArray,
): String {
    val suffix = "." + fileName.substringAfterLast('.', "tmp")
    val tempFile = File.createTempFile(fileName.substringBeforeLast('.'), suffix)
    tempFile.writeBytes(content)
    tempFile.deleteOnExit()
    return tempFile.absolutePath
}

fun lagTempFil(
    fileName: String,
    content: String,
): String = lagTempFil(fileName, content.encodeToByteArray())

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
