package no.nav.helsearbeidsgiver

import io.ktor.client.*
import io.ktor.client.engine.apache5.Apache5
import io.ktor.server.application.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.helsearbeidsgiver.kubernetes.KUBE_CTL_CONTEXT_ER_ALLTID_DEV
import kotlin.system.exitProcess

val client = HttpClient(Apache5)

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

fun Application.module() {
    if (KUBE_CTL_CONTEXT_ER_ALLTID_DEV != "dev-gcp") {
        giBrukerAdvarselBrukDev()
        exitProcess(0)
    }

    val portErBrukt = erPortBrukt(environment.config.property("ktor.deployment.port").getString())

    configureRouting()

    environment.monitor.subscribe(ApplicationStarted) {
        GlobalScope.launch {
            delay(1000)
            if (portErBrukt) {
                printStartupMelding(portBruktErrorTekst)
                exitProcess(0)
            } else if (!gcloudErAutentisert()) {
                printStartupMelding(gcloudErrorTekst)
                exitProcess(0)
            } else {
                printStartupMelding(successTekst)
            }
        }
    }
}
