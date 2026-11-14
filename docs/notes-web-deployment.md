# Notes 网站部署与应用发布

## 服务地址配置

| 用途 | 配置 |
|---|---|
| 客户端认证 | Gradle 属性 `notes.authBaseUrl` 或环境变量 `NOTES_AUTH_BASE_URL` |
| 客户端官网和 Android App Links | Gradle 属性 `notes.siteBaseUrl` 或环境变量 `NOTES_SITE_BASE_URL` |
| 网站生成的更新清单 | 环境变量 `NOTES_SITE_BASE_URL` |
| 网站接受的 Android APK 下载来源 | 环境变量 `NOTES_APK_BASE_URL` |
| 独立部署的已发布构建清单 | 环境变量 `NOTES_PUBLISHED_BUILDS_FILE` 指向该环境的 JSON 文件 |

Gradle 属性优先于同用途的环境变量。正式默认值在 [Gradle 配置](../gradle.properties) 和 [网站构建入口](../web/build.mjs)。
官网和认证配置均使用不带路径、端口或凭据的 HTTPS origin，构建时校验。更换 APK 来源时，发布清单中的 `artifactUrl` 须指向该来源；
生成器继续校验来源、渠道、构建号和文件名。

Xcode 构建使用 `NOTES_SITE_HOST` 和 `NOTES_PASSKEY_RP_ID` 生成签名关联域名，并在
未设置 URL 环境变量时为共享框架派生 HTTPS 地址。若用 Gradle 属性、环境变量或预构建框架
覆盖 URL，应同步配置 Xcode 的主机名。由 Xcode 构建共享框架时，Gradle 校验最终生效的官网和启用 Passkey 的认证主机名。使用预构建框架时，在签名包验收中核对这些值。测试地址也应提供匹配当前应用签名的关联文件。


## 站点预览与绑定

1. 从仓库根目录运行 `node --test web/build.test.mjs` 和 `node web/build.mjs`。使用全新检出目录构建，再用 `python3 -m http.server 8842 --bind 127.0.0.1 --directory web/dist` 预览。
2. 获得 GitHub 连接和发布授权后，在 Cloudflare Pages 选择 Git 集成并授权 `zhangls2014/Notes`。生产分支设为 `pages-production`，预览分支启用 `master` 与需求分支。Framework preset 设为 None，仓库根目录为构建根，构建命令为 `node web/build.mjs`，输出目录为 `web/dist`。使用 Node.js 24。
3. 在生产环境配置 `NOTES_ANDROID_SIGNING_SHA256` 为分发 APK 签名证书的 SHA-256 指纹（32 组用冒号分隔的十六进制字节），`NOTES_IOS_APP_ID` 为签名应用的完整 Application Identifier（前缀加 Bundle ID）。需要预览关联文件时在预览环境设置一对相符的值。构建会生成 `/.well-known/assetlinks.json` 和 `/.well-known/apple-app-site-association`。
4. 验收 `pages.dev` 上的构建。确认现有 DNS 归属和流量，再在 Pages 的 Custom domains 绑定 `notes.zhangls.me`。在 DNS 托管方核对目标为项目的 `pages.dev` 地址，确认 TLS 有效。两个关联文件必须以 HTTPS 直接返回 200 和 JSON。用正式签名包验收 `/home`、`/email`，联网账号的邮件深链仍需 `accountId`。
5. 经授权把生产分支快进到已验收的 `master` 提交，由 Pages 自动部署该提交。构建与域名操作见 [GitHub 集成](https://developers.cloudflare.com/pages/configuration/git-integration/github-integration/) 和 [自定义域名](https://developers.cloudflare.com/pages/configuration/custom-domains/)。

## 登录源站

按 [认证服务部署与配置清单](notes-auth-deployment.md) 部署 `auth.notes.zhangls.me`。该地址用于 Android 和 iOS 所有构建的默认注册和登录。测试服务的构建参数与应用关联按认证部署清单配置。在获授权的空库执行单份 `server/auth/sql/schema.sql`，并配置 SMTP 供注册和邮箱变更发码。测试库初始化、正式环境部署与公开发布分别按仓库规则授权。

## 发布新构建

当前生产环境由曼谷服务器的 Nginx 托管官网和 APK。官网目录为 `/var/www/notes-site`，APK 目录为 `/var/www/notes-downloads`，下载域名使用独立的 Let's Encrypt 证书。下文的 R2 和 Pages 是托管替代方案。现有生产环境先投放并从公网验证 APK，再更新官网页面与更新清单。若客户端变更依赖新的认证规则，须先部署并验证认证服务。

1. 按 [版本规则](app-releases.md) 准备版本、构建号与发布日期，完成依赖清单和两端安装包构建。Android 用 `apksigner verify --print-certs` 核对签名和包的渠道，iOS 核对商店版本和签名标识。
2. 确认当前开源声明已按[维护指南](../web/open-source/README.md)更新。官网和应用按平台使用固定地址，所有版本共用最新声明；发布流程无需创建构建归档。
3. 将 Android APK 放入 `downloads.zhangls.me` 对应的下载目录或 R2 bucket，公开路径为 `/<channel>/<build>/<filename>.apk`。文件名只使用字母、数字、点、下划线和连字符。从公网下载并与原包核对 SHA-256，验证实机覆盖安装。iOS 验证 App Store 目标地区的正式版本可获取。
4. 对已可获取的渠道，在 [发布构建清单](../web/published-builds.json) 末尾追加 `channel`、`version`、字符串 `build` 和 `artifactUrl`。Android 填 R2 的 APK 地址，iOS 填 `https://apps.apple.com/…` 商店地址。保留已公布的构建记录。各渠道最新条目分别决定线上清单，尚未发布过的渠道暂时没有清单。
5. 验收预览中的中英文历史、平台筛选、版本锚点、固定声明入口、旧声明链接跳转和下载地址。获授权后部署生产并验证 JSON、下载及 App 内检查结果。若新渠道地址不可用，先修复资源或用授权的新生产发布恢复该渠道的上一份清单。

## 验收记录

- 网站预览和正式域名的内容及关联文件分别记录。
- 登录验证包括密码、Passkey 注册和登录、会话刷新、退出、断网与限流。使用实机和正式签名等效环境。
- 更新验证包括最新版本、可更新、超时及重试，版本匹配摘要离线可用，开源声明在系统浏览器中在线可用。按仓库测试指南记录 Android 宿主测试、构建、iOS 编译和设备结果。
