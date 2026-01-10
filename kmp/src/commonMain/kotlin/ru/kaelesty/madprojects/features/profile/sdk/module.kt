package ru.kaelesty.madprojects.features.profile.sdk

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kaelesty.madprojects.features.profile.data.CommonProfileApi
import ru.kaelesty.madprojects.features.profile.data.JoinProjectApi
import ru.kaelesty.madprojects.features.profile.domain.GetCommonProfileUseCase
import ru.kaelesty.madprojects.features.profile.domain.GetUserProjectsUseCase
import ru.kaelesty.madprojects.features.profile.domain.JoinProjectUseCase
import ru.kaelesty.madprojects.features.profile.ui.CommonProfileViewModel
import ru.kaelesty.madprojects.navigation.NavItem

val profileModule = module {
    single { CommonProfileApi(get()) }
    single { JoinProjectApi(get()) }
    single { GetCommonProfileUseCase(get(), get()) }
    single { GetUserProjectsUseCase(get(), get()) }
    single { JoinProjectUseCase(get(), get()) }
    viewModel { CommonProfileViewModel(get(), get(), get()) }

    single { ProfileNavItem(get()) } binds arrayOf(
        NavItem::class,
        ProfileNavItem::class,
    )
}
