package app.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Config(
    val github: Github,
    val ssl: Ssl,
    val auth: Auth,
    val db: Db
) {

    @Serializable
    data class Db(
        val url: String,
        val user: String,
        val driver: String,
        val password: String,
    )

    @Serializable
    data class Github(
        val clientSecret: String,
        val clientId: String,
        val webProfileRedirectUrl: String? = null,
        val androidProfileRedirectUrl: String? = null,
        val iosProfileRedirectUrl: String? = null,
        val oauthSessionTtlSeconds: Long = 600L,
    )

    @Serializable
    data class Ssl(
        val domain: String,
        val certificatePassword: String,
        val certificateAlias: String,
    )

    @Serializable
    data class Auth(
        val jwtSecret: String,
    )

    companion object {

        fun load(): Config {
            val file = File("src/main/resources/config.json")
            val jsonString = file.readText()
            return Json.decodeFromString(jsonString)
        }

    }
}
