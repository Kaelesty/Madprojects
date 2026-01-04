package ru.kaelesty.madprojects.features.auth.sdk

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kaelesty.madprojects.features.auth.data.AuthContextImpl
import ru.kaelesty.madprojects.features.auth.data.AuthControllerImpl
import ru.kaelesty.madprojects.features.auth.data.LoginApi
import ru.kaelesty.madprojects.features.auth.data.RegisterApi
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.auth.domain.AuthController
import ru.kaelesty.madprojects.features.auth.domain.LoginUseCase
import ru.kaelesty.madprojects.features.auth.domain.RegisterUseCase
import ru.kaelesty.madprojects.features.auth.ui.LoginViewModel
import ru.kaelesty.madprojects.features.auth.ui.RegisterViewModel
import ru.kaelesty.madprojects.navigation.NavItem
import kotlin.math.cos

val authModule = module {

    single {
        AuthContextImpl()
    } binds arrayOf(
        AuthContext::class,
        AuthContextImpl::class,
    )

    single {
        AuthControllerImpl(
            contextImpl = get()
        )
    } bind AuthController::class

    single {
        LoginApi(get())
    }

    single {
        LoginUseCase(get())
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

