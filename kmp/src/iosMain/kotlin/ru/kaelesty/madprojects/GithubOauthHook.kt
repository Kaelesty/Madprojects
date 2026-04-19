package ru.kaelesty.madprojects

fun handleIncomingOauthUrl(url: String): Boolean {
    return IosAppBridge().handleOpenUrl(url)
}
