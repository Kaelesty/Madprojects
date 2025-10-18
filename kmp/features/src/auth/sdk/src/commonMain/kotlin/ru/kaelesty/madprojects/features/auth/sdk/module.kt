package ru.kaelesty.madprojects.features.auth.sdk

import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kaelesty.madprojects.navigation.FeatureNavigation

val authModule = module {

    single {
        AuthNavigation()
    } binds arrayOf(
        FeatureNavigation::class,
        AuthNavigation::class,
    )

}