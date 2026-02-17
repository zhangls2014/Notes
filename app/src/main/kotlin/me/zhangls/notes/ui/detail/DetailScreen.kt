package me.zhangls.notes.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.zhangls.framework.nav.NavEffect
import me.zhangls.theme.component.CenteredTopAppBar

/**
 * @author zhangls
 */
@Composable
fun DetailScreen(param: DetailDestination, onNavEffect: (NavEffect) -> Unit) {
  Scaffold(
    topBar = {
      CenteredTopAppBar(title = "详情", navigate = { onNavEffect(NavEffect.Popup()) })
    }
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxSize()
        .padding(it)
        .background(MaterialTheme.colorScheme.background)
    ) {
      Text(text = "Detail ${param.id}")
      Button(onClick = { onNavEffect(NavEffect.Popup()) }) {
        Text(text = "Back")
      }
    }
  }
}

@Preview
@Composable
private fun DetailScreenPreview() {
  DetailScreen(param = DetailDestination(0), onNavEffect = {})
}