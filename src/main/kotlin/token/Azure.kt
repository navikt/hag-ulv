package no.nav.helsearbeidsgiver.token

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.helsearbeidsgiver.client
import no.nav.helsearbeidsgiver.kubernetes.KubeSecret
import no.nav.helsearbeidsgiver.kubernetes.value

@Serializable
data class AzureTokenResponse(
//    val token_type: String,
//    val expires_in: Int,
//    val ext_expires_in: Int,
    val access_token: String,
)

val json = Json { ignoreUnknownKeys = true }

fun getAzureToken(
    secret: KubeSecret,
    scope: String?,
): String =
    runBlocking {
        val scopeString = scope ?: "dev-gcp.helsearbeidsgiver.spinosaurus"
        val response =
            client.post(secret.value("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("client_id", secret.value("AZURE_APP_CLIENT_ID"))
                            append("client_secret", secret.value("AZURE_APP_CLIENT_SECRET"))
                            append("scope", "api://$scopeString/.default")
                            append("grant_type", "client_credentials")
                        },
                    ),
                )
            }
        val body = response.bodyAsText()
        println("Fikk token response:\n" + body)
        val azureTokenResponse = json.decodeFromString<AzureTokenResponse>(body)
        azureTokenResponse.access_token
    }
