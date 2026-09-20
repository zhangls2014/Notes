package me.zhangls.data.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.EmailDraft
import me.zhangls.data.model.EmailModel

/**
 * 邮件仓库契约。
 *
 * 约定：本接口签名中不得出现任何 Room 类型（实体 / 查询载体），
 * 存储细节由实现模块内部消化。
 */
interface EmailsRepository {
  suspend fun insertEmails(emails: List<EmailModel>)

  /** 创建一封草稿；默认发件账户不存在时不做任何事。 */
  suspend fun insertDraft(draft: EmailDraft)

  fun getEmail(id: Long): Flow<EmailModel?>

  /** 切换收藏状态：按当前值取反。 */
  suspend fun toggleFavorite(emailId: Long)

  suspend fun updateIsFavorite(emailIds: Set<Long>, isImportant: Boolean)

  suspend fun deleteEmails(emailIds: Set<Long>)

  fun getEmailPaging(): Flow<PagingData<EmailModel>>

  fun getEmailFavoritePaging(): Flow<PagingData<EmailModel>>

  fun getThreadEmailsById(parentEmailId: Long): Flow<PagingData<EmailModel>>

  fun searchEmails(keywords: String): Flow<PagingData<EmailModel>>

  fun getAllAccounts(): Flow<List<AccountModel>>
}
