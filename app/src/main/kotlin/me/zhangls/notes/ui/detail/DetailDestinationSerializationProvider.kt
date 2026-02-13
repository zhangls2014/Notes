package me.zhangls.notes.ui.detail

import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.subclass
import me.zhangls.framework.nav.Destination
import me.zhangls.framework.nav.DestinationSerializationProvider

/**
 * @author zhangls
 */
class DetailDestinationSerializationProvider : DestinationSerializationProvider {
  override fun register(builder: PolymorphicModuleBuilder<Destination>) {
    builder.subclass(DetailDestination::class)
  }
}