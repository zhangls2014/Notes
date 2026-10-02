# 本地登录凭据安全存储

日期：2026-10-02。状态：用户已批准方案，已实施。

用户资料与凭据分离：UserModel 仅包含资料；AuthTokens 作为登录边界参数，不进入 UI State。UserRepository.login 接收资料和整组凭据，getTokens 供组合根 TokenProvider 使用。SecureTokenStore 位于 core:data 内部，平台实现通过 Koin 注入。

Android 使用 Keystore AES/GCM，随机 IV，密文经 AtomicFile 写入 noBackupFilesDir。iOS 使用 Generic Password Keychain，WhenUnlockedThisDeviceOnly，关闭同步。账号与 Token 成组存储，避免串号。安全存储错误不得回退明文。永久密钥失效或密文损坏清除凭据；暂时无法访问 Keychain 时保留数据并报告错误。

旧 DataStore user JSON 中的 Token 在首次读取或修改资料时迁移：在仓库互斥锁下写入安全存储并读回校验，成功后重写仅含资料的 JSON。写入失败保留旧记录以便重试。logout 先清除旧明文字段，再清除凭据并移除资料。缺失凭据的资料不作为登录态公开。登录持久化失败不发成功导航。

Ktor 禁用自身 Token 缓存，从仓库读取当前凭据。保持 Authorization 脱敏，不记录响应体。无后端刷新协议，本次不添加猜测的刷新端点。Access Token 必须持久化以保持现有单 Token 模拟登录行为。

验证：commonTest 覆盖迁移、重启、写入失败、退出、账号不匹配及资料更新；Android 整包和 iOS 编译检查平台实现和 Koin 装配；更新架构文档。

登录态 Flow 对暂时不可访问的安全存储或迁移 I/O 失败执行可取消重试，保留最近一次已公开状态；getTokens 的失败继续抛给请求调用方，不使用旧凭据兜底。
