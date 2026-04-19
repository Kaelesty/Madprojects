# iOS GitHub OAuth Callback Hook

This KMP module exposes a public function for OAuth callback forwarding:

`handleIncomingOauthUrl(url: String): Boolean`

## Required iOS host setup

1. Add URL Type with scheme `madprojects-ios` in the iOS app target.
2. Forward incoming callback URLs from `AppDelegate` or `SceneDelegate` to KMP:

```swift
import Shared

func application(
    _ app: UIApplication,
    open url: URL,
    options: [UIApplication.OpenURLOptionsKey : Any] = [:]
) -> Bool {
    // Generated top-level bridge from kmp/src/iosMain/.../GithubOauthHook.kt
    return GithubOauthHookKt.handleIncomingOauthUrl(url: url.absoluteString)
}
```

Scene-based apps can call the same bridge from:

`scene(_:openURLContexts:)`

## Expected callback format

`madprojects-ios://profile?provider=github&status=success`

or

`madprojects-ios://profile?provider=github&status=error&reason=...`
