package me.zhangls.entry

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.email.api.EmailNavigation
import me.zhangls.framework.nav.NavigationContribution
import me.zhangls.login.api.LoginDestination
import me.zhangls.login.api.LoginNavigation
import me.zhangls.main.api.FavoritesDestination
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.api.MainNavigation
import me.zhangls.main.api.SettingsDestination
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 返回栈序列化的守卫测试。
 *
 * 返回栈会被 `rememberNavBackStack` 序列化，用于配置/进程重建后的恢复。两件事只在运行期才
 * 暴露，且都表现为**重建时崩溃或状态丢失**，编译期毫无提示：
 *
 * 1. 新增目的地忘了登记进序列化注册表（各 feature 的 `NavigationContribution.navModule`）；
 * 2. 登记了 `polymorphic(NavKey::class)` 却漏了 `polymorphic(Destination::class)`——
 *    后者的用途是**作为字段类型**被引用的目的地，如 [LoginDestination.redirectTo]。
 *
 * 因此这里用真实的注册表做一次往返（encode→decode），把这两类漏项提前钉在测试里。
 *
 * 注 1：这里用 JSON 而不是 `rememberSerializable` 那套 encoder 只是为了让断言简单——
 * 多态查找完全由 `SerializersModule` 决定，与具体格式无关。
 *
 * 注 2：[contributions] 需要与组合根收集到的一致。它是一份手工列表，这是本测试最弱的一环：
 * 新增 feature 时若忘记加进来，它带来的漏项不会被这里拦住。之所以还能接受，是因为导航 key
 * 与它的登记现在同处一个文件（各 `-api` 的 `XxxNavigation`），编码时很难只加其中之一。
 */
class NavBackStackSerializationTest {

  private val contributions: List<NavigationContribution> =
    listOf(MainNavigation, LoginNavigation, EmailNavigation)

  private val json = Json {
    serializersModule = contributions.fold(SerializersModule { }) { acc, contribution ->
      acc + contribution.navModule
    }
  }

  private val serializer = NavBackStackSerializer(PolymorphicSerializer(NavKey::class))

  @Test
  fun `每个 Tab 目的地连同栈上压着的页面一起往返`() {
    listOf(HomeDestination, FavoritesDestination, SettingsDestination).forEach { tab ->
      val stack = NavBackStack<NavKey>(
        tab,
        EmailDetailDestination(emailId = 42L),
      )

      assertEquals(stack.toList(), roundTrip(stack).toList(), "$tab 未能往返")
    }
  }

  @Test
  fun `登录页 key 里嵌着的 redirectTo 能往返`() {
    val stack = NavBackStack<NavKey>(
      LoginDestination(redirectTo = EmailDetailDestination(emailId = 7L)),
    )

    assertEquals(stack.toList(), roundTrip(stack).toList())
  }

  @Test
  fun `redirectTo 为空时能往返`() {
    val stack = NavBackStack<NavKey>(LoginDestination())

    assertEquals(stack.toList(), roundTrip(stack).toList())
  }

  private fun roundTrip(stack: NavBackStack<NavKey>): NavBackStack<NavKey> {
    return json.decodeFromString(serializer, json.encodeToString(serializer, stack))
  }
}
