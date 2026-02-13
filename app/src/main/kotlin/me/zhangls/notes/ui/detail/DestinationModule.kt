package me.zhangls.notes.ui.detail

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import me.zhangls.framework.nav.DestinationSerializationProvider

/**
 * @author zhangls
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DestinationModule {
  @Binds
  @IntoSet
  abstract fun bindDetailProvider(impl: DetailDestinationSerializationProvider): DestinationSerializationProvider
}