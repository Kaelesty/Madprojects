package ru.kaelesty.madprojects

import org.koin.dsl.bind
import org.koin.dsl.module
import ru.kaelesty.madprojects.features.auth.data.storage.AuthStorage

val iosModule = module {
    single {
        AppleAuthStorage()
    } bind AuthStorage::class
}
