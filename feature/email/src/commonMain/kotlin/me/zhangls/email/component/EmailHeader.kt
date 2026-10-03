package me.zhangls.email.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import me.zhangls.data.model.AccountModel
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Star
import me.zhangls.theme.icon.StarFill
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_action_cancel_favorite
import notes.feature.email.generated.resources.email_action_favorite
import org.jetbrains.compose.resources.stringResource

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

/**
 * 邮件卡片头部：头像 + 发件人 / 时间 + 收藏按钮。
 *
 * 列表项（[EmailListItem]）与详情项（[EmailDetailItem]）复用头部，统一头像、时间、收藏按钮颜色与无障碍文案。
 * 星形图标在收藏状态使用 Rounded StarFill，未收藏状态使用 Rounded Star。
 *
 * [variant] 决定头像是否带选中翻转，时间与收藏按钮样式统一使用主题。
 *
 * @param account 发件人（头像与姓名来源）
 * @param createdAt 展示用的时间文本
 * @param isImportant 是否已收藏，决定星形填充状态、按钮主题颜色与无障碍文案
 * @param variant 列表项 or 详情项，见 [EmailHeaderVariant]
 * @param isSelected 仅列表项使用：多选态下驱动头像翻转
 * @param onFavoriteClick 收藏按钮回调
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EmailHeader(
  account: AccountModel,
  createdAt: String,
  isImportant: Boolean,
  variant: EmailHeaderVariant,
  modifier: Modifier = Modifier,
  isSelected: Boolean = false,
  onFavoriteClick: () -> Unit = {},
) {
  Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    if (variant == EmailHeaderVariant.ListItem) {
      FlipAvatar(isSelected = isSelected, account = account)
    } else {
      ProfileImage(drawableKey = account.avatar, description = account.fullName)
    }

    Column(
      modifier = Modifier
        .weight(1F)
        .padding(horizontal = 12.dp, vertical = 4.dp),
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = account.firstName,
        style = MaterialTheme.typography.labelLarge,
      )
      Text(
        text = createdAt,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    FilledTonalIconButton(
      onClick = onFavoriteClick,
      shapes = IconButtonDefaults.shapes(),
      colors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = if (isImportant) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (isImportant) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
      ),
    ) {
      Icon(
        imageVector = if (isImportant) Icons.Rounded.StarFill else Icons.Rounded.Star,
        contentDescription = stringResource(
          if (isImportant) Res.string.email_action_cancel_favorite
          else Res.string.email_action_favorite
        ),
      )
    }
  }
}

/**
 * 头像 + 选中翻转：绕 Y 轴翻到背面显示"已选中"图案。
 */
@Composable
private fun FlipAvatar(modifier: Modifier = Modifier, isSelected: Boolean, account: AccountModel) {
  val rotation by animateFloatAsState(
    targetValue = if (isSelected) 180F else 0F,
    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
    label = "rotation"
  )

  Box(
    modifier = modifier
      .graphicsLayer {
        rotationY = rotation
        cameraDistance = 12 * density
      }
  ) {
    if (rotation <= 90F) {
      ProfileImage(drawableKey = account.avatar, description = account.fullName)
    }
    if (rotation > 90F) {
      SelectedProfileImage(modifier = Modifier.graphicsLayer {
        // 修正背面的图像
        rotationY = 180F
      })
    }
  }
}
