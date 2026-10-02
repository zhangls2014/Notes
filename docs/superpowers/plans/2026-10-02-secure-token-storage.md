# Secure Token Storage Implementation Plan

Goal: 登录凭据由平台安全存储保护，用户资料不携带 Token。
Architecture: core:data 内部 SecureTokenStore 负责成组持久化；UserRepository 串行协调安全存储和旧资料迁移。组合根将 TokenProvider 接入仓库。
Tech Stack: Kotlin Multiplatform、DataStore、Koin、Android Keystore / AtomicFile、iOS Keychain。

- [x] 添加仓库回归测试，运行 :core:data:iosSimulatorArm64Test 验证缺失安全存储契约时失败。
- [x] 新增 AuthTokens、内部 StoredCredentials / SecureTokenStore，拆分 UserModel 与 login/getTokens 契约。
- [x] 实现 Android noBackup 密文与 iOS Keychain；仓库互斥协调迁移、登录、退出和资料更新。
- [x] 接入 LoginViewModel / TokenProvider，禁用 Ktor 鉴权缓存，排除旧凭据文件备份。
- [x] 执行 commonTest、Android assembleDebug 与 iOS 编译，修复发现的问题。
- [x] 更新架构说明，审查差异与安全失败路径。此工作树原地实施，不合入 master。

验证记录：7 个共享回归测试在 Android 宿主与 iOS 模拟器执行；Keychain 真实集成测试因独立执行环境返回 -25291 明确忽略，平台人工验收步骤见 docs/testing/secure-token-storage.md。
