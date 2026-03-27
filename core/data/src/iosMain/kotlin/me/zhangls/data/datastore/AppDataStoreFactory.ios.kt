package me.zhangls.data.datastore

import me.zhangls.data.documentDirectory

actual class AppDataStoreFactory {
  actual fun getDataStorePath(filename: String): String {
    return "${documentDirectory()}/datastore/${filename}"
  }
}