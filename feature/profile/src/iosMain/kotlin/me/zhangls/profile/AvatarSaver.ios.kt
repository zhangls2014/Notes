package me.zhangls.profile

import com.mohamedrejeb.calf.io.KmpFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import me.zhangls.data.util.AppFileManager
import org.koin.core.annotation.Factory
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreGraphics.CGImageRelease
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.numberWithBool
import platform.Foundation.numberWithInt
import platform.ImageIO.CGImageSourceCreateThumbnailAtIndex
import platform.ImageIO.CGImageSourceCreateWithURL
import platform.ImageIO.kCGImageSourceCreateThumbnailFromImageAlways
import platform.ImageIO.kCGImageSourceCreateThumbnailWithTransform
import platform.ImageIO.kCGImageSourceThumbnailMaxPixelSize
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

private const val JPEG_QUALITY_SCALE = 100.0

@OptIn(ExperimentalForeignApi::class)
@Factory(binds = [AvatarStorage::class])
internal actual class AvatarSaver(private val fileManager: AppFileManager) : AvatarStorage {
  actual override suspend fun save(avatar: KmpFile): String? {
    val srcUrl = avatar.url
    val targetPath = fileManager.getImageFilePath(avatarFilename(fileManager.fileExtension(srcUrl)))
    var copied = false
    return try {
      copied = fileManager.copyPathToFile(srcUrl, targetPath)
      targetPath.takeIf { copied }
    } finally {
      if (!copied) fileManager.deleteFile(targetPath)
    }
  }

  actual override fun encodeForUpload(path: String): ByteArray {
    val resized = thumbnail(path)
    val jpeg = checkNotNull(UIImageJPEGRepresentation(resized, AVATAR_JPEG_QUALITY / JPEG_QUALITY_SCALE))
    val bounded = if (jpeg.length <= AVATAR_MAX_BYTES.toULong()) {
      jpeg
    } else {
      checkNotNull(UIImageJPEGRepresentation(resized, AVATAR_JPEG_FALLBACK_QUALITY / JPEG_QUALITY_SCALE))
    }
    return checkNotNull(bounded.bytes).readBytes(bounded.length.toInt())
  }

  private fun thumbnail(path: String): UIImage {
    val url = CFBridgingRetain(NSURL.fileURLWithPath(path))
    val source = try {
      checkNotNull(CGImageSourceCreateWithURL(url?.reinterpret(), null))
    } finally {
      CFRelease(url)
    }
    val options = CFBridgingRetain(
      mapOf(
        CFBridgingRelease(CFRetain(kCGImageSourceCreateThumbnailFromImageAlways)) to NSNumber.numberWithBool(true),
        CFBridgingRelease(CFRetain(kCGImageSourceCreateThumbnailWithTransform)) to NSNumber.numberWithBool(true),
        CFBridgingRelease(CFRetain(kCGImageSourceThumbnailMaxPixelSize)) to NSNumber.numberWithInt(AVATAR_SIDE_PX),
      ),
    )
    try {
      val image = checkNotNull(CGImageSourceCreateThumbnailAtIndex(source, 0uL, options?.reinterpret()))
      return try {
        UIImage.imageWithCGImage(image)
      } finally {
        CGImageRelease(image)
      }
    } finally {
      CFRelease(options)
      CFRelease(source)
    }
  }

  actual override fun storeJpeg(jpeg: ByteArray): String {
    val path = fileManager.getImageFilePath(avatarFilename("jpg"))
    val data = jpeg.usePinned { NSData.create(bytes = it.addressOf(0), length = jpeg.size.toULong()) }
    if (!NSFileManager.defaultManager.createFileAtPath(path, contents = data, attributes = null)) {
      fileManager.deleteFile(path)
      error("Unable to write avatar")
    }
    return path
  }

  actual override fun exists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)

  actual override suspend fun delete(path: String) {
    if (isManagedAvatar(path, fileManager.getImageDir())) fileManager.deleteFile(path)
  }
}
