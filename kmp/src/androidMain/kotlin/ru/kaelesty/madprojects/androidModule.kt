package ru.kaelesty.madprojects

import org.koin.dsl.bind
import org.koin.dsl.module
import ru.kaelesty.madprojects.features.auth.data.storage.AuthStorage

val androidModule = module {
    single {
        AndroidAuthStorage(
            context = get()
        )
    } bind AuthStorage::class
}