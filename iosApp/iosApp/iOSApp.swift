import ComposeApp
import Foundation
import SwiftUI
import UIKit

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(DownloadAppDelegate.self) private var downloadAppDelegate
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

private final class DownloadAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping @Sendable () -> Void
    ) {
        WebAssetDownloadEventsKt.onWebAssetDownloadEvents(
            identifier: identifier,
            completion: completionHandler
        )
    }
}
