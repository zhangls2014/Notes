package me.zhangls.email.component

/**
 * 邮件卡片头部的两种形态。
 *
 * @author zhangls
 */
internal enum class EmailHeaderVariant {
  /** 列表项：头像随选中态翻转。 */
  ListItem,

  /** 详情项：静态头像。 */
  Detail,
}
