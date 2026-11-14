package me.zhangls.convention

import java.util.Properties
import org.gradle.api.Project
import org.gradle.api.provider.Provider

fun Project.localBuildProperties(name: String): Provider<Properties> =
  providers.fileContents(rootProject.layout.projectDirectory.file(".local/$name")).asText
    .orElse("")
    .map { text -> Properties().apply { load(text.reader()) } }

fun Project.notesServiceProperty(name: String, environmentVariable: String): Provider<String> =
  providers.gradleProperty(name)
    .orElse(providers.environmentVariable(environmentVariable))
    .orElse(localBuildProperties("server.properties").map { it.getProperty(name) })

fun Provider<String>.requiredSetting(name: String, localFile: String): String =
  requireNotNull(orNull) { "Set $name in .local/$localFile or supply a build override" }
