package me.zhangls.data.impl.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import me.zhangls.data.impl.database.entity.AccountEntity

@Dao
interface AccountDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(accounts: AccountEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(accounts: List<AccountEntity>)

  // 优先取显式标记的默认账户；无标记时按 id 兜底（老数据兼容）
  @Query("SELECT * FROM account ORDER BY isDefault DESC, id ASC LIMIT 1")
  suspend fun queryDefaultAccount(): AccountEntity?

  @Query("SELECT * FROM account ORDER BY id ASC")
  fun queryAllAccount(): Flow<List<AccountEntity>>
}
