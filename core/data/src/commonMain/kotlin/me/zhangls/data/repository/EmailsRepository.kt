package me.zhangls.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room3.withWriteTransaction
import kotlinx.coroutines.flow.Flow
import me.zhangls.data.database.AppDatabase
import me.zhangls.data.database.dao.AccountDao
import me.zhangls.data.database.dao.EmailDao
import me.zhangls.data.database.entity.AccountEntity
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.database.entity.EmailEntity
import me.zhangls.data.model.EmailModel
import me.zhangls.data.model.toEntity
import org.koin.core.annotation.Singleton

interface EmailsRepository {
  suspend fun insertEmails(emails: List<EmailModel>)

  fun getEmail(id: Long): Flow<EmailConvertModel?>

  suspend fun getEmailById(id: Long): EmailEntity?

  suspend fun insertEmail(entity: EmailEntity)

  suspend fun updateIsFavorite(emailIds: Set<Long>, isImportant: Boolean)

  suspend fun deleteEmails(emailIds: Set<Long>)

  fun getEmailPaging(): Flow<PagingData<EmailConvertModel>>

  fun getEmailFavoritePaging(): Flow<PagingData<EmailConvertModel>>

  fun getThreadEmailsById(parentEmailId: Long): Flow<PagingData<EmailConvertModel>>

  fun searchEmails(keywords: String): Flow<PagingData<EmailConvertModel>>

  suspend fun getDefaultAccount(): AccountEntity?

  fun getAllAccounts(): Flow<List<AccountEntity>>
}

@Singleton(binds = [EmailsRepository::class])
class EmailsRepositoryImpl(
  private val database: AppDatabase,
  private val accountDao: AccountDao,
  private val emailDao: EmailDao
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

  override fun getEmail(id: Long): Flow<EmailConvertModel?> {
    return emailDao.getEmail(id)
  }

  override suspend fun getEmailById(id: Long): EmailEntity? {
    return emailDao.getEmailById(id)
  }

  override suspend fun insertEmail(entity: EmailEntity) {
    emailDao.insert(entity)
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

  override fun getEmailPaging(): Flow<PagingData<EmailConvertModel>> {
    return Pager(
      config = PagingConfig(pageSize = 5),
      pagingSourceFactory = { emailDao.getEmailPaging() }
    ).flow
  }

  override fun getEmailFavoritePaging(): Flow<PagingData<EmailConvertModel>> {
    return Pager(
      config = PagingConfig(pageSize = 5),
      pagingSourceFactory = { emailDao.getEmailFavoritePaging() }
    ).flow
  }

  override fun getThreadEmailsById(parentEmailId: Long): Flow<PagingData<EmailConvertModel>> {
    return Pager(
      config = PagingConfig(pageSize = 5),
      pagingSourceFactory = { emailDao.getThreadEmails(parentEmailId) }
    ).flow
  }

  override fun searchEmails(keywords: String): Flow<PagingData<EmailConvertModel>> {
    return Pager(
      config = PagingConfig(pageSize = 5),
      pagingSourceFactory = { emailDao.searchEmails(keywords) }
    ).flow
  }

  override suspend fun getDefaultAccount() = accountDao.queryDefaultAccount()

  override fun getAllAccounts() = accountDao.queryAllAccount()
}
