package me.zhangls.entry

import me.zhangls.data.repository.AccountDataRepository
import me.zhangls.data.repository.UserRepository
import org.koin.core.annotation.Singleton

@Singleton
internal class AppSessionServices(val accounts: AccountDataRepository, users: UserRepository) {
  val auth = configuredAuthEndpoint?.let { PasswordlessAuthClient(it, users) }
}
