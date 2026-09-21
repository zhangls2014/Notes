package me.zhangls.framework.nav

import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.polymorphic

/**
 * 把一个导航目的地登记进返回栈的序列化注册表。
 *
 * `rememberNavBackStack` 以 `PolymorphicSerializer(NavKey::class)` 序列化整个返回栈，
 * 因此每个 key 都必须在 `polymorphic(NavKey::class)` 下登记 —— 漏登记的后果不是编译错误，
 * 而是配置或进程重建时抛 `SerializationException`。
 *
 * 为什么还要在 `polymorphic(Destination::class)` 下再登记一次：kotlinx 的多态查找是**按基类**
 * 匹配的（`SerializersModule.getPolymorphic(baseClass, serialName)` 只认与请求基类完全相同的
 * 登记项），登记在 `NavKey` 下并不会对 `Destination` 生效。而 `Destination` 会以**字段类型**
 * 的身份被引用（如 `LoginDestination.redirectTo`），那时代码里用的是
 * `PolymorphicSerializer(Destination::class)` —— 于是需要一份以 `Destination` 为基类的登记。
 *
 * 收拢成单个函数，是为了让"新增一个目的地"只有这一处需要记得改，而不是两处。
 */
fun <T : Destination> SerializersModuleBuilder.registerDestination(
  kClass: KClass<T>,
  serializer: KSerializer<T>,
) {
  polymorphic(NavKey::class) { subclass(kClass, serializer) }
  polymorphic(Destination::class) { subclass(kClass, serializer) }
}
