# iOS 持续白屏调查

日期：2026-10-07

状态：已确认根因并修复；Native 类型回归、共享数据回归、模拟器独立应用身份下的真实 Keychain CRUD，以及重新构建的 Notes Debug 首屏验收通过。真机与官方支持的 Xcode 26.4 环境未验收。

## 结论

`KeychainCredentialsStore.read()` 构造查询时，将 `kCFBooleanTrue` 经 `CFBridgingRelease` 放入 Kotlin Map，再通过 `CFBridgingRetain` 转回字典。实际传给 Security 的 `kSecReturnData` 值是 `ComposeAppBoolean`，不是 API 要求的原生 `CFBoolean`。`SecItemCopyMatching` 因此返回 `errSecParam (-50)`。

`UserRepositoryImpl.userFlow` 捕获异常并延迟重试，无法发出登录状态；`AppViewModel` 的 `isLogin` 保持初始 `null`，`AppNavHost` 在这个状态直接返回，因此应用持续白屏。

这次问题与 2026-10-02 的秒级启动加载延迟不同。当前查询失败的具体机制已经确认，但没有进行旧工具链/旧安装包对照，不能认定由某一项最近依赖升级引入。

## 初次调查环境与复现

- 检查源码 HEAD：`4c33e11d`，调查开始时工作区干净。
- Xcode：27.0（27A266a）。
- 模拟器：iPhone 18 Pro，iOS 27.0，UDID `6339160B-A0ED-4AD3-B1F9-F0B99921100F`。
- 应用：`me.zhangls.notes.Notes`，已安装 Debug 包，二进制文件时间为 2026-10-07 01:54。本次未重新构建，不将其认定为经过独立核实的 HEAD 构建产物；源码位置与运行时符号吻合。
- 初次采集时进程已运行约 10 分钟，截图仍为白屏。随后保留数据重新启动，继续复现。
- 未清理 DataStore、数据库或 Keychain，未卸载应用，未修改模拟器尺寸、密度或外观。

## 运行证据

1. 对白屏进程做 2 秒 `sample`：主线程处于 SwiftUI/UIKit 正常事件循环，没有停留在入口前 dyld 加载阶段。
2. LLDB 读取视图层级：SwiftUI 宿主、ComposeContainerView 和 SurfaceMetalView 都已创建，frame 为 `(0, 0, 402, 874)`。没有发现宿主内容尺寸为零的证据。
3. 根导航断点进入 `AppNavHost.kt` 的登录状态判断；设置流到达 `AppViewModel` 的状态更新，用户流进入异常重试。
4. 读取异常类型和消息（未打印凭据）：`IllegalStateException`，`Keychain read failed (-50)`。
5. `IosSecureTokenStore.kt` 中 Security 调用后的局部变量为 `status = -50`。
6. 在同一应用进程内，用原生字典和相同 class、service、account、synchronizable、returnData、matchLimit 查询，返回 `0`。说明该进程能够读取现有凭据，不能归因为凭据不存在或当前权限不足。

## 参数逐字段对照与最小实验

实际 Kotlin 桥接字典的内容：

| Security 字段 | 实际值 | 对照 |
| --- | --- | --- |
| `kSecClass` | `genp` | 原生查询一致 |
| `kSecAttrService` | `me.zhangls.notes.auth.v1` | 原生查询一致 |
| `kSecAttrAccount` | `current-session` | 原生查询一致 |
| `kSecMatchLimit` | `m_LimitOne` | 原生查询一致 |
| `kSecReturnData` | `1`，类为 `ComposeAppBoolean` | 原生值为 `kCFBooleanTrue` |
| `kSecAttrSynchronizable` | `0`，类为 `ComposeAppBoolean` | 原生值为 `kCFBooleanFalse` |

`CFGetTypeID` 测得 `kSecReturnData` 的实际对象类型 ID 为 `22`，同进程的 `CFBooleanGetTypeID()` 为 `21`。数字属于此次运行证据，不应在修复中硬编码。

在 LLDB 中创建原查询的原生可变字典副本，仅做内存内只读查询对照；每次查询都释放返回对象，不输出凭据内容：

| 查询变体 | `SecItemCopyMatching` 返回值 |
| --- | ---: |
| 原字典字段原样复制 | `-50` |
| 仅将 `kSecReturnData` 替换为原生 `kCFBooleanTrue` | `0` |
| 再将 `kSecAttrSynchronizable` 替换为原生 `kCFBooleanFalse` | `0` |

因此这次读取失败的直接触发字段是 `kSecReturnData`。`synchronizable` 同样存在桥接类型差异，但本次没有证据证明它单独导致读取失败。

Apple 的 [`kSecReturnData`](https://developer.apple.com/documentation/security/ksecreturndata) 文档明确要求对应值为 `CFBoolean`；[`errSecParam`](https://developer.apple.com/documentation/security/errsecparam) 表示传入参数不合法。

## 修复与回归

`keychainQuery()` 在桥接非布尔字段后创建原生可变 CF 字典，直接设置 `kCFBooleanTrue` / `kCFBooleanFalse`，避免布尔常量经过 Kotlin Map 的拆箱、装箱和再次桥接。读取、写入和清除共用该构造函数；临时桥接字典和返回字典保持 retain/release 配对。未更改凭据格式、service、account、可访问性、用户流重试策略或导航行为。

新增 `KeychainQueryTest` 检查 Security 实际消费的 CF 类型与布尔值，以及读取数量约束。保留旧桥接行为时，两条测试均失败：要求的类型为 `21`、实际为 `22`；改为原生常量后通过。这些测试不访问 Keychain，因此能够在普通 Native 测试环境防止类型回归。

执行以下回归（已有 JBR 25，Android SDK 由环境变量提供，Kotlin/Gradle 堆 8GB，最多 4 workers）：

```bash
./gradlew :core:data:iosSimulatorArm64Test :composeApp:iosSimulatorArm64Test \
  :composeApp:compileKotlinIosSimulatorArm64 :core:data:testAndroidHostTest
```

- 数据层 iOS：10 通过，1 个既有真实 Keychain 集成用例跳过。
- 组合根 iOS：29 通过，包含真实 Entry 调用与 Preference 初始化。
- 数据层 Android 宿主：8 通过。
- iOS 编译与完整 Xcode Debug 构建通过。环境为本机 Xcode 27.0，不扩大项目的官方工具链验收范围。

### 真实 Keychain 与首屏验收

在临时独立模拟器应用 `me.zhangls.notes.KeychainTests` 中运行既有 `keychainRoundTripUpdateAndDeletion`：1 项通过，覆盖空查询、新增、重新创建 store 后读取、更新账号/凭据、清除与幂等清除。测试使用随机独立 service，不访问 Notes 的真实 session。

临时诊断构建仅去掉测试的 `@Ignore`，通过 `/private/tmp` 的 Gradle init script 为测试可执行文件嵌入 `__TEXT,__entitlements` 中的独立 `application-identifier`（与 Xcode 生成的模拟器身份机制一致），然后作为 `.app` 安装运行。测试后已恢复仓库的 `@Ignore`；普通独立可执行测试仍不能当作有 Keychain 权限的应用宿主。未修改项目签名或构建配置。

首次 Notes 构建采用 `CODE_SIGNING_ALLOWED=NO`，运行时发现 `-34018` 权限错误，该轮白屏不计为修复验收。去掉该参数，以现有 Xcode 工程正常模拟器签名重新构建后，在保留现有用户数据的条件下成功进入邮件首页；再次终止并重启后仍进入首页。禁用签名的独立测试宿主同样不具备 Keychain 身份；仅 ad-hoc 签名也不能替代模拟器的嵌入身份声明。未将这些环境错误当作代码修复失败或通过证据。

有效完整构建命令：

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,id=6339160B-A0ED-4AD3-B1F9-F0B99921100F' \
  -derivedDataPath /private/tmp/notes-keychain-fixed-derived build
```

独立代码审查未发现阻塞问题。真机、Release、全新安装与锁屏后的凭据访问仍未验收；本次不改变其他平台验收事项的状态。

临时采集文件：`/private/tmp/notes-white-screen-current.png`、`/private/tmp/notes-white-screen-relaunch.png`、`/private/tmp/notes-white-screen-current.sample.txt`。临时文件可能被系统清理。

修复证据：`/private/tmp/notes-keychain-green.log`、`notes-keychain-xcode-signed.log`、`notes-keychain-fixed-signed-home.png`；独立宿主源配置与二进制在 `/private/tmp/notes-keychain-host-20261007/`，运行结果在 `/private/tmp/notes-keychain-host-run.log`。
