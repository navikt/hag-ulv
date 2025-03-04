package no.nav.helsearbeidsgiver

object SecretsCache {
    private var secretsCache: MutableMap<String, KubeSecret> = mutableMapOf()

    fun getValue(serviceName: String): KubeSecret? {
        val navn = secretsCache.keys.find { it.contains(serviceName) } ?: return null
        return secretsCache[navn]
    }

    fun getValue(
        secretType: SecretType,
        serviceName: String,
    ): KubeSecret? {
        val navn = secretsCache.keys.find { it.contains(serviceName) && it.contains(secretType.getNameString()) } ?: return null
        return secretsCache[navn]
    }

    fun setValue(
        serviceName: String,
        secret: KubeSecret,
    ) {
        secretsCache[serviceName] = secret
    }
}
