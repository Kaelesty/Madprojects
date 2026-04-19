package ru.kaelesty.madprojects

import ru.kaelesty.madprojects.features.auth.domain.GithubOauthBridge

fun handleIncomingOauthUrl(url: String): Boolean {
    return GithubOauthBridge.handleIncomingUrl(url)
}
