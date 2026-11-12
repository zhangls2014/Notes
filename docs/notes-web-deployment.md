# Notes 网站部署与应用发布

## 站点预览与绑定

1. 从仓库根目录运行 `node --test web/build.test.mjs` 和 `node web/build.mjs`。使用全新检出目录构建，再用 `python3 -m http.server 8842 --bind 127.0.0.1 --directory web/dist` 预览。
2. 获得 GitHub 连接和发布授权后，在 Cloudflare Pages 选择 Git 集成并授权 `zhangls2014/Notes`。生产分支设为 `pages-production`，预览分支启用 `master` 与需求分支。Framework preset 设为 None，仓库根目录为构建根，构建命令为 `node web/build.mjs`，输出目录为 `web/dist`。使用 Node.js 24。
3. 在生产环境配置 `NOTES_ANDROID_SIGNING_SHA256` 为分发 APK 签名证书的 SHA-256 指纹（32 组用冒号分隔的十六进制字节），`NOTES_IOS_APP_ID` 为签名应用的完整 Application Identifier（前缀加 Bundle ID）。需要预览关联文件时在预览环境设置一对相符的值。构建会生成 `/.well-known/assetlinks.json` 和 `/.well-known/apple-app-site-association`。
4. 验收 `pages.dev` 上的构建。确认现有 DNS 归属和流量，再在 Pages 的 Custom domains 绑定 `notes.zhangls.me`。在 DNS 托管方核对目标为项目的 `pages.dev` 地址，确认 TLS 有效。两个关联文件必须以 HTTPS 直接返回 200 和 JSON。用正式签名包验收 `/home`、`/email`，联网账号的邮件深链仍需 `accountId`。
5. 经授权把生产分支快进到已验收的 `master` 提交，由 Pages 自动部署该提交。构建与域名操作见 [GitHub 集成](https://developers.cloudflare.com/pages/configuration/git-integration/github-integration/) 和 [自定义域名](https://developers.cloudflare.com/pages/configuration/custom-domains/)。

## 登录源站

按 [认证服务部署与配置清单](notes-auth-deployment.md) 部署 `auth.notes.zhangls.me`。Android 正式渠道 Release 和 iOS Release 使用该认证地址并启用 Passkey。测试构建继续使用 `NOTES_AUTH_BASE_URL` 与 `NOTES_AUTH_PASSKEYS_ENABLED`，iOS 用测试 entitlement 和测试 RP ID。在获授权的空库执行单份 `server/auth/sql/schema.sql`，并配置 SMTP 供注册和邮箱变更发码。测试库初始化、正式环境部署与公开发布分别按仓库规则授权。

## 发布新构建

1. 按 [版本规则](app-releases.md) 准备版本、构建号与发布日期，完成依赖清单和两端安装包构建。Android 用 `apksigner verify --print-certs` 核对签名和包的渠道，iOS 核对商店版本和签名标识。
2. 从每个正式分发包提取并校验两份声明 HTML。例如，`python3 web/archive-notices.py Notes.apk android-prd-full 1.1.2 5`。iOS 使用导出的 IPA 和 `ios-app-store`。用相同参数追加 `--verify` 可与仓库中的归档逐字节核对。历史归档持续随网站部署，网页历史的每个版本按渠道和构建号链接到已登记的声明归档。
3. 将 Android APK 上传到 R2 并绑定公开下载域名 `downloads.zhangls.me`，对象路径为 `/<channel>/<build>/<filename>.apk`。下载验证与原包的 SHA-256 一致，实机覆盖安装成功。iOS 验证 App Store 目标地区的正式版本可获取。
4. 对已可获取的渠道，在 [发布构建清单](../web/published-builds.json) 末尾追加 `channel`、`version`、字符串 `build` 和 `artifactUrl`。Android 填 R2 的 APK 地址，iOS 填 `https://apps.apple.com/…` 商店地址。沿用已公布的归档和记录。各渠道最新条目分别决定线上清单，尚未发布过的渠道暂时没有清单。
5. 验收预览中的中英文历史、平台筛选、版本锚点、构建归档和下载地址。获授权后部署生产并验证 JSON、下载及 App 内检查结果。若新渠道地址不可用，先修复资源或用授权的新生产发布恢复该渠道的上一份清单。

## 验收记录

- 网站预览和正式域名的内容及关联文件分别记录。
- 登录验证包括密码、Passkey 注册和登录、会话刷新、退出、断网与限流。使用实机和正式签名等效环境。
- 更新验证包括最新版本、可更新、超时及重试，包内历史和开源声明离线可用。按仓库测试指南记录 Android 宿主测试、构建、iOS 编译和设备结果。
