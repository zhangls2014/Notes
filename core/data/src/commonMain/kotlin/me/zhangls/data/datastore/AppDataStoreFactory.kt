package me.zhangls.data.datastore

/**
 * @author zhangls
 */
expect class AppDataStoreFactory {
  fun getDataStorePath(filename: String): String
}