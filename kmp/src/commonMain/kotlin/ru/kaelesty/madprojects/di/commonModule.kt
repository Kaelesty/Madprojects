package ru.kaelesty.madprojects.di

import org.koin.dsl.module
import ru.kaelesty.madprojects.features.auth.sdk.authModule
import ru.kaelesty.madprojects.features.checkin.sdk.checkInModule
import ru.kaelesty.madprojects.features.profile.sdk.profileModule
import ru.kaelesty.madprojects.features.projectcreate.sdk.projectCreateModule
import ru.kaelesty.madprojects.ktor.ktorModule

val commonModule = module {
    includes(
        ktorModule,
        authModule,
        checkInModule,
        profileModule,
        projectCreateModule,
    )
}
