package ru.kaelesty.madprojects.features.auth.domain

import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.kaelesty.madprojects.utils.KLogger
import ru.kaelesty.madprojects.utils.nowMillis

data class GithubOauthResult(
    val status: Status,
    val reason: String?,
    val eventId: Long = nowMillis(),
) {
    enum class Status {
        Success,
        Error,
    }
}

object GithubOauthBridge {

    private val _events = MutableSharedFlow<GithubOauthResult>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private val _pendingResult = MutableStateFlow<GithubOauthResult?>(null)
    val pendingResult = _pendingResult.asStateFlow()

    fun handleIncomingUrl(rawUrl: String): Boolean {
        val parsed = parseResult(rawUrl) ?: run {
            KLogger.d(TAG) { "handleIncomingUrl ignored: not GitHub OAuth callback" }
            return false
        }
        _pendingResult.value = parsed
        _events.tryEmit(parsed)
        KLogger.i(TAG) { "handleIncomingUrl accepted: status=${parsed.status} reason=${parsed.reason}" }
        return true
    }

    fun consumePendingResult(): GithubOauthResult? {
        val result = _pendingResult.value
        _pendingResult.value = null
        return result
    }

    private fun parseResult(rawUrl: String): GithubOauthResult? {
        val url = runCatching { Url(rawUrl) }
            .getOrElse { error ->
                KLogger.e(TAG, error) { "parseResult failed: invalid url" }
                return null
            }

        val provider = url.parameters[ProviderParam]
            ?.lowercase()
            ?: return null
        if (provider != GithubProviderValue) return null

        val status = when (url.parameters[StatusParam]?.lowercase()) {
            SuccessValue -> GithubOauthResult.Status.Success
            ErrorValue -> GithubOauthResult.Status.Error
            else -> return null
        }
        val reason = url.parameters[ReasonParam]?.takeIf { it.isNotBlank() }
        return GithubOauthResult(status = status, reason = reason)
    }

    private const val TAG = "GithubOauthBridge"
    private const val ProviderParam = "provider"
    private const val StatusParam = "status"
    private const val ReasonParam = "reason"
    private const val GithubProviderValue = "github"
    private const val SuccessValue = "success"
    private const val ErrorValue = "error"
}
