package me.zhangls.database.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import kotlinx.coroutines.flow.Flow
import me.zhangls.database.entity.EmailWithSender
import me.zhangls.database.entity.EmailEntity

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface EmailDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(emails: EmailEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(emails: List<EmailEntity>)

  @Transaction
  @Query("SELECT * FROM email WHERE id = :emailId")
  fun getEmail(emailId: Long): Flow<EmailWithSender?>

  @Query("SELECT * FROM email WHERE id = :emailId")
  suspend fun getEmailById(emailId: Long): EmailEntity?

  @Transaction
  @Query("SELECT * FROM email WHERE parentEmailId IS NULL")
  fun getEmailPaging(): PagingSource<Int, EmailWithSender>

  @Transaction
  @Query("SELECT * FROM email WHERE parentEmailId IS NULL AND isImportant == 1")
  fun getEmailFavoritePaging(): PagingSource<Int, EmailWithSender>

  @Transaction
  @Query("SELECT * FROM email WHERE parentEmailId = :parentEmailId")
  fun getThreadEmails(parentEmailId: Long): PagingSource<Int, EmailWithSender>

  @Transaction
  @Query("SELECT * FROM email WHERE :keywords != '' AND subject LIKE '%' || :keywords || '%' COLLATE NOCASE")
  fun searchEmails(keywords: String): PagingSource<Int, EmailWithSender>

  @Query("UPDATE email SET isImportant = :isImportant WHERE id IN (:emailId)")
  suspend fun updateIsImportant(emailId: Set<Long>, isImportant: Boolean)

  @Query("DELETE FROM email WHERE id IN (:emailIds)")
  suspend fun deleteByIds(emailIds: Set<Long>)

  @Query("DELETE FROM email WHERE parentEmailId IN (:parentEmailIds)")
  suspend fun deleteByParentIds(parentEmailIds: Set<Long>)

  @Query("DELETE FROM email WHERE id = :emailId")
  suspend fun deleteById(emailId: Long)
}