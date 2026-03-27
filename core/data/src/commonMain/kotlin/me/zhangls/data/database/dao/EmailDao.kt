package me.zhangls.data.database.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import kotlinx.coroutines.flow.Flow
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.database.entity.EmailEntity

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface EmailDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(emails: EmailEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(emails: List<EmailEntity>)

  @Transaction
  @Query("SELECT * FROM email WHERE id = :emailId")
  fun getEmail(emailId: Long): Flow<EmailConvertModel?>

  @Query("SELECT * FROM email WHERE id = :emailId")
  suspend fun getEmailById(emailId: Long): EmailEntity?

  @Transaction
  @Query("SELECT * FROM email WHERE parentEmailId IS NULL")
  fun getEmailPaging(): PagingSource<Int, EmailConvertModel>

  @Transaction
  @Query("SELECT * FROM email WHERE parentEmailId IS NULL AND isImportant == 1")
  fun getEmailFavoritePaging(): PagingSource<Int, EmailConvertModel>

  @Transaction
  @Query("SELECT * FROM email WHERE parentEmailId = :parentEmailId")
  fun getThreadEmails(parentEmailId: Long): PagingSource<Int, EmailConvertModel>

  @Query("SELECT * FROM email WHERE parentEmailId = :parentEmailId")
  suspend fun getThreadEmailsByParentId(parentEmailId: Long): List<EmailEntity>

  @Transaction
  @Query("SELECT * FROM email WHERE :keywords != '' AND subject LIKE '%' || :keywords || '%' COLLATE NOCASE")
  fun searchEmails(keywords: String): PagingSource<Int, EmailConvertModel>

  @Query("UPDATE email SET isImportant = :isImportant WHERE id IN (:emailId)")
  suspend fun updateIsImportant(emailId: Set<Long>, isImportant: Boolean)

  @Query("DELETE FROM email WHERE id = :emailId")
  suspend fun deleteById(emailId: Long)
}