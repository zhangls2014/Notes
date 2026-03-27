package me.zhangls.data.datastore

import android.content.Context
import androidx.datastore.dataStoreFile
import org.koin.core.annotation.Factory


@Factory
actual class AppDataStoreFactory(private val context: Context) {
  actual fun getDataStorePath(filename: String): String {
    return context.dataStoreFile(filename).absolutePath
  }
}