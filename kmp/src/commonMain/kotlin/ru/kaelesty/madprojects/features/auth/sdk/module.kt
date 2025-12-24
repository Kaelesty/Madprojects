package ru.kaelesty.madprojects.features.auth.sdk

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kaelesty.madprojects.features.domain.LoginUseCase
import ru.kaelesty.madprojects.features.login.LoginViewModel
import ru.kaelesty.madprojects.navigation.NavItem

val authModule = module {

    single {
        LoginUseCase()
    }

    viewModel {
        LoginViewModel(
            useCase = get(),
        )
    }

    single {
        AuthNavItem()
    } binds arrayOf(
        NavItem::class,
        AuthNavItem::class,
    )

}

