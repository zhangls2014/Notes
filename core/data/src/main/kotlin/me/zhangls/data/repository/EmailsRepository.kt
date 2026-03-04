package me.zhangls.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.zhangls.data.database.dao.AccountDao
import me.zhangls.data.database.dao.EmailDao
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.model.EmailModel
import me.zhangls.data.model.toEntity
import javax.inject.Inject

class EmailsRepository @Inject constructor(
  private val accountDao: AccountDao,
  private val emailDao: EmailDao
) {
  @Transaction
  suspend fun insertEmails(emails: List<EmailModel>) {
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

  fun getEmailPaging(): Flow<PagingData<EmailConvertModel>> {
    return Pager(
      config = PagingConfig(pageSize = 5),
      pagingSourceFactory = { emailDao.getEmailPaging() }
    ).flow
  }

  fun getThreadEmails(parentEmailId: Long): Flow<PagingData<EmailConvertModel>> {
    return Pager(
      config = PagingConfig(pageSize = 5),
      pagingSourceFactory = { emailDao.getThreadEmails(parentEmailId) }
    ).flow
  }
}