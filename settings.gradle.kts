rootProject.name = "Notes"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  // Settings 插件解析早于版本目录生成，直接读取同一版本源。
  val foojayVersion = providers.fileContents(layout.settingsDirectory.file("gradle/kmp.versions.toml"))
    .asText.get().lineSequence().first { it.startsWith("foojay-resolver =") }
    .substringAfter('"').substringBefore('"')
  plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version foojayVersion
  }
  includeBuild("build-logic")
  repositories {
    google {
      mavenContent {
        includeGroupAndSubgroups("androidx")
        includeGroupAndSubgroups("com.android")
        includeGroupAndSubgroups("com.google")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention")
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google {
      mavenContent {
        includeGroupAndSubgroups("androidx")
        includeGroupAndSubgroups("com.android")
        includeGroupAndSubgroups("com.google")
      }
    }
    ivy {
      name = "Kotlin Wasm Node.js distributions"
      url = uri("https://nodejs.org/dist")
      patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
      metadataSources { artifact() }
      content { includeModule("org.nodejs", "node") }
    }
    ivy {
      name = "Kotlin Wasm Binaryen distributions"
      url = uri("https://github.com/WebAssembly/binaryen/releases/download")
      patternLayout { artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]") }
      metadataSources { artifact() }
      content { includeModule("com.github.webassembly", "binaryen") }
    }
    mavenCentral()
  }

  versionCatalogs {
    create("kmp") {
      from(files("gradle/kmp.versions.toml"))
    }
  }
}

include(":androidApp")
include(":composeApp")
include(":web")
include(":core:model")
include(":core:database")
include(":core:data")
include(":core:theme")
include(":core:preference")
include(":core:network")
include(":core:download")
include(":core:framework")
include(":core:navigation")
include(":feature:main")
include(":feature:main-api")
include(":feature:login")
include(":feature:login-api")
include(":feature:email")
include(":feature:email-api")
include(":feature:settings")
include(":feature:settings-api")
include(":feature:profile")
include(":feature:profile-api")
include(":feature:about")
include(":feature:about-api")
include(":android:output:login")
include(":android:baselineprofile")
