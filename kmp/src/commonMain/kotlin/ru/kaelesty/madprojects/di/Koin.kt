package ru.kaelesty.madprojects.di

import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

private var koinApplication: KoinApplication? = null

fun initKoin(
    appDeclaration: KoinAppDeclaration = {},
    platformModules: List<Module> = emptyList(),
): Koin {
    koinApplication?.let { return it.koin }

    val application = startKoin {
        appDeclaration()
        modules(listOf(commonModule) + platformModules)
    }
    koinApplication = application
    return application.koin
}

fun getKoin(): Koin {
    return koinApplication?.koin ?: error("Koin has not been initialized")
}
