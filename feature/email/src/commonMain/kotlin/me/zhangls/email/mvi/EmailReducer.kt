package me.zhangls.email.mvi


object EmailReducer {
  fun reduce(oldState: EmailState, action: EmailAction): EmailState {
    return with(oldState) {
      when (action) {
        EmailAction.ClearSelectedEmail -> copy(selectedItems = emptySet())
        is EmailAction.UpdateUser -> copy(user = action.user)
        is EmailAction.UpdateSelectedEmail -> updateSelectedEmail(action)
        is EmailAction.SetDraftVisible -> copy(isEmailDraftVisible = action.visible)
        is EmailAction.SetSending -> copy(isSending = action.sending)
        is EmailAction.UpdateAllAccounts -> copy(accounts = action.accounts)
        is EmailAction.UpdateDraftRecipients -> updateDraftRecipients(action)
        is EmailAction.UpdateDraftSubject -> copy(draftSubject = action.subject)
        is EmailAction.UpdateDraftBody -> copy(draftBody = action.body)
        EmailAction.ClearDraft -> clearDraft()
      }
    }
  }

  private fun EmailState.updateSelectedEmail(action: EmailAction.UpdateSelectedEmail): EmailState {
    val newSelectedItems = if (selectedItems.contains(action.emailId)) {
      selectedItems - action.emailId
    } else {
      selectedItems + action.emailId
    }
    return copy(selectedItems = newSelectedItems)
  }

  private fun EmailState.updateDraftRecipients(action: EmailAction.UpdateDraftRecipients): EmailState {
    val ids = if (action.selected) {
      draftRecipientIds + action.recipientId
    } else {
      draftRecipientIds - action.recipientId
    }
    return copy(draftRecipientIds = ids)
  }

  private fun EmailState.clearDraft(): EmailState {
    return copy(
      draftRecipientIds = emptySet(),
      draftSubject = "",
      draftBody = "",
    )
  }
}
