package ru.kaelesty.madprojects.features.project.sdk

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module
import ru.kaelesty.madprojects.features.project.data.GithubActivityApi
import ru.kaelesty.madprojects.features.project.domain.GithubActivityUseCase
import ru.kaelesty.madprojects.features.project.ui.ProjectViewModel
import ru.kaelesty.madprojects.navigation.NavItem

val projectModule = module {
    single { GithubActivityApi(get()) }
    single { GithubActivityUseCase(get(), get()) }
    viewModel { (projectId: String) -> ProjectViewModel(projectId, get()) }

    single { ProjectNavItem(get()) } binds arrayOf(
        NavItem::class,
        ProjectNavItem::class,
    )
}
