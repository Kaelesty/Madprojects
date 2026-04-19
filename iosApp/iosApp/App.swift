import SwiftUI
import MadprojectsShared

@main
struct iosAppApp: App {
    private let bridge = IosAppBridge()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    _ = bridge.handleOpenUrl(url: url.absoluteString)
                }
        }
    }
}
