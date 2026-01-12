package me.zhangls.notes.ui.home

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
fun HomeScreen(onNavEffect: (NavEffect) -> Unit) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxSize()
  ) {
    Button(onClick = { onNavEffect.invoke(NavEffect.Restart(Destination.Login)) }) {
      Text(text = "Back to Login")
    }

    Button(onClick = {
      val navigate = NavEffect.Navigate(Destination.Detail((0..100).random()))
      onNavEffect.invoke(navigate)
    }) {
      Text(text = "Nav to Detail")
    }
  }
}

@Preview
@Composable
private fun HomeScreenPreview() {
  HomeScreen(onNavEffect = {})
}