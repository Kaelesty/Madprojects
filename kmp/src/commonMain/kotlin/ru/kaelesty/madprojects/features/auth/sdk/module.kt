package ru.kaelesty.madprojects.features.auth.sdk

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kaelesty.madprojects.features.auth.data.RegisterApi
import ru.kaelesty.madprojects.features.auth.domain.LoginUseCase
import ru.kaelesty.madprojects.features.auth.domain.RegisterUseCase
import ru.kaelesty.madprojects.features.auth.ui.LoginViewModel
import ru.kaelesty.madprojects.features.auth.ui.RegisterViewModel
import ru.kaelesty.madprojects.navigation.NavItem

val authModule = module {

    single {
        LoginUseCase()
    }

    single {
        RegisterApi(get())
    }

    single {
        RegisterUseCase(get())
    }

    viewModel {
        LoginViewModel(
            useCase = get(),
        )
    }

    viewModel {
        RegisterViewModel(
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

