dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
  versionCatalogs {
    create("kmp") {
      from(files("../gradle/kmp.versions.toml"))
    }
  }
}

rootProject.name = "build-logic"
