package me.zhangls.about

import kotlinx.coroutines.flow.Flow

internal expect class UpdateInstaller {
  fun observeDownload(apkUrl: String): Flow<DownloadStatus>
  suspend fun hasOlderDownload(apkUrl: String): Boolean
  suspend fun enqueue(apkUrl: String): Boolean
  suspend fun install(apkUrl: String, manifest: UpdateManifest): InstallResult
}

internal enum class DownloadStatus { Idle, Downloading, Ready, Failed }

internal enum class InstallResult { HandedOff, PermissionRequired, Failed }
