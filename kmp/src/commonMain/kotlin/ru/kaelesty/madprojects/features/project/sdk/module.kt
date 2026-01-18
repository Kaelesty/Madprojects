package ru.kaelesty.madprojects.features.project.sdk

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.binds
import org.koin.dsl.module
import org.koin.dsl.bind
import ru.kaelesty.madprojects.features.project.data.GithubActivityApi
import ru.kaelesty.madprojects.features.project.data.MessengerSocketClient
import ru.kaelesty.madprojects.features.project.domain.GithubActivityUseCase
import ru.kaelesty.madprojects.features.project.domain.MessengerSocket
import ru.kaelesty.madprojects.features.project.ui.ProjectChatViewModel
import ru.kaelesty.madprojects.features.project.ui.ProjectMessengerViewModel
import ru.kaelesty.madprojects.features.project.ui.ProjectSocketViewModel
import ru.kaelesty.madprojects.features.project.ui.ProjectViewModel
import ru.kaelesty.madprojects.navigation.NavItem

val projectModule = module {
    single { GithubActivityApi(get()) }
    single { GithubActivityUseCase(get(), get()) }
    single { MessengerSocketClient(get(), get()) } bind MessengerSocket::class
    viewModel { (projectId: String) -> ProjectViewModel(projectId, get()) }
    viewModel { (projectId: String) -> ProjectSocketViewModel(projectId, get()) }
    viewModel { (projectId: String) -> ProjectMessengerViewModel(projectId, get()) }
    viewModel { (projectId: String, chatId: Int) -> ProjectChatViewModel(projectId, chatId, get()) }

    single { ProjectNavItem(get()) } binds arrayOf(
        NavItem::class,
        ProjectNavItem::class,
    )
}
