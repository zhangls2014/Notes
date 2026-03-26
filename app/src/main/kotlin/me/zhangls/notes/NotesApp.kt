package me.zhangls.notes

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.zhangls.data.repository.CommonRepository
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.notes.data.local.LocalEmailsDataProvider
import me.zhangls.notes.util.appVersionCode
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.plugin.module.dsl.startKoin

/**
 * @author zhangls
 */
@KoinApplication(modules = [NotesModule::class])
class NotesApp : Application() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val emailsRepository: EmailsRepository by inject()
  private val commonRepository: CommonRepository by inject()

  override fun onCreate() {
    super.onCreate()

    initKoin()
    initData()
  }

  private fun initKoin() {
    startKoin<NotesApp> {
      androidLogger()
      androidContext(this@NotesApp)
    }
  }

  private fun initData() {
    scope.launch {
      commonRepository.commonFlow.collect {
        if (it.launchCount == 0L) {
          emailsRepository.insertEmails(LocalEmailsDataProvider.allEmails)
        }
        if (it.lastVersionCode != appVersionCode()) {
          // TODO 版本更新后的第一次启动
        }
      }
    }
    scope.launch {
      commonRepository.increaseLaunchCount()
      commonRepository.updateVersionCode(appVersionCode())
    }
  }
}