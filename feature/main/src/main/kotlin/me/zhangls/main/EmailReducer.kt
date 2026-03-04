package me.zhangls.main

import me.zhangls.data.model.toDomain


object EmailReducer {
  fun reduce(oldState: EmailState, action: EmailAction): EmailState {
    return with(oldState) {
      when (action) {
        EmailAction.ClearSelectedEmail -> copy(selectedItems = emptySet())
        is EmailAction.UpdateOwnerAccount -> copy(ownerAccount = action.account?.toDomain())
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