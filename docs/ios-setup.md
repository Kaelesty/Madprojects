# iOS setup

The repository now contains a minimal Xcode host app in `iosApp/` and a static Kotlin framework exported from `:kmp`.

## Open on macOS

1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. Let the build phase run `./gradlew :kmp:embedAndSignAppleFrameworkForXcode`.
3. Choose an iPhone simulator or a connected device and run the `iosApp` scheme.

## Repository expectations

- Do not commit `local.properties`.
- Do not set `org.gradle.java.home` to a machine-specific path in `gradle.properties`.
- Xcode must be installed and selected with `xcode-select`.

## Android Studio / IntelliJ

The shared iOS code lives in `:kmp`, but the runnable iOS target is the native host app under `iosApp/`.
If the IDE does not show an iOS run configuration, open the Xcode project directly and run from Xcode.
