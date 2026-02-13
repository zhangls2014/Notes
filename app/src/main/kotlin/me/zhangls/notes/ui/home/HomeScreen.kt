package me.zhangls.notes.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/**
 * @author zhangls
 */
@Composable
fun HomeScreen(viewmodel: HomeViewModel = hiltViewModel(), onHomeResult: (HomeResult) -> Unit) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    val state = viewmodel.state.collectAsState()

    LaunchedEffect(Unit) {
      viewmodel.effect.collect {
        when (it) {
          is HomeResult -> onHomeResult(it)
        }
      }
    }


    Text(text = state.value.greeting, modifier = Modifier.padding(16.dp))

    Button(onClick = { viewmodel.sendIntent(HomeIntent.Logout) }) {
      Text(text = "Back to Login")
    }

    Button(onClick = { viewmodel.sendIntent(HomeIntent.Detail) }) {
      Text(text = "Nav to Detail")
    }
  }
}

@Preview
@Composable
private fun HomeScreenPreview() {
  HomeScreen(onHomeResult = {})
}