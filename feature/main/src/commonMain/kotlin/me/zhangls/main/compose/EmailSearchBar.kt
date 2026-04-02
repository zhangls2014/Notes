package me.zhangls.main.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.mohamedrejeb.calf.io.KmpFile
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.model.UserModel
import me.zhangls.data.model.toDomain
import me.zhangls.main.EmailIntent
import me.zhangls.main.EmailViewModel
import me.zhangls.main.icon.Search
import me.zhangls.theme.icon.ArrowBackIosNew
import me.zhangls.theme.icon.Icons
import notes.feature.main.generated.resources.Res
import notes.feature.main.generated.resources.main_action_owner_info
import notes.feature.main.generated.resources.main_action_search_collapsed
import notes.feature.main.generated.resources.main_hint_search
import notes.feature.main.generated.resources.main_ic_default_avatar
import notes.feature.main.generated.resources.main_msg_no_item_found
import notes.feature.main.generated.resources.main_msg_no_search_history
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * @author zhangls
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
internal fun EmailSearchBar(
  isBottomNavigationBar: Boolean,
  scrollBehavior: SearchBarScrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(),
  viewmodel: EmailViewModel,
  onResultClick: (Long) -> Unit = {}
) {
  val state by viewmodel.state.collectAsStateWithLifecycle()
  val searchResults = viewmodel.searchResults.collectAsLazyPagingItems()

  val textFieldState = rememberTextFieldState()
  val searchBarState = rememberSearchBarState()
  val scope = rememberCoroutineScope()
  val inputField = @Composable {
    SearchBarDefaults.InputField(
      searchBarState = searchBarState,
      textFieldState = textFieldState,
      readOnly = searchBarState.currentValue == SearchBarValue.Collapsed,
      onSearch = {
        textFieldState.clearText()
        scope.launch { searchBarState.animateToCollapsed() }
      },
      placeholder = {
        Text(
          text = stringResource(Res.string.main_hint_search),
          modifier = Modifier.clearAndSetSemantics {}
        )
      },
      leadingIcon = {
        if (searchBarState.currentValue == SearchBarValue.Expanded) {
          TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(stringResource(Res.string.main_action_search_collapsed)) } },
            state = rememberTooltipState(),
          ) {
            IconButton(onClick = { scope.launch { searchBarState.animateToCollapsed() } }) {
              Icon(
                imageVector = Icons.Rounded.ArrowBackIosNew,
                contentDescription = stringResource(Res.string.main_action_search_collapsed),
              )
            }
          }
        } else {
          Icon(Icons.Rounded.Search, contentDescription = null)
        }
      },
      trailingIcon = {
        state.user?.also {
          ProfileImage(it) { kmpFile ->
            viewmodel.sendIntent(EmailIntent.UpdateSelectedAvatar(kmpFile))
          }
        }
      },
    )
  }

  LaunchedEffect(searchBarState.currentValue) {
    scope.launch {
      if (searchBarState.currentValue == SearchBarValue.Collapsed) {
        textFieldState.clearText()
        searchBarState.animateToCollapsed()
      }
    }
  }

  LaunchedEffect(viewmodel) {
    snapshotFlow { textFieldState.text.toString() }
      .debounce(300)
      .distinctUntilChanged()
      .collect {
        viewmodel.sendIntent(EmailIntent.UpdateSearchText(it))
      }
  }

  AppBarWithSearch(
    scrollBehavior = scrollBehavior,
    state = searchBarState,
    inputField = inputField,
    colors = SearchBarDefaults.appBarWithSearchColors(
      scrolledSearchBarContainerColor = Color.Unspecified,
      appBarContainerColor = Color.Transparent,
      scrolledAppBarContainerColor = Color.Unspecified,
    ),
  )

  if (isBottomNavigationBar) {
    ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
      SearchResults(searchText = textFieldState.text, searchResults = searchResults) {
        textFieldState.clearText()
        scope.launch { searchBarState.animateToCollapsed() }
        onResultClick(it)
      }
    }
  } else {
    ExpandedDockedSearchBar(state = searchBarState, inputField = inputField) {
      SearchResults(searchText = textFieldState.text, searchResults = searchResults) {
        textFieldState.clearText()
        scope.launch { searchBarState.animateToCollapsed() }
        onResultClick(it)
      }
    }
  }
}

@Composable
private fun ProfileImage(user: UserModel, onImageSelected: (KmpFile?) -> Unit) {
  val launcher = rememberFilePickerLauncher(
    type = FilePickerFileType.Image,
    selectionMode = FilePickerSelectionMode.Single,
    onResult = { onImageSelected(it.firstOrNull()) }
  )

  AsyncImage(
    modifier = Modifier
      .size(40.dp)
      .clip(CircleShape)
      .clickable { launcher.launch() },
    model = user.avatar,
    placeholder = painterResource(Res.drawable.main_ic_default_avatar),
    error = painterResource(Res.drawable.main_ic_default_avatar),
    contentScale = ContentScale.Crop,
    contentDescription = stringResource(Res.string.main_action_owner_info),
  )
}


@Composable
private fun SearchResults(
  searchText: CharSequence,
  searchResults: LazyPagingItems<EmailConvertModel>,
  onResultClick: (Long) -> Unit,
) {
  if (searchResults.itemCount > 0) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
      items(count = searchResults.itemCount, key = searchResults.itemKey { it.email.id }) {
        val email = searchResults[it] ?: return@items
        val sender = email.sender.toDomain()
        ListItem(
          headlineContent = { Text(email.email.subject) },
          supportingContent = { Text(sender.fullName) },
          leadingContent = {
            ProfileImage(drawableKey = sender.avatar, description = stringResource(Res.string.main_action_owner_info))
          },
          modifier = Modifier.clickable { onResultClick(email.email.id) },
        )
      }
    }
  } else if (searchText.isNotEmpty()) {
    Text(
      text = stringResource(Res.string.main_msg_no_item_found),
      modifier = Modifier.padding(16.dp),
    )
  } else {
    Text(
      text = stringResource(Res.string.main_msg_no_search_history),
      modifier = Modifier.padding(16.dp),
    )
  }
}
