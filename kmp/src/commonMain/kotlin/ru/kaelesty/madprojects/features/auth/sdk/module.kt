package ru.kaelesty.madprojects.features.auth.sdk

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kaelesty.madprojects.features.auth.data.AuthContextImpl
import ru.kaelesty.madprojects.features.auth.data.api.LoginApi
import ru.kaelesty.madprojects.features.auth.data.api.RefreshApi
import ru.kaelesty.madprojects.features.auth.data.api.RegisterApi
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.auth.domain.LoginUseCase
import ru.kaelesty.madprojects.features.auth.domain.RefreshUseCase
import ru.kaelesty.madprojects.features.auth.domain.RegisterUseCase
import ru.kaelesty.madprojects.features.auth.domain.StartGithubOauthUseCase
import ru.kaelesty.madprojects.features.auth.ui.LoginViewModel
import ru.kaelesty.madprojects.features.auth.ui.RegisterViewModel
import ru.kaelesty.madprojects.navigation.NavItem

val authModule = module {

    single(createdAtStart = true) {
        AuthContextImpl(
            refreshUseCase = get(),
            storage = get(),
        )
    } binds arrayOf(
        AuthContext::class,
        AuthContextImpl::class,
    )

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

    single {
        RefreshApi(get())
    }

    single {
        RefreshUseCase(get())
    }

    single {
        StartGithubOauthUseCase(get())
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
