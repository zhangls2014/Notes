package me.zhangls.data.impl.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import androidx.room3.withWriteTransaction
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.zhangls.data.impl.mapper.toEntity
import me.zhangls.data.impl.mapper.toModel
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.EmailDraft
import me.zhangls.data.model.EmailModel
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.database.AppDatabase
import me.zhangls.database.entity.EmailEntity
import me.zhangls.database.entity.EmailWithSender
import me.zhangls.database.entity.RecipientIdsCodec

internal class EmailsRepositoryImpl(private val database: AppDatabase) : EmailsRepository {
  private val accountDao = database.accountDao()
  private val emailDao = database.emailDao()
  private val lifetime = Job()

  suspend fun seedIfEmpty(emails: List<EmailModel>) {
    database.withWriteTransaction {
      if (accountDao.queryDefaultAccount() == null) writeEmails(emails)
    }
  }

  fun retire() {
    lifetime.cancel()
    database.close()
  }

  override suspend fun insertEmails(emails: List<EmailModel>) = withAccount {
    database.withWriteTransaction { writeEmails(emails) }
  }

  private suspend fun writeEmails(emails: List<EmailModel>) {
    emails.forEach { email ->
      accountDao.insert(email.sender.toEntity())
      accountDao.insert(email.recipients.map { it.toEntity() })
      emailDao.insert(email.toEntity(null))
    }

    emails.forEach { email ->
      email.threads.forEach { thread ->
        emailDao.insert(thread.toEntity(email.id))
      }
    }
  }

  override suspend fun insertDraft(draft: EmailDraft): Unit = withAccount {
    val senderId = accountDao.queryDefaultAccount()?.id ?: return@withAccount
    emailDao.insert(
      EmailEntity(
        id = 0L,
        senderId = senderId,
        recipientIds = RecipientIdsCodec.encode(draft.recipientIds),
        subject = draft.subject,
        body = draft.body,
        createdAt = draft.createdAt,
      ),
    )
  }

  override fun getEmail(id: Long): Flow<EmailModel?> = observeAccount {
    emailDao.getEmail(id).map { it?.toModel() }
  }

  override suspend fun toggleFavorite(emailId: Long) = withAccount {
    val entity = emailDao.getEmailById(emailId) ?: return@withAccount
    // 走 UPDATE 语句，避免用 INSERT OR REPLACE 模拟更新（会触发 REPLACE
    // 语义的删行重建，开销更大且可能触发级联行为）
    emailDao.updateIsImportant(setOf(emailId), entity.isImportant.not())
  }

  override suspend fun updateIsFavorite(emailIds: Set<Long>, isImportant: Boolean) = withAccount {
    emailDao.updateIsImportant(emailIds, isImportant)
  }

  override suspend fun deleteEmails(emailIds: Set<Long>) = withAccount {
    database.withWriteTransaction {
      // 先批量删除子邮件，再批量删除所选邮件，避免 N+1 循环逐条删除
      emailDao.deleteByParentIds(emailIds)
      emailDao.deleteByIds(emailIds)
    }
  }

  override fun getEmailPaging(): Flow<PagingData<EmailModel>> = paging { emailDao.getEmailPaging() }

  override fun getEmailFavoritePaging(): Flow<PagingData<EmailModel>> = paging { emailDao.getEmailFavoritePaging() }

  override fun getThreadEmailsById(parentEmailId: Long): Flow<PagingData<EmailModel>> =
    paging { emailDao.getThreadEmails(parentEmailId) }

  override fun searchEmails(keywords: String): Flow<PagingData<EmailModel>> = paging { emailDao.searchEmails(keywords) }

  override fun getAllAccounts(): Flow<List<AccountModel>> = observeAccount {
    accountDao.queryAllAccount().map { accounts -> accounts.map { it.toModel() } }
  }

  /** 统一在此把 Room 查询载体映射为对外读模型，签名不外泄 Room 类型。 */
  private fun paging(source: () -> PagingSource<Int, EmailWithSender>): Flow<PagingData<EmailModel>> = observeAccount {
    Pager(
      config = PagingConfig(pageSize = PAGE_SIZE),
      pagingSourceFactory = source,
    ).flow.map { data -> data.map { it.toModel() } }
  }

  private fun <T> observeAccount(upstream: () -> Flow<T>): Flow<T> = flow {
    withAccount { upstream().collect { emit(it) } }
  }

  private suspend fun <T> withAccount(block: suspend () -> T): T = coroutineScope {
    val operation = coroutineContext[Job]!!
    val retireHook = lifetime.invokeOnCompletion { operation.cancel() }
    try {
      operation.ensureActive()
      block()
    } finally {
      retireHook.dispose()
    }
  }

  private companion object {
    const val PAGE_SIZE = 5
  }
}
