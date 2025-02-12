package no.nav.helsearbeidsgiver

import io.ktor.client.*
import io.ktor.client.engine.apache5.Apache5
import io.ktor.network.sockets.*
import io.ktor.server.application.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

val client = HttpClient(Apache5)

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

fun Application.module() {
    configureRouting()

    environment.monitor.subscribe(ApplicationStarted) {
        val status = initialiserKubeCtlClient()

        if (status != KubeCtlStatus.SUCCESS) {
            exitProcess(0)
        }

        GlobalScope.launch {
            delay(1000)
            printStartupMelding(status)
        }
    }
}
