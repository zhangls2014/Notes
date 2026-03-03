package me.zhangls.main.favorites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableSupportingPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun FavoritesScreen() {
  val scaffoldNavigator = rememberSupportingPaneScaffoldNavigator()
  val scope = rememberCoroutineScope()
  val backNavigationBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange

  NavigableSupportingPaneScaffold(
    navigator = scaffoldNavigator,
    mainPane = {
      AnimatedPane {
        if (scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting] == PaneAdaptedValue.Hidden) {
          Button(
            modifier = Modifier
              .wrapContentSize(),
            onClick = {
              scope.launch {
                scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Supporting)
              }
            }
          ) {
            Text("Show supporting pane")
          }
        } else {
          Text("Supporting pane is shown")
        }
      }
    },
    supportingPane = {
      AnimatedPane(modifier = Modifier.safeContentPadding()) {
        Column {
          // Allow users to dismiss the supporting pane. Use back navigation to
          // hide an expanded supporting pane.
          if (scaffoldNavigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting] == PaneAdaptedValue.Expanded) {
            // Material design principles promote the usage of a right-aligned
            // close (X) button.
            IconButton(
              modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp),
              onClick = {
                scope.launch {
                  scaffoldNavigator.navigateBack(backNavigationBehavior)
                }
              }
            ) {
              Icon(Icons.Default.Close, contentDescription = "Close")
            }
          }
          Text("Supporting pane")
        }
      }
    }
  )
}

@Preview(showSystemUi = true)
@Composable
private fun FavoritesScreenPreview() {
  FavoritesScreen()
}