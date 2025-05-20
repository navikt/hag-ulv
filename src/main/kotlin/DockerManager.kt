package no.nav.helsearbeidsgiver

import java.io.BufferedReader
import java.io.InputStreamReader

var containerId: String? = null

object DockerManager {
    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                stopContainer(containerId)
            },
        )
    }

    fun startContainer(cmd: List<String>): String {
        println("Lager docker container med kommando: ")
        println(cmd.joinToString(" "))
        stopContainer(containerId)

        val process = ProcessBuilder(cmd).start()
        process.waitFor()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val containerId = reader.readLine()?.trim()
        reader.close()

        if (containerId.isNullOrBlank()) {
            throw RuntimeException("Feilet å hente container ID")
        }

        streamDockerLogs(containerId)

        no.nav.helsearbeidsgiver.containerId = containerId
        return containerId
    }

    private fun stopContainer(id: String?): Boolean {
        val idString = id ?: return false

        var process = ProcessBuilder("docker", "kill", idString).start()
        process.waitFor()
        val killSuccess = process.exitValue() == 0

        process = ProcessBuilder("docker", "rm", idString).start()
        process.waitFor()

        return killSuccess && (process.exitValue() == 0)
    }
}

fun imageEksisterer(imageNavn: String): Boolean {
    val process =
        ProcessBuilder("docker", "image", "inspect", imageNavn)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    return process.waitFor() == 0
}

fun daemonIsRunning(): Boolean {
    val process =
        ProcessBuilder("docker", "info")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    return process.waitFor() == 0
}

fun streamDockerLogs(containerId: String) {
    val process =
        ProcessBuilder("docker", "logs", "-f", containerId)
            .apply {
                redirectErrorStream(true) // Merge stderr into stdout
            }.start()

    Thread {
        process.inputStream.bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                println("[Docker] $line")
            }
        }
    }.start()
}
