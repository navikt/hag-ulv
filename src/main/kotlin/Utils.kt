package no.nav.helsearbeidsgiver

import no.nav.helsearbeidsgiver.kubernetes.KUBE_CTL_CONTEXT

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

//    fun KubeCtlStatus.feilTekst(): String =
//        when {
//            this == KubeCtlStatus.UNAUTHORIZED -> "Ikke autorisert - Logg på med: ${bold}${green}gcloud auth login$reset"
//            this == KubeCtlStatus.TIMEOUT -> "Timeout mot kubectl - Er du pålogget med ${bold}${green}NAIS$reset?"
//            else -> "Ukjent feil - Se på logs for flere detaljer"
//        }

//    val feilTekst = """
// $red🛑 Serveren ble ikke startet$reset
//
// ${red}${bold}Feil ved oppstart:$reset
//   ${status.feilTekst()}"""

    val successTekst = """
${green}Server online med context: [$KUBE_CTL_CONTEXT]$reset    
        
${green}${bold}Hent token:$reset    
  http://localhost:4242/token/maskinporten-hag-lps-api-client
      
${green}${bold}Swagger:$reset
  http://localhost:4242/swagger"""

    println("\n".repeat(2))
    println(blue + line + reset)
    print(bold + cyan + centerText("🔑 Maskinporten Token Server 🔑") + reset)

//    println(if (status == KubeCtlStatus.SUCCESS) successTekst else feilTekst)
    println(successTekst)

    print(blue + line + reset)
}
