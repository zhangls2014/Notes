package me.zhangls.theme.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupedDropdownMenu(
  expanded: Boolean,
  onDismissRequest: () -> Unit,
  content: @Composable ColumnScope.() -> Unit,
) {
  DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismissRequest) {
    DropdownMenuGroup(shapes = MenuDefaults.groupShapes(), content = content)
  }
}
