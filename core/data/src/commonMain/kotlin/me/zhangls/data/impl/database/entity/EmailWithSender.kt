package me.zhangls.data.impl.database.entity

import androidx.room3.Embedded
import androidx.room3.Relation

/**
 * Room 查询载体：邮件实体 + 其发件人实体。
 *
 * 仅供数据层内部查询返回使用，调用方应使用对外读模型 `me.zhangls.data.model.EmailModel`。
 *
 * 可见性说明：本类型出现在 `EmailDao`（public）的方法签名中，而 Room 生成的
 * `AppDatabaseConstructor` 被硬编码为 `public actual`，这条链（构造器 → 数据库 →
 * DAO → 实体）无法用 `internal` 收敛，故此处保持 public，靠包路径 `impl.*` 表达归属。
 */
data class EmailWithSender(
  @Embedded val email: EmailEntity,

  @Relation(
    parentColumns = ["senderId"],
    entityColumns = ["id"]
  )
  val sender: AccountEntity,
)
