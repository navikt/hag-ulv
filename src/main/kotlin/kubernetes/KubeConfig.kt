package no.nav.helsearbeidsgiver.kubernetes

import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.util.Config
import io.kubernetes.client.util.KubeConfig
import java.io.File
import java.io.FileReader
import java.util.*

// MÅ ALLTID VÆRE DEV-GCP
// MÅ ALLTID VÆRE DEV-GCP
// MÅ ALLTID VÆRE DEV-GCP
val KUBE_CTL_CONTEXT_ER_ALLTID_DEV = "dev-gcp"
// MÅ ALLTID VÆRE DEV-GCP
// MÅ ALLTID VÆRE DEV-GCP
// MÅ ALLTID VÆRE DEV-GCP

fun hentDevGcpKubeConfig(): ApiClient {
    val kubeConfigFile = findConfigInHomeDir() ?: throw Exception("KubeConfigFile ikke funnet")

    val reader = FileReader(kubeConfigFile)
    val kubeConfig = KubeConfig.loadKubeConfig(reader)

    kubeConfig.setContext(KUBE_CTL_CONTEXT_ER_ALLTID_DEV)
    println("KubeCtl kontekst er satt til: $KUBE_CTL_CONTEXT_ER_ALLTID_DEV")

    val apiClient = Config.fromConfig(kubeConfig)
    return apiClient
}

// -----------------------------------------------------------------------
//           Denne koden er tatt fra Kubernetes Java klient
//           Repo: https://github.com/kubernetes-client/java
//           Lisens: Apache License Version 2.0
// ------------------------------------------------------------------------

fun findHomeDir(): File? {
    val envHome = System.getenv("HOME")
    if (envHome != null && envHome.length > 0) {
        val config = File(envHome)
        if (config.exists()) {
            return config
        }
    }

    if (System.getProperty("os.name").lowercase(Locale.getDefault()).startsWith("windows")) {
        val homeDrive = System.getenv("HOMEDRIVE")
        val homePath = System.getenv("HOMEPATH")
        if (homeDrive != null && homeDrive.length > 0 && homePath != null && homePath.length > 0) {
            val homeDir = File(File(homeDrive), homePath)
            if (homeDir.exists()) {
                return homeDir
            }
        }

        val userProfile = System.getenv("USERPROFILE")
        if (userProfile != null && userProfile.length > 0) {
            val profileDir = File(userProfile)
            if (profileDir.exists()) {
                return profileDir
            }
        }
    }

    return null
}

fun findConfigInHomeDir(): File? {
    val homeDir = findHomeDir()
    if (homeDir != null) {
        val config = File(File(homeDir, ".kube"), "config")
        if (config.exists()) {
            return config
        }
    }
    println("Could not find ~/.kube/config")
    return null
}
