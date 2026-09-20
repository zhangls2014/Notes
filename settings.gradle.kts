rootProject.name = "Notes"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
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
include(":composeApp")
include(":core:model")
include(":core:database")
include(":core:data")
include(":core:theme")
include(":core:network")
include(":core:framework")
include(":feature:main")
include(":feature:main-api")
include(":feature:login")
include(":feature:login-api")
include(":feature:email")
include(":feature:email-api")
include(":feature:settings")
include(":feature:settings-api")
include(":android:output:login")
include(":android:baselineprofile")
