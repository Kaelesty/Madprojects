package app

import app.config.Config
import app.features.featuresModule
import app.openapi.SwaggerDocsConfig
import app.openapi.model.OpenApiSecurityScheme
import app.openapi.model.OpenApiServer
import app.plugins.pluginsModule
import app.smtp.smtpModule
import org.koin.dsl.module
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val appModule = module {

    includes(featuresModule, pluginsModule, smtpModule)

    single<HttpClient> {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    single<Config> {
        Config.load()
    }

    single<SwaggerDocsConfig> {
        val config = get<Config>()
        SwaggerDocsConfig(
            title = "Madprojects API",
            version = "2.0.0",
            description = "Internal OpenAPI registry generated from explicit annotations.",
            servers = listOf(
                OpenApiServer(url = "https://${config.ssl.domain}:8080"),
                OpenApiServer(url = "http://localhost:8079"),
            ),
            securitySchemes = mapOf(
                "auth-jwt" to OpenApiSecurityScheme(
                    type = "http",
                    scheme = "bearer",
                    bearerFormat = "JWT",
                    description = "JWT bearer token used by authenticated backend endpoints.",
                )
            ),
        )
    }

    single<GithubTokenUtil> {
        GithubTokenUtil(
            githubTokensRepo = get(),
            httpClient = get(),
            config = get(),
        )
    }
}
