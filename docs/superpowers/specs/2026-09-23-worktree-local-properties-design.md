# Worktree 本地构建设计

日期：2026-09-23
状态：已实施；验证过程中修正了根因判断

## 背景

`local.properties` 不受 Git 跟踪，因此新建 Git worktree 时不会出现。当前
`androidApp/build.gradle.kts` 曾从该文件读取可选的 release 签名配置。
隔离工作树验证表明，缺文件本身不会使 debug 构建失败：在没有
`ANDROID_HOME` 的进程里，Android Gradle Plugin 找不到 SDK；显式提供该变量后，
原有 debug 构建即可成功。

## 目标

- 新建 worktree 后，无需复制或链接 `local.properties` 即可执行 Android debug
  构建及不需要签名的其他 Gradle 任务。
- SDK 路径使用机器级 `ANDROID_HOME` 配置，由所有 worktree 共享。
- release 签名信息不进入仓库，并同时支持开发机和 CI。
- 缺少 release 签名信息时不影响 debug 构建；需要签名的 release 构建不得产生
  误导性的已签名产物。

## 方案

四项签名配置分别从 Gradle 属性读取，以同名大写环境变量作为回退，并继续支持
已有 `local.properties` 中的 `signing.*` 值，避免现有 release 签名失效：

| Gradle 属性 | 环境变量 |
| --- | --- |
| `notes.signing.path` | `NOTES_SIGNING_PATH` |
| `notes.signing.storePassword` | `NOTES_SIGNING_STORE_PASSWORD` |
| `notes.signing.keyAlias` | `NOTES_SIGNING_KEY_ALIAS` |
| `notes.signing.keyPassword` | `NOTES_SIGNING_KEY_PASSWORD` |

新开发机将 Gradle 属性保存在 `~/.gradle/gradle.properties`；CI 使用机密环境变量。
优先级依次为 Gradle 属性、环境变量、现有 `local.properties`。
只有四项配置全部存在且非空时才创建并关联 release signing config。SDK 位置不在
项目构建脚本中另行解析，交由 Android Gradle Plugin 从 `ANDROID_HOME` 或 IDE
生成的本地配置发现。

## 文档

README 的本地配置章节改为说明 `ANDROID_HOME` 和用户级 Gradle 属性。保留
`local.properties` 的忽略规则，因为 Android Studio 仍可能自动生成它。

## 验证

1. 在新建且无 `local.properties` 的隔离工作树执行 debug 构建，区分 SDK 发现
   问题和签名配置问题。
2. 显式设置有效的 `ANDROID_HOME` 后执行 `:androidApp:assembleDebug`。
3. 检查 Git diff，确认未改动或提交任何真实签名凭据。
