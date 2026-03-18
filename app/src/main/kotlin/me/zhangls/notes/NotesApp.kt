package me.zhangls.notes

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.notes.data.local.LocalEmailsDataProvider
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
      emailsRepository.insertEmails(LocalEmailsDataProvider.allEmails)
    }
  }
}