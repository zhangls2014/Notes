package me.zhangls.entry.data

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.CommonModel
import me.zhangls.data.model.EmailDraft
import me.zhangls.data.model.EmailModel
import me.zhangls.data.repository.CommonRepository
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.entry.util.AppInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class InitDataTest {
  @Test
  fun firstLaunchSeedsEmailsAndRecordsVersion() = runTest {
    val common = FakeCommonRepository(CommonModel())
    val emails = RecordingEmailsRepository()
    val initData = InitData(emails, common, FakeAppInfo(42))

    initData.run()

    assertEquals(1, emails.insertCalls)
    assertEquals(1L, common.commonFlow.value.launchCount)
    assertEquals(42L, common.commonFlow.value.lastVersionCode)
  }

  @Test
  fun laterLaunchDoesNotSeedAgain() = runTest {
    val common = FakeCommonRepository(CommonModel(launchCount = 3, lastVersionCode = 42))
    val emails = RecordingEmailsRepository()

    InitData(emails, common, FakeAppInfo(42)).run()

    assertEquals(0, emails.insertCalls)
    assertEquals(4L, common.commonFlow.value.launchCount)
  }
}

private class FakeAppInfo(private val version: Long) : AppInfo {
  override fun getVersionCode(): Long = version
  override fun getVersionName(): String = "test"
  override fun getBuildNumber(): String = version.toString()
}

private class FakeCommonRepository(initial: CommonModel) : CommonRepository {
  override val commonFlow = MutableStateFlow(initial)

  override suspend fun increaseLaunchCount() {
    commonFlow.value = commonFlow.value.copy(launchCount = commonFlow.value.launchCount + 1)
  }

  override suspend fun updateVersionCode(versionCode: Long) {
    commonFlow.value = commonFlow.value.copy(lastVersionCode = versionCode)
  }
}

private class RecordingEmailsRepository : EmailsRepository {
  var insertCalls = 0

  override suspend fun insertEmails(emails: List<EmailModel>) {
    insertCalls++
  }

  override suspend fun insertDraft(draft: EmailDraft) = error("not used")
  override fun getEmail(id: Long): Flow<EmailModel?> = emptyFlow()
  override suspend fun toggleFavorite(emailId: Long) = error("not used")
  override suspend fun updateIsFavorite(emailIds: Set<Long>, isImportant: Boolean) = error("not used")
  override suspend fun deleteEmails(emailIds: Set<Long>) = error("not used")
  override fun getEmailPaging(): Flow<PagingData<EmailModel>> = emptyFlow()
  override fun getEmailFavoritePaging(): Flow<PagingData<EmailModel>> = emptyFlow()
  override fun getThreadEmailsById(parentEmailId: Long): Flow<PagingData<EmailModel>> = emptyFlow()
  override fun searchEmails(keywords: String): Flow<PagingData<EmailModel>> = emptyFlow()
  override fun getAllAccounts(): Flow<List<AccountModel>> = emptyFlow()
}
