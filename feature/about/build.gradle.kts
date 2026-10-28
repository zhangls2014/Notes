plugins {
  id("me.zhangls.kmp-library")
  id("me.zhangls.kmp-compose")
  id("me.zhangls.kmp-koin")
}

kotlin {
  sourceSets {
    commonMain.dependencies {
      implementation(projects.core.theme)
      // aboutNavEntry exposes NavEffect and AboutAppInfo.
      api(projects.core.framework)
      api(projects.feature.aboutApi)
    }
    commonTest.dependencies { implementation(kmp.kotlin.test) }
  }
}

compose.resources {
  for (platform in listOf("android", "ios")) {
    val outputDirectory = layout.buildDirectory.dir("generated/open-source/${platform}Main/composeResources")
    val syncNotices = tasks.register<Sync>("sync${platform.replaceFirstChar { it.uppercaseChar() }}NoticesResources") {
      from(rootProject.layout.projectDirectory.dir("web/open-source")) {
        include("$platform.html", "$platform-licenses.html")
        into("files/open-source")
      }
      into(outputDirectory)
    }
    customDirectory("${platform}Main", syncNotices.map { outputDirectory.get() })
  }
}
