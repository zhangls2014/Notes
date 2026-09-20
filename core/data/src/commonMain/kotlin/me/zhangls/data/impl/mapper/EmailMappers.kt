package me.zhangls.data.impl.mapper

import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.EmailModel
import me.zhangls.database.entity.AccountEntity
import me.zhangls.database.entity.EmailEntity
import me.zhangls.database.entity.EmailWithSender
import me.zhangls.database.entity.RecipientIdsCodec

/**
 * 数据层内部的映射集合：实体 ↔ 对外模型。
 *
 * 放在此处的意义：对外模型（`me.zhangls.data.model`）不再反向依赖 Room，
 * 存储形态的任何变化都只需改这一个文件。
 */

internal fun AccountEntity.toModel(): AccountModel = AccountModel(
  id = id,
  firstName = firstName,
  lastName = lastName,
  email = email,
  altEmail = altEmail,
  avatar = avatar,
  isDefault = isDefault,
)

internal fun AccountModel.toEntity(): AccountEntity = AccountEntity(
  id = id,
  firstName = firstName,
  lastName = lastName,
  email = email,
  altEmail = altEmail,
  avatar = avatar,
  isDefault = isDefault,
)

internal fun EmailModel.toEntity(parentEmailId: Long?): EmailEntity = EmailEntity(
  id = id,
  senderId = sender.id,
  recipientIds = RecipientIdsCodec.encode(recipients.map { it.id }.toSet()),
  subject = subject,
  body = body,
  isImportant = isImportant,
  isStarred = isStarred,
  mailbox = mailbox,
  createdAt = createdAt,
  parentEmailId = parentEmailId,
)

internal fun EmailWithSender.toModel(): EmailModel = EmailModel(
  id = email.id,
  sender = sender.toModel(),
  subject = email.subject,
  body = email.body,
  isImportant = email.isImportant,
  isStarred = email.isStarred,
  mailbox = email.mailbox,
  createdAt = email.createdAt,
)
