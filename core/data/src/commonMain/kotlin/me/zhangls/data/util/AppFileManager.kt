package me.zhangls.data.util


expect class AppFileManager {
  fun getDatabasePath(filename: String): String
  fun getDataStorePath(filename: String): String
  fun createImageFile(filename: String): String
  fun getImageDir(): String
  fun deleteFile(path: String)
}