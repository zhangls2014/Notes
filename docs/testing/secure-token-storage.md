# 登录凭据存储验证

更新：2026-10-07。

共享仓库测试：

```bash
./gradlew :core:data:testAndroidHostTest :core:data:iosSimulatorArm64Test
```

覆盖旧明文迁移与清除、迁移写入失败后重试、重启读取、更新头像保留凭据、退出清除、账号不匹配隔离、凭据丢失清除资料，中断迁移不覆盖较新的安全凭据、退出删除失败后不恢复旧明文会话，以及暂时无法读取安全存储时登录态订阅能够重试恢复。

`iosTest/KeychainCredentialsStoreTest` 使用随机独立 service，检查真实 Keychain 写入、重启读取、账号/凭据更新与幂等删除。当前 Gradle Kotlin/Native 独立可执行测试环境返回 `errSecNotAvailable (-25291)`，因此该集成测试明确标记 `@Ignore`，不计入通过的测试。需要在可访问 Keychain 的 iOS 应用宿主测试中启用。iOS 编译通过不能视为该集成测试通过。

`iosTest/KeychainQueryTest` 不访问 Keychain，直接检查查询字典中布尔值的 CF 类型、取值与匹配数量。Security 要求的 `CFBoolean` 必须在原生字典中设置；经过 Kotlin Map 桥接的 Boolean 会成为 NSNumber，导致读取参数错误。该测试通过普通 `:core:data:iosSimulatorArm64Test` 执行。

验收 Keychain 的 Notes 模拟器包必须使用正常 Xcode 签名流程，不加 `CODE_SIGNING_ALLOWED=NO`：缺少模拟器应用身份会返回 `errSecMissingEntitlement (-34018)`，不能据此判断参数修复无效。2026-10-07 已在独立模拟器应用身份下完成既有 Keychain CRUD 测试，并确认正常签名的 Notes Debug 包保留现有登录数据进入首页；条件和限制见 [白屏调查与修复](../audits/2026-10-07-ios-white-screen.md)。真机、锁屏及旧明文迁移的完整验收继续独立记录。

平台人工验收：

1. Android 登录后重启，确认保持登录；检查 `files/datastore/notes.preferences_pb` 无 Token，`no_backup/auth-credentials.enc` 为密文。重复保存同一凭据应产生不同密文。
2. Android 删除对应 Keystore 密钥或破坏密文，再读取鉴权凭据；确认返回未登录且持久化资料移除。磁盘写入失败不得进入登录成功导航。
3. iOS 应用宿主中运行上述 Keychain 测试；验证未解锁访问失败时凭据不被删除，重新解锁后仍可读取。
4. 两端退出后重启，确认未登录；切换账号后请求只使用新账号凭据。
5. 旧版本升级：先将存量明文迁移到安全存储，确认安全读回后原 JSON 不再含 Token。备份排除规则保护迁移前的文件，无法追溯删除已存在的旧系统备份。

Android 密文位于 noBackupFilesDir；旧 preferences 文件也被云备份和设备迁移规则排除。iOS DataStore 目录排除备份。普通设置因此不会通过该文件备份迁移。当前 iOS 使用 WhenUnlockedThisDeviceOnly；将来需要锁屏后台同步时需重新评估可访问性策略。
