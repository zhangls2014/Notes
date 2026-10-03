package me.zhangls.convention

/**
 * 从模块路径推导 android namespace：
 * - :core:data -> me.zhangls.data
 * - :feature:login-api -> me.zhangls.login.api
 * - :composeApp -> me.zhangls.entry（历史包名）
 */
fun deriveNamespace(projectPath: String): String {
  val segments = projectPath.split(":").filter { it.isNotEmpty() }
  return when (segments.first()) {
    "composeApp" -> "me.zhangls.entry"
    else -> "me.zhangls." + segments.drop(1).joinToString(".") { it.replace("-", ".") }
  }
}

/**
 * 从模块路径推导 iOS framework baseName：
 * - :core:data -> DataKit
 * - :feature:login-api -> LoginApiKit
 * - :composeApp -> ComposeApp
 */
fun deriveFrameworkName(projectPath: String): String {
  val segments = projectPath.split(":").filter { it.isNotEmpty() }
  if (segments.first() == "composeApp") return "ComposeApp"
  val camel = segments.drop(1).joinToString("") { segment ->
    segment.split("-").joinToString("") { part -> part.replaceFirstChar { it.uppercase() } }
  }
  return camel + "Kit"
}
