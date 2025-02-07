package no.nav.helsearbeidsgiver

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

fun Application.module() {
    configureRouting()

    environment.monitor.subscribe(ApplicationStarted) {
        println("Token server startet. Eksempel route:")
        println("http://localhost:4242/token/maskinporten-hag-lps-api-client")
    }
}
