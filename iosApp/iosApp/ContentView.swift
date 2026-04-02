import ComposeApp
import Foundation
import SwiftUI
import UIKit

struct ComposeView: UIViewControllerRepresentable {
    let deepLinkUrl: String?

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(deepLinkUrl: deepLinkUrl)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

struct ContentView: View {
    let deepLinkUrl: String?
    let deepLinkEventId: Int

    var body: some View {
        ComposeView(deepLinkUrl: deepLinkUrl)
            .id(deepLinkEventId)
            .ignoresSafeArea()
    }
}
