# Notes 网站部署与应用发布

## 服务地址配置

| 用途 | 配置 |
|---|---|
| 客户端认证 | Gradle 属性 `notes.authBaseUrl` 或环境变量 `NOTES_AUTH_BASE_URL` |
| 客户端官网和 Android App Links | Gradle 属性 `notes.siteBaseUrl` 或环境变量 `NOTES_SITE_BASE_URL` |
| 网站生成的更新清单 | 环境变量 `NOTES_SITE_BASE_URL` |
| mainland 客户端和网站生成器使用的 APK 来源 | Gradle 属性 `notes.apkBaseUrl` 或环境变量 `NOTES_APK_BASE_URL`（网站只读取环境变量） |
| 独立部署的已发布构建清单 | 环境变量 `NOTES_PUBLISHED_BUILDS_FILE` 指向该环境的 JSON 文件 |

Gradle 属性优先于同用途的环境变量。正式默认值在 [Gradle 配置](../gradle.properties) 和 [网站构建入口](../web/build.mjs)。
客户端官网、APK 和认证配置均使用不带路径、端口或凭据的 HTTPS origin，构建时校验。更换 APK 来源时，发布清单中的 `artifactUrl` 须指向该来源；
网站与安装包必须配置同一下载来源。生成器会校验来源、渠道、构建号和文件名。

Xcode 构建使用 `NOTES_SITE_HOST` 和 `NOTES_PASSKEY_RP_ID` 生成签名关联域名，并在
未设置 URL 环境变量时为共享框架派生 HTTPS 地址。若用 Gradle 属性、环境变量或预构建框架
覆盖 URL，应同步配置 Xcode 的主机名。由 Xcode 构建共享框架时，Gradle 校验最终生效的官网和启用 Passkey 的认证主机名。使用预构建框架时，在签名包验收中核对这些值。测试地址也应提供匹配当前应用签名的关联文件。


## 站点预览与绑定

1. 从仓库根目录运行 `node --test web/build.test.mjs` 和 `./gradlew --configure-on-demand -Pkotlin.js.yarn=false :web:buildSite`。使用全新检出目录构建，再用 `python3 web/preview.py --port 8842` 预览。访问不存在的路径，验证 Compose 的错误页及 HTTP 404 状态。
2. 获得 GitHub 连接和发布授权后，在 Cloudflare Pages 选择 Git 集成并授权 `zhangls2014/Notes`。生产分支设为 `pages-production`，预览分支启用 `master` 与需求分支。Framework preset 设为 None，仓库根目录为构建根，构建命令为 `./gradlew --configure-on-demand -Pkotlin.js.yarn=false :web:buildSite`，输出目录为 `web/dist`。构建环境须能运行仓库的 Gradle Wrapper。
3. 在生产环境配置 `NOTES_ANDROID_SIGNING_SHA256` 为分发 APK 签名证书的 SHA-256 指纹（32 组用冒号分隔的十六进制字节），`NOTES_IOS_APP_ID` 为签名应用的完整 Application Identifier（前缀加 Bundle ID）。需要预览关联文件时在预览环境设置一对相符的值。构建会生成 `/.well-known/assetlinks.json` 和 `/.well-known/apple-app-site-association`。
4. 验收 `pages.dev` 上的构建。确认现有 DNS 归属和流量，再在 Pages 的 Custom domains 绑定 `notes.zhangls.me`。在 DNS 托管方核对目标为项目的 `pages.dev` 地址，确认 TLS 有效。两个关联文件必须以 HTTPS 直接返回 200 和 JSON。确认 Wasm 文件的 MIME 类型为 `application/wasm`，并检查 `/releases/published.json` 仅包含已发布版本。用正式签名包验收 `/home`、`/email`，联网账号的邮件深链仍需 `accountId`。
5. 经授权把生产分支快进到已验收的 `master` 提交，由 Pages 自动部署该提交。构建与域名操作见 [GitHub 集成](https://developers.cloudflare.com/pages/configuration/git-integration/github-integration/) 和 [自定义域名](https://developers.cloudflare.com/pages/configuration/custom-domains/)。

## 登录源站

按 [认证服务部署与配置清单](notes-auth-deployment.md) 部署 `auth.notes.zhangls.me`。该地址用于 Android 和 iOS 所有构建的默认注册和登录。测试服务的构建参数与应用关联按认证部署清单配置。在获授权的空库执行单份 `server/auth/sql/schema.sql`，并配置 SMTP 供注册和邮箱变更发码。测试库初始化、正式环境部署与公开发布分别按仓库规则授权。

## 发布新构建

当前生产环境由曼谷服务器的 Nginx 托管官网和 APK。官网目录为 `/var/www/notes-site`，APK 目录为 `/var/www/notes-downloads`，下载域名使用独立的 Let's Encrypt 证书。下文的 R2 和 Pages 是托管替代方案。现有生产环境先投放并从公网验证 APK，再更新官网页面与更新清单。若客户端变更依赖新的认证规则，须先部署并验证认证服务。

1. 按 [版本规则](app-releases.md) 准备版本、构建号与发布日期，完成依赖清单和两端安装包构建。Android 用 `apksigner verify --print-certs` 核对签名和包的渠道，iOS 核对商店版本和签名标识。
2. 确认当前开源声明已按[维护指南](../tools/open-source/README.md)更新。官网与应用打开相同固定地址的 Kotlin/Wasm 声明；发布前在支持该 Wasm 产物的浏览器和应用 WebView 验证列表、详情拦截和返回滚动位置。
3. 将 mainland 正式 APK 放入下载源的 `/android-prd-mainland/<build>/Notes-<version>-mainland.apk`。从公网下载并与原包核对 SHA-256，验证实机覆盖安装。global 暂只公告版本，不分发 APK；iOS 验证 App Store 目标地区的正式版本可获取。
4. 用新版本更新 [发布构建记录](../web/published-builds.json) 中相应渠道的 `channel`、`version` 和字符串 `build`。每个现行渠道只保留当前构建，构建号须继续递增。mainland 另填符合上一步约定的 `artifactUrl`，iOS 填 `https://apps.apple.com/…`；global 不填 `artifactUrl`。历史版本继续由更新日志记录，此步骤不删除已上传的 APK。使用全新输出目录构建，避免保留旧清单；本地可用 `NOTES_WEB_OUTPUT_DIR` 指定空目录。
5. 验收预览中的中英文历史、平台筛选、版本锚点、固定声明入口和 APK 直达链接。产物应不含按构建号生成的 APK 下载页和按版本生成的声明跳转页。mainland v1 更新清单保留已安装客户端校验所需的旧路径字段；客户端从配置的 APK 源下载。获授权后部署生产并验证 JSON、下载及 App 内检查结果。若新渠道地址不可用，先修复资源或用授权的新生产发布恢复该渠道的上一份清单。

Nginx 应将不存在的路径交给 `404.html`（例如 `error_page 404 /404.html;`），并保留 HTTP 404 状态。上线验收需用不存在的深层路径检查状态和页面渲染。

官网构建生成 `/web-assets/manifest-v2.json`，记录 Skiko Wasm 和 MiSans 的路径、字节数和完整 SHA-256。MiSans 正式文件名使用摘要的前 20 个十六进制字符。官网产物仅包含新版清单和 20 字符字体文件。上线后确认旧版 `/web-assets/manifest.json` 和 64 字符字体路径均返回 404。只支持旧版清单的客户端将跳过预缓存，由 WebView 通过 HTTPS 加载现行页面资源。部署时先上传清单引用的资源，再更新 HTML、启动脚本和清单。保持清单可重新验证，避免客户端持续读到旧版本。上线后按清单验证两项资源的大小、SHA-256 和响应类型，并以 `Range: bytes=0-0`、`Accept-Encoding: identity` 对公网地址发 GET，确认返回 `206 Partial Content`、`Content-Range: bytes 0-0/<清单大小>` 和恰好一个字节。返回 `200` 时客户端仍可校验并缓存完整文件，但该源站不能按分片续传。未命中本地文件的客户端直接使用相同的 HTTPS 资源。

### 官网缓存策略

HTML、资源清单、公开更新记录以及其他固定地址用 `Cache-Control: no-cache`，允许客户端用 `ETag` 或 `Last-Modified` 校验后继续复用。构建按内容生成 JS、源映射、Wasm、MiSans 和 Compose 资源地址。同一个带指纹的地址始终提供同一份文件，响应使用 `public, max-age=31536000, immutable`。发布时先上传带指纹的文件，再切换 HTML 和清单。保留上一版 HTML 依赖的文件，供尚未刷新的页面完成加载。

Cloudflare Pages 和本地预览使用构建产物中的 `_headers`。曼谷 Nginx 源站需要在 `http` 上下文定义以下规则，并在官网 HTTPS `server` 应用该响应头。若该站点的 `location` 已声明自己的 `add_header`，也须在该 `location` 添加相同的缓存头。404 响应使用 `no-cache`。

```nginx
map "$status:$uri" $notes_cache_control {
    default "no-cache";
    "~^(200|206|304):/([a-f0-9]{20}\.wasm|notes-[a-f0-9]{64}\.js(\.map)?|web-assets/misans-[a-f0-9]{20}\.otf|assets/[a-f0-9]{64}/[^/]+)$" "public, max-age=31536000, immutable";
}
```

```nginx
add_header Cache-Control $notes_cache_control always;
```

上线时验证首页和清单返回 `no-cache`，带指纹的资源返回 `immutable`。用首次请求返回的校验器再次请求 HTML，确认未变更时返回 304。部署新构建后，用同一个 WebView 再打开页面并检查网络请求，确认新 HTML 引用新版文件、未变化的文件继续命中缓存。

## 验收记录

- 网站预览和正式域名的内容及关联文件分别记录。
- 登录验证包括密码、Passkey 注册和登录、会话刷新、退出、断网与限流。使用实机和正式签名等效环境。
- 更新验证包括最新版本、可更新、超时及重试，版本匹配摘要离线可用，开源声明在系统浏览器中在线可用。按仓库测试指南记录 Android 宿主测试、构建、iOS 编译和设备结果。
