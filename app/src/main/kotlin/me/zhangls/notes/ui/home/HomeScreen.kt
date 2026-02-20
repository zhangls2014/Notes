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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.zhangls.theme.component.CenteredTopAppBar

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeScreen(isNavigationBar: Boolean) {
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
          DetailPane(index = it) {
            scope.launch {
              scaffoldNavigator.navigateBack()
            }
          }
        }
      }
    },
    extraPane = {
      AnimatedPane {
        scaffoldNavigator.currentDestination?.contentKey?.let {
          DetailPane(index = it) {
            scope.launch {
              scaffoldNavigator.navigateBack()
            }
          }
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
            .fillMaxWidth()
            .clickable { onClick(it) }
            .padding(16.dp)
        )
      }
    }
  }
}

@Composable
fun DetailPane(index: Int, navigate: () -> Unit) {
  Scaffold(
    topBar = {
      CenteredTopAppBar(title = "Detail Pane", navigate = navigate)
    }
  ) {
    Card(modifier = Modifier.padding(it)) {
      Text(text = "Detail pane content ==> $index")
    }
  }
}

@Preview(showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
  HomeScreen(isNavigationBar = false)
}