package me.zhangls.notes.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import me.zhangls.notes.navigation.Destination
import me.zhangls.notes.navigation.NavEffect

/**
 * @author zhangls
 */
@Composable
fun DetailScreen(param: Destination.Detail, onNavEffect: (NavEffect) -> Unit) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxSize()
  ) {
    Text(text = "Detail ${param.id}")
    Button(onClick = { onNavEffect(NavEffect.Back) }) {
      Text(text = "Back")
    }
  }
}

@Preview
@Composable
private fun DetailScreenPreview() {
  DetailScreen(param = Destination.Detail(0), onNavEffect = {})
}