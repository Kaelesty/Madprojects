package ru.kaelesty.madprojects.features.auth.domain

enum class GithubOauthChannel(val value: String) {
    Android("android"),
    Ios("ios"),
}

internal expect fun resolveGithubOauthChannel(): GithubOauthChannel
