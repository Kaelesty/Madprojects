package ru.kaelesty.madprojects.features.profile.sdk

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kaelesty.madprojects.features.profile.data.CommonProfileApi
import ru.kaelesty.madprojects.features.profile.data.JoinProjectApi
import ru.kaelesty.madprojects.features.profile.domain.GetCommonProfileUseCase
import ru.kaelesty.madprojects.features.profile.domain.GetCuratorGroupProjectsUseCase
import ru.kaelesty.madprojects.features.profile.domain.GetCuratorProfileUseCase
import ru.kaelesty.madprojects.features.profile.domain.GetUserProjectsUseCase
import ru.kaelesty.madprojects.features.profile.domain.JoinProjectUseCase
import ru.kaelesty.madprojects.features.profile.domain.CreateProjectGroupUseCase
import ru.kaelesty.madprojects.features.profile.domain.DeleteProjectGroupUseCase
import ru.kaelesty.madprojects.features.profile.domain.UpdateCommonProfileUseCase
import ru.kaelesty.madprojects.features.profile.domain.UpdateCuratorProfileUseCase
import ru.kaelesty.madprojects.features.profile.ui.CommonProfileViewModel
import ru.kaelesty.madprojects.features.profile.ui.CuratorGroupViewModel
import ru.kaelesty.madprojects.features.profile.ui.CuratorProfileViewModel
import ru.kaelesty.madprojects.navigation.NavItem

val profileModule = module {
    single { CommonProfileApi(get()) }
    single { JoinProjectApi(get()) }
    single { GetCommonProfileUseCase(get(), get()) }
    single { GetCuratorGroupProjectsUseCase(get(), get()) }
    single { GetCuratorProfileUseCase(get(), get()) }
    single { GetUserProjectsUseCase(get(), get()) }
    single { JoinProjectUseCase(get(), get()) }
    single { CreateProjectGroupUseCase(get(), get()) }
    single { DeleteProjectGroupUseCase(get(), get()) }
    single { UpdateCommonProfileUseCase(get(), get()) }
    single { UpdateCuratorProfileUseCase(get(), get()) }
    viewModel { CommonProfileViewModel(get(), get(), get(), get(), get()) }
    viewModel { (groupId: String) -> CuratorGroupViewModel(groupId, get()) }
    viewModel { CuratorProfileViewModel(get(), get(), get(), get(), get()) }

    single { ProfileNavItem(get()) } binds arrayOf(
        NavItem::class,
        ProfileNavItem::class,
    )
}
