package me.zhangls.framework.nav

import kotlinx.serialization.modules.PolymorphicModuleBuilder


/**
 * @author zhangls
 */
interface DestinationSerializationProvider {
  fun register(builder: PolymorphicModuleBuilder<Destination>)
}