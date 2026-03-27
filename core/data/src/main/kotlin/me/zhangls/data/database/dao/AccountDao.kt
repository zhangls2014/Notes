package me.zhangls.data.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import me.zhangls.data.database.entity.AccountEntity

@Dao
interface AccountDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(accounts: AccountEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(accounts: List<AccountEntity>)
}