package me.zhangls.entry.data

import kotlinx.coroutines.flow.first
import me.zhangls.data.repository.CommonRepository
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.entry.data.sample.LocalEmailsDataProvider
import me.zhangls.entry.util.AppInfo
import org.koin.core.annotation.Factory

/** 启动时执行一次的数据初始化；由应用入口管理协程生命周期。 */
@Factory
class InitData(
  private val emailsRepository: EmailsRepository,
  private val commonRepository: CommonRepository,
  private val appInfo: AppInfo,
) {
  suspend fun run() {
    // 先读状态，再按顺序写入样例邮件和启动信息。
    val common = commonRepository.commonFlow.first()
    if (common.launchCount == 0L) {
      emailsRepository.insertEmails(LocalEmailsDataProvider.allEmails)
    }
    val versionCode = appInfo.getVersionCode()
    if (common.lastVersionCode != versionCode) {
      // TODO 版本更新后的第一次启动
    }
    commonRepository.increaseLaunchCount()
    commonRepository.updateVersionCode(versionCode)
  }
}
