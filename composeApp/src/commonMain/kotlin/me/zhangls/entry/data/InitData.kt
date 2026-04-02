package me.zhangls.entry.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.zhangls.data.repository.CommonRepository
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.entry.data.local.LocalEmailsDataProvider
import me.zhangls.entry.util.AppInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * @author zhangls
 */
object InitData : KoinComponent {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val emailsRepository: EmailsRepository by inject()
  private val commonRepository: CommonRepository by inject()


  fun launch() {
    scope.launch {
      commonRepository.commonFlow.collect {
        if (it.launchCount == 0L) {
          emailsRepository.insertEmails(LocalEmailsDataProvider.allEmails)
        }
        if (it.lastVersionCode != AppInfo.getVersionCode()) {
          // TODO 版本更新后的第一次启动
        }
      }
    }
    scope.launch {
      commonRepository.increaseLaunchCount()
      commonRepository.updateVersionCode(AppInfo.getVersionCode())
    }
  }
}