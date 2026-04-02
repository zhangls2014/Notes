import ComposeApp
import Foundation
import SwiftUI

@main
struct iOSApp: App {
    @State private var deepLinkUrl: String?
    @State private var deepLinkEventId: Int = 0

    init() {
        NotesModuleKt.doInitKoin()
        NotesModuleKt.doInitData()
    }

    var body: some Scene {
        WindowGroup {
            ContentView(
                deepLinkUrl: deepLinkUrl,
                deepLinkEventId: deepLinkEventId
            )
            .onOpenURL { url in
                handleDeepLink(url)
            }
            .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                if let url = activity.webpageURL {
                    handleDeepLink(url)
                }
            }
        }
    }

    private func handleDeepLink(_ url: URL) {
        deepLinkUrl = url.absoluteString
        deepLinkEventId += 1
    }
}
