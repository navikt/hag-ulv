package no.nav.helsearbeidsgiver

object SecretsCache {
    private var secretsCache: MutableMap<String, KubeCtlSecret> = mutableMapOf()

    fun getValue(serviceName: String): KubeCtlSecret? {
        val navn = secretsCache.keys.find { it.contains(serviceName) } ?: return null
        return secretsCache[navn]
    }

    fun setValue(
        serviceName: String,
        secret: KubeCtlSecret,
    ) {
        secretsCache[serviceName] = secret
    }
}
