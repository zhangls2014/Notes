package me.zhangls.data.impl.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import androidx.room3.withWriteTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.zhangls.data.impl.mapper.toEntity
import me.zhangls.data.impl.mapper.toModel
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.EmailDraft
import me.zhangls.data.model.EmailModel
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.database.AppDatabase
import me.zhangls.database.dao.AccountDao
import me.zhangls.database.dao.EmailDao
import me.zhangls.database.entity.EmailEntity
import me.zhangls.database.entity.EmailWithSender
import me.zhangls.database.entity.RecipientIdsCodec
import org.koin.core.annotation.Singleton

@Singleton(binds = [EmailsRepository::class])
internal class EmailsRepositoryImpl(
  private val database: AppDatabase,
  private val accountDao: AccountDao,
  private val emailDao: EmailDao,
) : EmailsRepository {

  override suspend fun insertEmails(emails: List<EmailModel>) {
    database.withWriteTransaction {
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
  }

  override suspend fun insertDraft(draft: EmailDraft) {
    val senderId = accountDao.queryDefaultAccount()?.id ?: return
    emailDao.insert(
      EmailEntity(
        id = 0L,
        senderId = senderId,
        recipientIds = RecipientIdsCodec.encode(draft.recipientIds),
        subject = draft.subject,
        body = draft.body,
        createdAt = draft.createdAt,
      )
    )
  }

  override fun getEmail(id: Long): Flow<EmailModel?> {
    return emailDao.getEmail(id).map { it?.toModel() }
  }

  override suspend fun toggleFavorite(emailId: Long) {
    val entity = emailDao.getEmailById(emailId) ?: return
    // 走 UPDATE 语句，避免用 INSERT OR REPLACE 模拟更新（会触发 REPLACE
    // 语义的删行重建，开销更大且可能触发级联行为）
    emailDao.updateIsImportant(setOf(emailId), entity.isImportant.not())
  }

  override suspend fun updateIsFavorite(emailIds: Set<Long>, isImportant: Boolean) {
    emailDao.updateIsImportant(emailIds, isImportant)
  }

  override suspend fun deleteEmails(emailIds: Set<Long>) {
    database.withWriteTransaction {
      // 先批量删除子邮件，再批量删除所选邮件，避免 N+1 循环逐条删除
      emailDao.deleteByParentIds(emailIds)
      emailDao.deleteByIds(emailIds)
    }
  }

  override fun getEmailPaging(): Flow<PagingData<EmailModel>> =
    paging { emailDao.getEmailPaging() }

  override fun getEmailFavoritePaging(): Flow<PagingData<EmailModel>> =
    paging { emailDao.getEmailFavoritePaging() }

  override fun getThreadEmailsById(parentEmailId: Long): Flow<PagingData<EmailModel>> =
    paging { emailDao.getThreadEmails(parentEmailId) }

  override fun searchEmails(keywords: String): Flow<PagingData<EmailModel>> =
    paging { emailDao.searchEmails(keywords) }

  override fun getAllAccounts(): Flow<List<AccountModel>> =
    accountDao.queryAllAccount().map { accounts -> accounts.map { it.toModel() } }

  /** 统一在此把 Room 查询载体映射为对外读模型，签名不外泄 Room 类型。 */
  private fun paging(source: () -> PagingSource<Int, EmailWithSender>): Flow<PagingData<EmailModel>> =
    Pager(
      config = PagingConfig(pageSize = PAGE_SIZE),
      pagingSourceFactory = source,
    ).flow.map { data -> data.map { it.toModel() } }

  private companion object {
    const val PAGE_SIZE = 5
  }
}
