package me.zhangls.notes

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.notes.data.local.LocalEmailsDataProvider
import javax.inject.Inject

/**
 * @author zhangls
 */
@HiltAndroidApp
class NotesApp : Application() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  @Inject
  lateinit var emailsRepository: EmailsRepository

  override fun onCreate() {
    super.onCreate()

    initData()
  }

  private fun initData() {
    scope.launch {
      emailsRepository.insertEmails(LocalEmailsDataProvider.allEmails)
    }
  }
}