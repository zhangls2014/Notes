plugins {
  alias(kmp.plugins.android.test)
  alias(kmp.plugins.androidx.baselineprofile)
}

android {
  namespace = "me.zhangls.baselineprofile"
  buildToolsVersion = kmp.versions.android.buildTools.get()

  compileSdk {
    version = release(kmp.versions.android.compileSdk.get().toInt())
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  defaultConfig {
    minSdk = kmp.versions.android.minSdk.get().toInt()
    targetSdk = kmp.versions.android.targetSdk.get().toInt()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  targetProjectPath = ":androidApp"

  flavorDimensions += listOf("environment")
  productFlavors {
    create("dev") { dimension = "environment" }
  }
}

baselineProfile {
  useConnectedDevices = true
}

dependencies {
  implementation(kmp.androidx.test.ext.junit)
  implementation(kmp.androidx.test.espresso)
  implementation(kmp.androidx.test.uiautomator)
  implementation(kmp.androidx.benchmark.macro.junit4)
}

androidComponents {
  onVariants { v ->
    val artifactsLoader = v.artifacts.getBuiltArtifactsLoader()
    v.instrumentationRunnerArguments.put(
      "targetAppId",
      v.testedApks.map { artifactsLoader.load(it)?.applicationId }
    )
  }
}
