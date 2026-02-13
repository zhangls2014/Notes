package me.zhangls.notes.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay

/**
 * @author zhangls
 */
@Composable
fun SplashScreen(onResult: () -> Unit) {
  LaunchedEffect(Unit) {
    delay(1000)
    onResult()
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Text(text = "Hello, Compose!")
  }
}

@Preview
@Composable
private fun SplashScreenPreview() {
  SplashScreen(onResult = {})
}