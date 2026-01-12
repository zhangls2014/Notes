package me.zhangls.notes.ui.login

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
fun LoginScreen(onNavEffect: (NavEffect) -> Unit) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxSize()
  ) {
    Button(onClick = { onNavEffect(NavEffect.Replace(Destination.Home)) }) {
      Text(text = "Login to Home")
    }
  }
}

@Preview
@Composable
fun LoginScreenPreview() {
  LoginScreen(onNavEffect = {})
}