plugins {
  alias(libs.plugins.android.fusedlibrary)
}

androidFusedLibrary {
  namespace = "me.zhangls.login"
  minSdk {
    version = release(libs.versions.minSdk.get().toInt())
  }
}

dependencies {
  include(projects.core.data)
  include(projects.core.theme)
  include(projects.core.framework)
  include(projects.feature.login)
}