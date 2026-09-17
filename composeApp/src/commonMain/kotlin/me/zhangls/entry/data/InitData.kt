package me.zhangls.entry.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.zhangls.data.repository.CommonRepository
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.entry.util.AppInfo
import me.zhangls.entry.data.sample.LocalEmailsDataProvider
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
      // 先读取一次当前值再决定是否插入样例数据，最后才更新计数，
      // 避免"读计数"与"写计数"两个并发协程竞争导致样例数据永不插入
      val common = commonRepository.commonFlow.first()
      if (common.launchCount == 0L) {
        emailsRepository.insertEmails(LocalEmailsDataProvider.allEmails)
      }
      if (common.lastVersionCode != AppInfo.getVersionCode()) {
        // TODO 版本更新后的第一次启动
      }
      commonRepository.increaseLaunchCount()
      commonRepository.updateVersionCode(AppInfo.getVersionCode())
    }
  }
}