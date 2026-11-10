import ComposeApp
import Foundation
import SwiftUI
import UIKit

struct ComposeView: UIViewControllerRepresentable {
    let deepLinkUrl: String?
    let deepLinkEventId: Int

    final class Coordinator {
        let host = IosAppHost()
        var deliveredEventId: Int?
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIViewController(context: Context) -> UIViewController {
        deliverDeepLink(to: context.coordinator)
        return context.coordinator.host.viewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        deliverDeepLink(to: context.coordinator)
    }

    private func deliverDeepLink(to coordinator: Coordinator) {
        guard coordinator.deliveredEventId != deepLinkEventId else { return }
        coordinator.deliveredEventId = deepLinkEventId
        coordinator.host.updateDeepLink(url: deepLinkUrl)
    }
}

struct ContentView: View {
    let deepLinkUrl: String?
    let deepLinkEventId: Int

    var body: some View {
        ComposeView(deepLinkUrl: deepLinkUrl, deepLinkEventId: deepLinkEventId)
            .ignoresSafeArea()
    }
}
