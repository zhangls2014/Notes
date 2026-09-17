plugins {
  alias(kmp.plugins.android.fusedlibrary)
}

androidFusedLibrary {
  namespace = "me.zhangls.login.output"
  minSdk {
    version = release(kmp.versions.android.minSdk.get().toInt())
  }
}

dependencies {
  include(projects.core.data)
  include(projects.core.theme)
  include(projects.core.framework)
  include(projects.feature.login)
}