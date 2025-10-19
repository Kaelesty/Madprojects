package ru.kaelesty.madprojects.di

import org.koin.dsl.module
import ru.kaelesty.madprojects.features.auth.sdk.authModule

val commonModule = module {
    includes(
        authModule,
    )
}