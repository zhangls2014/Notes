package me.zhangls.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room3.Transaction
import androidx.room3.withWriteTransaction
import kotlinx.coroutines.flow.Flow
import me.zhangls.data.database.AppDatabase
import me.zhangls.data.database.dao.AccountDao
import me.zhangls.data.database.dao.EmailDao
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.database.entity.EmailEntity
import me.zhangls.data.model.EmailModel
import me.zhangls.data.model.toEntity
import org.koin.core.annotation.Factory

@Factory
class EmailsRepository(
  private val database: AppDatabase,
  private val accountDao: AccountDao,
  private val emailDao: EmailDao
) {
  @Transaction
  suspend fun insertEmails(emails: List<EmailModel>) {
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

  fun getEmail(id: Long): Flow<EmailConvertModel?> {
    return emailDao.getEmail(id)
  }

  suspend fun getEmailById(id: Long): EmailEntity? {
    return emailDao.getEmailById(id)
  }

  suspend fun insertEmail(entity: EmailEntity) {
    emailDao.insert(entity)
  }

  suspend fun updateIsFavorite(emailIds: Set<Long>, isImportant: Boolean) {
    emailDao.updateIsImportant(emailIds, isImportant)
  }

  suspend fun deleteEmails(emailIds: Set<Long>) {
    database.withWriteTransaction {
      emailIds.forEach {
        val threads = emailDao.getThreadEmailsByParentId(it)
        threads.forEach { thread ->
          emailDao.deleteById(thread.id)
        }
        emailDao.deleteById(it)
      }
    }
  }

  fun getEmailPaging(): Flow<PagingData<EmailConvertModel>> {
    return Pager(
      config = PagingConfig(pageSize = 5),
      pagingSourceFactory = { emailDao.getEmailPaging() }
    ).flow
  }

  fun getEmailFavoritePaging(): Flow<PagingData<EmailConvertModel>> {
    return Pager(
      config = PagingConfig(pageSize = 5),
      pagingSourceFactory = { emailDao.getEmailFavoritePaging() }
    ).flow
  }

  fun getThreadEmailsById(parentEmailId: Long): Flow<PagingData<EmailConvertModel>> {
    return Pager(
      config = PagingConfig(pageSize = 5),
      pagingSourceFactory = { emailDao.getThreadEmails(parentEmailId) }
    ).flow
  }

  fun searchEmails(keywords: String): Flow<PagingData<EmailConvertModel>> {
    return Pager(
      config = PagingConfig(pageSize = 5),
      pagingSourceFactory = { emailDao.searchEmails(keywords) }
    ).flow
  }
}