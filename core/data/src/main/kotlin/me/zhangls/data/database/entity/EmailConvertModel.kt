package me.zhangls.data.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class EmailConvertModel(
  @Embedded val email: EmailEntity,

  @Relation(
    parentColumn = "senderId",
    entityColumn = "id"
  )
  val sender: AccountEntity,
)