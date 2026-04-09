rootProject.name = "Notes"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
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
    mavenCentral()
  }

  versionCatalogs {
    create("kmp") {
      from(files("gradle/kmp.versions.toml"))
    }
  }
}

include(":androidApp")
include(":iosApp")
include(":composeApp")
include(":core:data")
include(":core:theme")
include(":core:network")
include(":core:framework")
include(":feature:main")
include(":feature:login")
include(":feature:email")
include(":feature:settings")
include(":android:output:login")
include(":android:baselineprofile")
