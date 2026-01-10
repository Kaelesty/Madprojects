package ru.kaelesty.madprojects.features.projectcreate.sdk

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kaelesty.madprojects.features.projectcreate.data.CreateProjectApi
import ru.kaelesty.madprojects.features.projectcreate.data.CuratorsApi
import ru.kaelesty.madprojects.features.projectcreate.data.ProjectGroupsApi
import ru.kaelesty.madprojects.features.projectcreate.data.VerifyRepoLinkApi
import ru.kaelesty.madprojects.features.projectcreate.domain.CreateProjectUseCase
import ru.kaelesty.madprojects.features.projectcreate.domain.GetCuratorGroupsUseCase
import ru.kaelesty.madprojects.features.projectcreate.domain.GetCuratorsUseCase
import ru.kaelesty.madprojects.features.projectcreate.domain.VerifyRepoLinkUseCase
import ru.kaelesty.madprojects.features.projectcreate.ui.CreateProjectViewModel
import ru.kaelesty.madprojects.navigation.NavItem

val projectCreateModule = module {
    single { CreateProjectApi(get()) }
    single { CuratorsApi(get()) }
    single { ProjectGroupsApi(get()) }
    single { VerifyRepoLinkApi(get()) }
    single { CreateProjectUseCase(get(), get()) }
    single { GetCuratorsUseCase(get(), get()) }
    single { GetCuratorGroupsUseCase(get(), get()) }
    single { VerifyRepoLinkUseCase(get(), get()) }
    viewModel { CreateProjectViewModel(get(), get(), get(), get()) }

    single { ProjectCreateNavItem() } binds arrayOf(
        NavItem::class,
        ProjectCreateNavItem::class,
    )
}
