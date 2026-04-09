package me.zhangls.main.waterfall


object EmailReducer {
  fun reduce(oldState: EmailState, action: EmailAction): EmailState {
    return with(oldState) {
      when (action) {
        EmailAction.ClearSelectedEmail -> copy(selectedItems = emptySet())
        is EmailAction.UpdateUser -> copy(user = action.user)
        is EmailAction.UpdateSelectedEmail -> {
          val newSelectedItems = if (selectedItems.contains(action.emailId)) {
            selectedItems - action.emailId
          } else {
            selectedItems + action.emailId
          }
          copy(selectedItems = newSelectedItems)
        }

        is EmailAction.UpdateSearchText -> copy(searchText = action.text)
      }
    }
  }
}
