package me.zhangls.data.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.database.entity.EmailEntity

@Dao
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
  @Query("SELECT * FROM email WHERE subject LIKE '%' || :keywords || '%' COLLATE NOCASE")
  fun searchEmails(keywords: String): PagingSource<Int, EmailConvertModel>

  @Query("UPDATE email SET isImportant = :isImportant WHERE id IN (:emailId)")
  suspend fun updateIsImportant(emailId: Set<Long>, isImportant: Boolean)

  @Query("DELETE FROM email WHERE id = :emailId")
  suspend fun deleteById(emailId: Long)
}