package me.zhangls.profile

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import com.mohamedrejeb.calf.io.KmpFile
import java.io.File
import kotlinx.coroutines.runBlocking
import me.zhangls.data.util.AppFileManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class AvatarSaverTest {
  private val files = AppFileManager(RuntimeEnvironment.getApplication())
  private val saver = AvatarSaver(files)

  @Test fun consecutiveSelectionsHaveDistinctPathsAndManagedDeletionPreservesSources() = runBlocking {
    val source = File.createTempFile("profile-source", ".png")
    source.writeBytes(byteArrayOf(1, 2, 3))
    val first = saver.save(KmpFile(Uri.fromFile(source)))!!
    val second = saver.save(KmpFile(Uri.fromFile(source)))!!
    try {
      assertNotEquals(first, second)
      assertArrayEquals(source.readBytes(), File(first).readBytes())
      saver.delete(first)
      assertFalse(File(first).exists())
      assertTrue(File(second).exists())
      saver.delete(source.path)
      assertTrue(source.exists())
    } finally {
      saver.delete(first)
      saver.delete(second)
      source.delete()
    }
  }

  @Test fun nullInputStreamFailsAndRemovesPartialCopy() = runBlocking {
    val provider = object : ContentProvider() {
      override fun onCreate() = true
      override fun getType(uri: Uri) = "image/png"
      override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
      override fun insert(uri: Uri, values: ContentValues?): Uri? = null
      override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
      override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
      override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? = null
    }
    ShadowContentResolver.registerProviderInternal("empty-avatar", provider)
    val directory = File(files.getImageDir())
    val before = directory.listFiles().orEmpty().map { it.name }.toSet()
    assertNull(saver.save(KmpFile(Uri.parse("content://empty-avatar/photo"))))
    assertEquals(before, directory.listFiles().orEmpty().map { it.name }.toSet())
  }

  @Test fun extensionCannotEscapePrivateDirectory() {
    assertFalse(avatarFilename("png/../../outside").contains('/'))
    assertFalse(isManagedAvatar("${files.getImageDir()}/avatar_../outside", files.getImageDir()))
  }
}
