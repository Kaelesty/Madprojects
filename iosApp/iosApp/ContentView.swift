import SwiftUI
import UIKit
import MadprojectsShared

struct ContentView: View {
    var body: some View {
        ComposeViewController()
            .ignoresSafeArea()
    }
}

private struct ComposeViewController: UIViewControllerRepresentable {
    private let bridge = IosAppBridge()

    func makeUIViewController(context: Context) -> UIViewController {
        bridge.mainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

#Preview {
    ContentView()
}
