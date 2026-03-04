package me.zhangls.notes

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.notes.data.local.LocalEmailsDataProvider
import javax.inject.Inject

/**
 * @author zhangls
 */
@HiltAndroidApp
class NotesApp : Application() {
  private val scope = MainScope()

  @Inject
  lateinit var emailsRepository: EmailsRepository

  override fun onCreate() {
    super.onCreate()

    initData()
  }

  private fun initData() {
    scope.launch(Dispatchers.IO) {
      emailsRepository.insertEmails(LocalEmailsDataProvider.allEmails)
    }
  }
}