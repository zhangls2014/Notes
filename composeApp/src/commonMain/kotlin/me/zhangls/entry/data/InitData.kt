package me.zhangls.entry.data

import me.zhangls.data.repository.CommonRepository
import me.zhangls.framework.AppInfo
import org.koin.core.annotation.Factory

/** 启动时执行一次的数据初始化；由应用入口管理协程生命周期。 */
@Factory
class InitData(
  private val commonRepository: CommonRepository,
  private val appInfo: AppInfo,
) {
  suspend fun run() {
    val versionCode = appInfo.getVersionCode()
    // TODO 版本更新后的第一次启动
    commonRepository.increaseLaunchCount()
    commonRepository.updateVersionCode(versionCode)
  }
}
