package ru.kaelesty.madprojects.features.checkin.sdk

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kaelesty.madprojects.features.checkin.data.CheckInApi
import ru.kaelesty.madprojects.features.checkin.domain.CheckInUseCase
import ru.kaelesty.madprojects.features.checkin.ui.CheckInViewModel
import ru.kaelesty.madprojects.navigation.NavItem

val checkInModule = module {
    single { CheckInApi(get()) }
    single { CheckInUseCase(get()) }
    viewModel { CheckInViewModel(get()) }

    single { CheckInNavItem() } binds arrayOf(
        NavItem::class,
        CheckInNavItem::class,
    )
}
