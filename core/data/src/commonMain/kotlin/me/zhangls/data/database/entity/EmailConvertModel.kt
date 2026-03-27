package me.zhangls.data.database.entity

import androidx.room3.Embedded
import androidx.room3.Relation

data class EmailConvertModel(
  @Embedded val email: EmailEntity,

  @Relation(
    parentColumn = "senderId",
    entityColumn = "id"
  )
  val sender: AccountEntity,
)