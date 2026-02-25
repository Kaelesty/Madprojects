package ru.kaelesty.madprojects.features.project.sdk

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module
import org.koin.dsl.bind
import ru.kaelesty.madprojects.features.project.data.GithubActivityApi
import ru.kaelesty.madprojects.features.project.data.KanbanSocketClient
import ru.kaelesty.madprojects.features.project.data.MessengerSocketClient
import ru.kaelesty.madprojects.features.project.data.ProjectInfoApi
import ru.kaelesty.madprojects.features.project.data.ProjectKanbanApi
import ru.kaelesty.madprojects.features.project.data.ProjectSprintsApi
import ru.kaelesty.madprojects.features.project.domain.CreateKardChatUseCase
import ru.kaelesty.madprojects.features.project.domain.CreateSprintUseCase
import ru.kaelesty.madprojects.features.project.domain.FinishSprintUseCase
import ru.kaelesty.madprojects.features.project.domain.GetProjectKardsUseCase
import ru.kaelesty.madprojects.features.project.domain.GetProjectActivitiesUseCase
import ru.kaelesty.madprojects.features.project.domain.GetProjectSprintsUseCase
import ru.kaelesty.madprojects.features.project.domain.GetProjectUserCommitsAnalyticsUseCase
import ru.kaelesty.madprojects.features.project.domain.GetSprintUseCase
import ru.kaelesty.madprojects.features.project.domain.GithubActivityUseCase
import ru.kaelesty.madprojects.features.project.domain.KanbanSocket
import ru.kaelesty.madprojects.features.project.domain.MessengerSocket
import ru.kaelesty.madprojects.features.project.domain.ProjectInfoUseCase
import ru.kaelesty.madprojects.features.project.domain.UpdateSprintUseCase
import ru.kaelesty.madprojects.features.project.ui.ProjectKanbanViewModel
import ru.kaelesty.madprojects.features.project.ui.ProjectChatViewModel
import ru.kaelesty.madprojects.features.project.ui.ProjectInfoViewModel
import ru.kaelesty.madprojects.features.project.ui.ProjectMessengerViewModel
import ru.kaelesty.madprojects.features.project.ui.ProjectModerationViewModel
import ru.kaelesty.madprojects.features.project.ui.ProjectSocketViewModel
import ru.kaelesty.madprojects.features.project.ui.ProjectSprintCreateViewModel
import ru.kaelesty.madprojects.features.project.ui.ProjectSprintDetailsViewModel
import ru.kaelesty.madprojects.features.project.ui.ProjectSprintEditViewModel
import ru.kaelesty.madprojects.features.project.ui.ProjectSprintsViewModel
import ru.kaelesty.madprojects.features.project.ui.ProjectViewModel
import ru.kaelesty.madprojects.navigation.NavItem

val projectModule = module {
    single { GithubActivityApi(get()) }
    single { GithubActivityUseCase(get(), get()) }
    single { MessengerSocketClient(get(), get()) } bind MessengerSocket::class
    single { KanbanSocketClient(get(), get()) } bind KanbanSocket::class
    single { ProjectKanbanApi(get()) }
    single { ProjectInfoApi(get()) }
    single { ProjectInfoUseCase(get(), get(), get()) }
    single { CreateKardChatUseCase(get(), get()) }
    single { ProjectSprintsApi(get()) }
    single { GetProjectSprintsUseCase(get(), get()) }
    single { GetProjectActivitiesUseCase(get(), get()) }
    single { GetProjectUserCommitsAnalyticsUseCase(get(), get()) }
    single { GetSprintUseCase(get(), get()) }
    single { CreateSprintUseCase(get(), get()) }
    single { UpdateSprintUseCase(get(), get()) }
    single { FinishSprintUseCase(get(), get()) }
    single { GetProjectKardsUseCase(get(), get()) }
    viewModel { (projectId: String) -> ProjectViewModel(projectId, get()) }
    viewModel { (projectId: String) -> ProjectInfoViewModel(projectId, get()) }
    viewModel { (projectId: String) -> ProjectModerationViewModel(projectId, get()) }
    viewModel { (projectId: String) -> ProjectSocketViewModel(projectId, get()) }
    viewModel { (projectId: String) -> ProjectMessengerViewModel(projectId, get()) }
    viewModel { (projectId: String, chatId: Int) -> ProjectChatViewModel(projectId, chatId, get()) }
    viewModel { (projectId: String) -> ProjectKanbanViewModel(projectId, get(), get()) }
    viewModel { (projectId: String) -> ProjectSprintsViewModel(projectId, get(), get(), get()) }
    viewModel { (sprintId: String) -> ProjectSprintDetailsViewModel(sprintId, get(), get()) }
    viewModel { (projectId: String) -> ProjectSprintCreateViewModel(projectId, get(), get()) }
    viewModel { (projectId: String, sprintId: String, sprint: domain.sprints.SprintView) ->
        ProjectSprintEditViewModel(projectId, sprintId, sprint, get(), get())
    }

    single { ProjectNavItem(get()) } binds arrayOf(
        NavItem::class,
        ProjectNavItem::class,
    )
}
