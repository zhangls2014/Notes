package me.zhangls.notes

import android.app.Application
import me.zhangls.entry.initData
import me.zhangls.entry.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

/**
 * @author zhangls
 */
class NotesApp : Application() {
  override fun onCreate() {
    super.onCreate()

    initKoin {
      androidLogger()
      androidContext(this@NotesApp)
    }
    initData()
  }
}