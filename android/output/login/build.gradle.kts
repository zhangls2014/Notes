plugins {
  alias(kmp.plugins.android.fusedlibrary)
  `maven-publish`
}

androidFusedLibrary {
  namespace = "me.zhangls.login.output"
  minSdk {
    version = release(kmp.versions.android.minSdk.get().toInt())
  }
}

dependencies {
  include(projects.core.model)
  include(projects.core.database)
  include(projects.core.data)
  include(projects.core.theme)
  include(projects.core.preference)
  include(projects.core.framework)
  include(projects.feature.loginApi)
  include(projects.feature.login)
}

// AGP 9.3.1 按 GAV 重建融合包的外部依赖，会丢失 BOM 的 platform 属性。
// 从实际解析结果恢复类型，使消费方无需自行排除或重声明 BOM。
configurations.named("runtimePublication") {
  withDependencies {
    val platforms = configurations.getByName("includeTransitiveResolvedRuntime")
      .incoming.resolutionResult.allComponents
      .filter { component ->
        component.variants.any {
          it.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)?.name == Category.REGULAR_PLATFORM
        }
      }
      .mapNotNull { it.moduleVersion?.let { id -> "${id.group}:${id.name}" } }
      .toSet()
    withType<ExternalModuleDependency>().configureEach {
      if ("$group:$name" in platforms) {
        attributes {
          attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.REGULAR_PLATFORM))
        }
      }
    }
  }
}

publishing {
  publications {
    register<MavenPublication>("release") {
      groupId = "me.zhangls"
      artifactId = "notes-login"
      version = "0.0.0-local"
      from(components["fusedLibraryComponent"])
    }
  }
  repositories {
    maven {
      name = "localBuild"
      url = uri(layout.buildDirectory.dir("repo"))
    }
  }
}
