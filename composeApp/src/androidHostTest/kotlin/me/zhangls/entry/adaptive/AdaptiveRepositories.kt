package me.zhangls.entry.adaptive

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.EmailDraft
import me.zhangls.data.model.EmailModel
import me.zhangls.data.model.SettingsModel
import me.zhangls.data.model.UserModel
import me.zhangls.data.model.AuthTokens
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.model.AppLanguage
import me.zhangls.model.DarkThemeConfig
import me.zhangls.model.FontSizeConfig

internal class AdaptiveUsers : UserRepository {
  override val userFlow = MutableStateFlow<UserModel?>(null)
  override suspend fun getUser() = userFlow.value
  override suspend fun login(user: UserModel, tokens: AuthTokens) { userFlow.value = user }
  override suspend fun getTokens(): AuthTokens? = null
  override suspend fun updateAvatar(avatar: String) { userFlow.value = userFlow.value?.copy(avatar = avatar) }
  override suspend fun updateEmailSearchHistory(keyword: String) {
    userFlow.value = userFlow.value?.let { it.copy(emailSearchHistory = (listOf(keyword) + it.emailSearchHistory).distinct()) }
  }
  override suspend fun deleteEmailSearchHistory(keyword: String) {
    userFlow.value = userFlow.value?.let { it.copy(emailSearchHistory = it.emailSearchHistory - keyword) }
  }
  override suspend fun clear() { userFlow.value = null }
}

internal class AdaptiveSettings : SettingsRepository {
  override val settingsFlow = MutableStateFlow(SettingsModel(appLanguage = AppLanguage.FOLLOW_SYSTEM))
  override suspend fun updateDarkTheme(darkThemeConfig: DarkThemeConfig) {
    settingsFlow.value = settingsFlow.value.copy(darkTheme = darkThemeConfig)
  }
  override suspend fun updateDynamicColor(useDynamicColor: Boolean) {
    settingsFlow.value = settingsFlow.value.copy(dynamicColor = useDynamicColor)
  }
  override suspend fun updateFontSize(fontSizeConfig: FontSizeConfig) {
    settingsFlow.value = settingsFlow.value.copy(fontSize = fontSizeConfig)
  }
  override suspend fun updateAppLanguage(appLanguage: AppLanguage) {
    settingsFlow.value = settingsFlow.value.copy(appLanguage = appLanguage)
  }
}

internal class AdaptiveEmails : EmailsRepository {
  private val account = AccountModel(1, "Ada", "Lovelace", "ada@example.test", "", "avatar_1")
  private val emails = MutableStateFlow(List(12) { index ->
    EmailModel(
      id = index.toLong() + 1,
      sender = account,
      subject = if (index == 0) "Adaptive review" else "Window layout ${index + 1}",
      body = "Review the layout across phone, tablet and desktop windows. Long text must remain readable when the window is resized or the font size increases.",
      isImportant = index % 2 == 0,
      createdAt = "09:30",
    )
  })
  override suspend fun insertEmails(emails: List<EmailModel>) { this.emails.value += emails }
  override suspend fun insertDraft(draft: EmailDraft) = Unit
  override fun getEmail(id: Long) = emails.map { list -> list.find { it.id == id } }
  override suspend fun toggleFavorite(emailId: Long) {
    emails.value = emails.value.map { if (it.id == emailId) it.copy(isImportant = !it.isImportant) else it }
  }
  override suspend fun updateIsFavorite(emailIds: Set<Long>, isImportant: Boolean) {
    emails.value = emails.value.map { if (it.id in emailIds) it.copy(isImportant = isImportant) else it }
  }
  override suspend fun deleteEmails(emailIds: Set<Long>) { emails.value = emails.value.filterNot { it.id in emailIds } }
  private fun page(items: List<EmailModel>) = PagingData.from(
    items,
    sourceLoadStates = LoadStates(LoadState.NotLoading(false), LoadState.NotLoading(true), LoadState.NotLoading(true)),
  )
  override fun getEmailPaging() = emails.map { page(it) }
  override fun getEmailFavoritePaging() = emails.map { list -> page(list.filter { it.isImportant }) }
  override fun getThreadEmailsById(parentEmailId: Long) = MutableStateFlow(PagingData.empty<EmailModel>())
  override fun searchEmails(keywords: String) = emails.map { list -> page(list.filter { it.subject.contains(keywords, true) }) }
  override fun getAllAccounts() = MutableStateFlow(listOf(account))
}
