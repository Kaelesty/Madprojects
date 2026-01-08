package ru.kaelesty.madprojects.features.auth.sdk

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kaelesty.madprojects.features.auth.data.AuthContextImpl
import ru.kaelesty.madprojects.features.auth.data.AuthControllerImpl
import ru.kaelesty.madprojects.features.auth.data.api.LoginApi
import ru.kaelesty.madprojects.features.auth.data.api.RegisterApi
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.auth.domain.AuthController
import ru.kaelesty.madprojects.features.auth.domain.LoginUseCase
import ru.kaelesty.madprojects.features.auth.domain.RegisterUseCase
import ru.kaelesty.madprojects.features.auth.ui.LoginViewModel
import ru.kaelesty.madprojects.features.auth.ui.RegisterViewModel
import ru.kaelesty.madprojects.navigation.NavItem

val authModule = module {

    single {
        AuthContextImpl()
    } binds arrayOf(
        AuthContext::class,
        AuthContextImpl::class,
    )

    single(createdAtStart = true) {
        AuthControllerImpl(
            contextImpl = get(),
            storage = get(),
        )
    } bind AuthController::class

    single {
        LoginApi(get())
    }

    single {
        LoginUseCase(get(), get())
    }

    single {
        RegisterApi(get())
    }

    single {
        RegisterUseCase(get(), get())
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
