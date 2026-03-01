package me.zhangls.notes.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeScreen(viewmodel: HomeViewModel = hiltViewModel(), isNavigationBar: Boolean, onClick: () -> Unit) {
  val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Int>()
  val scope = rememberCoroutineScope()

  NavigableListDetailPaneScaffold(
    navigator = scaffoldNavigator,
    listPane = {
      AnimatedPane {
        ListPane(isNavigationBar) {
          scope.launch {
            scaffoldNavigator.navigateTo(pane = ListDetailPaneScaffoldRole.Detail, contentKey = it)
          }
        }
      }
    },
    detailPane = {
      AnimatedPane {
        scaffoldNavigator.currentDestination?.contentKey?.let {
          DetailPane(index = it, navigate = onClick)
        }
      }
    },
    extraPane = {
      AnimatedPane {
        scaffoldNavigator.currentDestination?.contentKey?.let {
          DetailPane(index = it, navigate = onClick)
        }
      }
    }
  )
}

@Composable
fun ListPane(isNavigationBar: Boolean, onClick: (Int) -> Unit) {
  val padding = if (isNavigationBar) {
    WindowInsets.systemBars.exclude(WindowInsets.navigationBars).asPaddingValues()
  } else {
    WindowInsets.systemBars.asPaddingValues()
  }
  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(contentPadding = padding) {
      items(20) {
        Text(
          text = "Item $it",
          modifier = Modifier
            .clickable(onClick = { onClick(it) })
            .fillMaxWidth()
            .padding(16.dp)
        )
      }
    }
  }
}

@Composable
fun DetailPane(index: Int, navigate: () -> Unit) {
  Scaffold {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxSize()
        .padding(it)
    ) {
      Card {
        Box(
          modifier = Modifier
            .clickable { navigate() }
            .padding(16.dp)
        ) {
          Text(text = "Detail pane content ==> $index")
        }
      }
    }
  }
}

@Preview(showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
  HomeScreen(isNavigationBar = false, onClick = {})
}