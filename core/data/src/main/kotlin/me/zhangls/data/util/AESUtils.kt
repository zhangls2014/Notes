package me.zhangls.data.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 加解密工具
 *
 * @author zhangls
 */
object AESUtils {
  private const val ANDROID_KEYSTORE = "AndroidKeyStore"
  private const val TRANSFORMATION = "AES/GCM/NoPadding"
  private const val IV_LENGTH = 12
  private const val TAG_LENGTH = 128

  /**
   * 通过 KeyStore 创建或获取 AES Key
   */
  fun getSecretKey(alias: String): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    // 如果已存在直接返回
    keyStore.getKey(alias, null)?.let { return it as SecretKey }

    // 生成新的 AES Key
    val keyGenParam = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
      .setKeySize(TAG_LENGTH)
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setRandomizedEncryptionRequired(true)
      .build()

    return KeyGenerator.getInstance("AES", ANDROID_KEYSTORE).let {
      it.init(keyGenParam)
      it.generateKey()
    }
  }

  /**
   * 加密
   * 返回 Base64(IV + CipherText)
   */
  fun encrypt(alias: String, plainText: String): String {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    val secretKey = getSecretKey(alias)

    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val iv = cipher.iv
    val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

    val result = ByteArray(iv.size + encrypted.size)
    System.arraycopy(iv, 0, result, 0, iv.size)
    System.arraycopy(encrypted, 0, result, iv.size, encrypted.size)

    return Base64.encodeToString(result, Base64.NO_WRAP)
  }

  /**
   * 解密
   */
  fun decrypt(alias: String, cipherText: String): String {
    val decoded = Base64.decode(cipherText, Base64.NO_WRAP)
    val iv = decoded.copyOfRange(0, IV_LENGTH)
    val encrypted = decoded.copyOfRange(IV_LENGTH, decoded.size)

    val cipher = Cipher.getInstance(TRANSFORMATION)
    val spec = GCMParameterSpec(TAG_LENGTH, iv)
    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(alias), spec)

    val decrypted = cipher.doFinal(encrypted)
    return String(decrypted, Charsets.UTF_8)
  }
}