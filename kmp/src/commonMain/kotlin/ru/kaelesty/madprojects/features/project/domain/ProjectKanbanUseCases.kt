package ru.kaelesty.madprojects.features.project.domain

import io.ktor.http.HttpStatusCode
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.project.data.ProjectKanbanApi
import ru.kaelesty.madprojects.utils.KLogger

class CreateKardChatUseCase(
    private val api: ProjectKanbanApi,
    private val authContext: AuthContext,
) {
    suspend fun create(projectId: String, kardId: Int): Result {
        KLogger.d(TAG) { "create kard chat start: projectId=$projectId kardId=$kardId" }
        val accessToken = authContext.getAccessToken() ?: run {
            KLogger.w(TAG) { "create kard chat failed: access token missing" }
            return Result.Fail(null, null)
        }
        val response = api.createKardChat(accessToken, projectId, kardId) ?: run {
            KLogger.w(TAG) { "create kard chat failed: response is null" }
            return Result.Fail(null, null)
        }
        if (response.status == HttpStatusCode.OK) {
            val chatId = response.chatId ?: run {
                KLogger.w(TAG) { "create kard chat failed: chatId missing" }
                return Result.Fail(response.status, response.errorMessage)
            }
            KLogger.i(TAG) { "create kard chat success: chatId=$chatId" }
            return Result.Success(chatId)
        }
        KLogger.w(TAG) { "create kard chat failed: status=${response.status} message=${response.errorMessage}" }
        return Result.Fail(response.status, response.errorMessage)
    }

    sealed interface Result {
        data class Success(val chatId: String) : Result
        data class Fail(val status: HttpStatusCode?, val message: String?) : Result
    }

    private companion object {
        private const val TAG = "CreateKardChatUseCase"
    }
}
