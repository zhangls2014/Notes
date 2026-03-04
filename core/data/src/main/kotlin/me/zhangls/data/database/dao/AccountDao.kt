package me.zhangls.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import me.zhangls.data.database.entity.AccountEntity

@Dao
interface AccountDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(accounts: AccountEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(accounts: List<AccountEntity>)
}