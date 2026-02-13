package me.zhangls.notes.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.zhangls.framework.nav.NavEffect

/**
 * @author zhangls
 */
@Composable
fun DetailScreen(param: DetailDestination, onNavEffect: (NavEffect) -> Unit) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Text(text = "Detail ${param.id}")
    Button(onClick = { onNavEffect(NavEffect.Popup()) }) {
      Text(text = "Back")
    }
  }
}

@Preview
@Composable
private fun DetailScreenPreview() {
  DetailScreen(param = DetailDestination(0), onNavEffect = {})
}