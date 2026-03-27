package me.zhangls.data.util

/**
 * @author zhangls
 */
expect object AESUtils {
  fun encrypt(alias: String, plainText: String): String
  fun decrypt(alias: String, cipherText: String): String
}