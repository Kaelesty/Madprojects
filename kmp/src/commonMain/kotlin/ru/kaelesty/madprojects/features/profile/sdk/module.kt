package ru.kaelesty.madprojects.features.profile.sdk

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kaelesty.madprojects.features.profile.data.CommonProfileApi
import ru.kaelesty.madprojects.features.profile.domain.GetCommonProfileUseCase
import ru.kaelesty.madprojects.features.profile.ui.CommonProfileViewModel
import ru.kaelesty.madprojects.navigation.NavItem

val profileModule = module {
    single { CommonProfileApi(get()) }
    single { GetCommonProfileUseCase(get(), get()) }
    viewModel { CommonProfileViewModel(get()) }

    single { ProfileNavItem(get()) } binds arrayOf(
        NavItem::class,
        ProfileNavItem::class,
    )
}
