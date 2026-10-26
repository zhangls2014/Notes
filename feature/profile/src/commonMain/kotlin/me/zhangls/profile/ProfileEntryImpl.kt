package me.zhangls.profile

import androidx.compose.runtime.Composable
import me.zhangls.profile.api.ProfileEntry
import org.koin.core.annotation.Singleton

@Singleton(binds = [ProfileEntry::class])
class ProfileEntryImpl : ProfileEntry {
  @Composable
  override fun Screen(onBack: () -> Unit) {
    ProfileScreen(onBack = onBack)
  }
}
