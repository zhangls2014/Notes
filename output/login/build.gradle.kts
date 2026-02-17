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
  include(project(":core:data"))
  include(project(":core:theme"))
  include(project(":core:framework"))
  include(project(":feature:login"))
}