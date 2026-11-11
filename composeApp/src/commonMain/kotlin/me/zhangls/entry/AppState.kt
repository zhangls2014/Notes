package me.zhangls.entry

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.zhangls.framework.mvi.MviState
import me.zhangls.model.AppLanguage
import me.zhangls.model.DarkThemeConfig
import me.zhangls.model.FontSizeConfig

/**
 * @author zhangls
 */
@Serializable
data class AppState(
  val isLogin: Boolean?,
  @Transient val accountId: String? = null,
  @Transient val sessionLoaded: Boolean = false,
  @Transient val biometricUnlockEnabled: Boolean = false,
  @Transient val deviceVerified: Boolean = false,
  @Transient val verifyingDevice: Boolean = false,
  @Transient val settingsLoaded: Boolean = false,
  @Transient val lastReadReleaseVersion: String? = null,
  val dynamicColor: Boolean = false,
  val darkTheme: DarkThemeConfig = DarkThemeConfig.LIGHT,
  val fontSize: FontSizeConfig = FontSizeConfig.STANDARD,
  val appLanguage: AppLanguage? = null,
) : MviState {
  internal val access: AppAccess
    get() = when {
      !sessionLoaded -> AppAccess.Loading
      isLogin == false -> AppAccess.SignedOut
      biometricUnlockEnabled && !deviceVerified -> AppAccess.Locked
      else -> AppAccess.Ready
    }
}

internal enum class AppAccess { Loading, SignedOut, Locked, Ready }
