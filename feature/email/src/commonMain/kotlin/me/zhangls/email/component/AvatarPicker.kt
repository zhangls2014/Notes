package me.zhangls.email.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mohamedrejeb.calf.io.KmpFile
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import me.zhangls.data.model.UserModel
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_action_owner_info
import notes.feature.email.generated.resources.main_ic_default_avatar
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * 用户头像选择器：展示头像并支持从文件选择器更换头像。
 *
 * 该组件只负责「展示 + 选择」的交互，头像上传的业务逻辑由调用方
 * 通过 [onImageSelected] 回调处理，与搜索逻辑解耦。
 */
@Composable
fun AvatarPicker(
  user: UserModel,
  onImageSelected: (KmpFile?) -> Unit,
) {
  val launcher = rememberFilePickerLauncher(
    type = FilePickerFileType.Image,
    selectionMode = FilePickerSelectionMode.Single,
    onResult = { onImageSelected(it.firstOrNull()) }
  )

  AsyncImage(
    modifier = Modifier
      .size(40.dp)
      .clip(CircleShape)
      .clickable { launcher.launch() },
    model = user.avatar,
    placeholder = painterResource(Res.drawable.main_ic_default_avatar),
    error = painterResource(Res.drawable.main_ic_default_avatar),
    contentScale = ContentScale.Crop,
    contentDescription = stringResource(Res.string.email_action_owner_info),
  )
}
