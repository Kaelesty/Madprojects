package ru.kaelesty.madprojects.ktor

import io.ktor.client.HttpClient
import org.koin.dsl.module

val ktorModule = module {
    single<HttpClient> {
        createKtorClient()
    }
}
