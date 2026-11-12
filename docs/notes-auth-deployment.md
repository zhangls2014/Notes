# Notes 认证服务部署与配置清单

本清单用于首次发布。客户端和服务端应配套验收。切换后，旧客户端提交用户名的密码请求和未提交邮箱的 Passkey 登录请求会被拒绝。部署示例采用 **Linux + systemd + Nginx + PostgreSQL + SMTP**，认证源站为 `auth.notes.zhangls.me`。命令按同机 PostgreSQL 编写，路径、数据库名、系统账户名和 SMTP 参数可按实际环境替换。

| 地址或组件 | 职责 |
| --- | --- |
| `https://auth.notes.zhangls.me` | 注册发码、邮箱密码登录、Passkey、会话、个人资料与头像 |
| `127.0.0.1:8080` | Node.js 认证进程，由同机 Nginx 转发 |
| `127.0.0.1:5432` | 本示例的 PostgreSQL，仅本机可达 |
| `https://notes.zhangls.me` | Cloudflare Pages 官网、版本清单、历史更新、开源声明与应用导航深链 |
| `https://downloads.zhangls.me` | R2 上的 Android APK 下载域名 |

生产和测试分别准备域名、数据库、SMTP 凭据、验证码密钥和签名标识。测试可使用 `auth-staging.zhangls.me` 作为示例域名，以下涉及域名的 DNS、证书、RP ID、Origin 和签名关联文件均替换为对应环境的值。实际空库初始化、域名切换与生产上线按仓库约定分别授权。官网与 APK 发布步骤见 [网站部署与应用发布](notes-web-deployment.md)。

## 1. 准备主机、域名和发布代码

- [ ] 记录目标环境、源站 IP、发布提交、数据库名、测试邮箱、Android 分发 APK 和 iOS 签名包。优先在测试环境走完本清单。
- [ ] 安装 Node.js **24.x**、npm、PostgreSQL 及客户端、Nginx、OpenSSL、curl 和证书签发工具。确认系统时间同步，检查 `node --version`、`npm --version`、`psql --version` 与 `nginx -v`。
- [ ] 将认证域名的 A 记录指向源站。仅在 IPv6 可达时配置 AAAA 记录。本示例使用直连 Nginx。DNS 由 Cloudflare 管理时选择 **仅 DNS**，使 Nginx 直接获得客户端 IP。使用其他前置代理的环境需另行配置可信代理和真实 IP 解析。
- [ ] 对外开放 HTTPS 443。HTTP 80 可用于证书签发和 HTTPS 跳转。Node.js 8080 和本机 PostgreSQL 5432 只监听本地。确认源站可出站访问 SMTP 服务商指定的端口。
- [ ] 用部署账号将已确认的提交检出到 `/opt/notes/releases/<release>`。在源站或同平台环境安装依赖，确保 `sharp` 的可选平台依赖一同安装。

在所检出的仓库根目录运行：

```bash
git rev-parse HEAD
cd server/auth
npm ci --include=optional
npm run check
npm test
node --input-type=module -e 'import sharp from "sharp"; console.log(sharp.versions.vips)'
```

- [ ] 有授权测试库时，按下一节初始化并配置 `NOTES_AUTH_TEST_DATABASE_URL` 后重跑 `npm test`，确认 PostgreSQL 用例实际执行。使用同一份测试库运行测试和清理，不把生产库用作测试库。服务依赖和接口以 [package.json](../server/auth/package.json) 与 [OpenAPI](../server/auth/openapi.yaml) 为准。

## 2. 一次性初始化 PostgreSQL 空库

本地 PostgreSQL 示例使用独立角色 `notes_auth` 作为数据库 owner 和服务运行角色。已有托管库时由数据库管理员在对应实例创建空库和角色，并按供应商要求配置连接证书和网络访问。

1. 使用 PostgreSQL 管理员进入 `psql`。创建角色，通过交互提示设置密码，再创建同名数据库。测试环境使用独立的库和角色名。

   ```text
   CREATE ROLE notes_auth LOGIN;
   \password notes_auth
   CREATE DATABASE notes_auth OWNER notes_auth;
   \q
   ```

2. 从目标提交的 `server/auth` 目录执行下列命令。先查询目标库与公开表，核对输出为目标空库，再执行第二条命令。脚本直接创建首次发布所需的完整结构，脚本失败时事务回滚。

   ```bash
   psql -X -W -h 127.0.0.1 -U notes_auth -d notes_auth -v ON_ERROR_STOP=1 \
     -c 'SELECT current_database(), current_user;' \
     -c "SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;"

   psql -X -W -h 127.0.0.1 -U notes_auth -d notes_auth \
     -v ON_ERROR_STOP=1 --single-transaction -f sql/schema.sql
   ```

3. 再查询公开表，确认存在 `auth_users`、`auth_email_codes`、`auth_rate_limits`、`auth_sessions`、`auth_passkeys`、`auth_challenges` 和 `auth_user_avatars`。保存执行的提交号和目标库名，服务启动时只连接数据库。

数据库连接失败时，先核对服务角色能否使用与 Node 相同的主机、端口和库名登录。使用远程 PostgreSQL 时，按服务商提供的 TLS 参数和 CA 配置连接。参考 [首次发布建库脚本](../server/auth/sql/schema.sql) 与 [psql 参数](https://www.postgresql.org/docs/current/app-psql.html)。

## 3. 准备 SMTP 和服务环境变量

- [ ] 在邮件服务商后台验证发件域，配置服务商要求的 SPF、DKIM 和 DMARC。若服务商有沙箱或收件白名单，先加入验收邮箱。
- [ ] 准备可用于 SMTP 提交的用户名、密码和允许的发件地址。源站按服务商给出的端口连接。当前实现用 `smtp://` 强制 STARTTLS，默认端口 587。用 `smtps://` 建立隐式 TLS，默认端口 465。
- [ ] 为每个环境生成独立的验证码密钥。可在受控终端用 `openssl rand -hex 32` 生成，直接保存到凭据管理位置。密钥用于验证码摘要和限流桶，变更后旧验证码及现有桶的对应关系会失效。
- [ ] 把真实值交给服务环境。PostgreSQL 和 SMTP URI 的用户名或密码如包含 `@`、`:`、`/`、`#` 或 `%`，对相应字段做百分号编码。凭据只保存在服务器的凭据管理位置或下节的受限环境文件。

| 变量 | 生产填写方式 |
| --- | --- |
| `DATABASE_URL` | `postgresql://notes_auth:ENCODED_PASSWORD@127.0.0.1:5432/notes_auth` |
| `NOTES_AUTH_RP_ID` | `auth.notes.zhangls.me`，只填主机名 |
| `NOTES_AUTH_ORIGINS` | `https://auth.notes.zhangls.me,android:apk-key-hash:BASE64URL_SHA256`。多个分发签名 Origin 用逗号追加 |
| `NOTES_AUTH_CODE_KEY` | 至少 32 字节的随机秘密值 |
| `NOTES_AUTH_SMTP_URL` | 如 `smtp://ENCODED_USER:ENCODED_PASSWORD@smtp.example.com:587`，用邮件服务商实际值 |
| `NOTES_AUTH_SMTP_FROM` | SMTP 服务商允许的发件邮箱 |
| `NOTES_AUTH_HOST` | `127.0.0.1` |
| `NOTES_AUTH_PORT` | `8080` |
| `NOTES_AUTH_TRUST_LOOPBACK_PROXY` | `true`，与下文的同机 Nginx 配置配套使用 |

可先用以下命令验证 STARTTLS、证书链和主机名，再通过测试 App 验证实际发信。若用隐式 TLS，改用相应端口并省去 `-starttls smtp` 参数。参考 [Nodemailer SMTP 连接选项](https://nodemailer.com/extras/smtp-connection)。

```bash
openssl s_client -starttls smtp -connect smtp.example.com:587 \
  -servername smtp.example.com -verify_hostname smtp.example.com -verify_return_error </dev/null
```

## 4. 配置 Android 与 iOS Passkey 域名关联

### Android

- [ ] 用实际分发的 APK 核对包名和签名证书。当前正式包名是 `me.zhangls.notes`，以签名 APK 为准。不同正式渠道若使用不同签名，逐个登记实际证书。

  ```bash
  apksigner verify --print-certs /path/to/Notes-release.apk
  ```

- [ ] 将上述输出中的 `Signer #1 certificate SHA-256 digest` 填入下列脚本。脚本根据同一份摘要输出 Android Digital Asset Links 所用的冒号分隔指纹和认证服务所用的 Base64URL Origin。保留脚本输出的完整 `android:apk-key-hash:` 前缀。

  ```bash
  python3 - 'REPLACE_WITH_CERTIFICATE_SHA256_DIGEST' <<'PY'
  import base64
  import sys

  digest = bytes.fromhex(sys.argv[1].replace(":", ""))
  if len(digest) != 32:
      raise SystemExit("Expected a SHA-256 certificate digest")
  print("Fingerprint:", ":".join(f"{byte:02X}" for byte in digest))
  print("Origin: android:apk-key-hash:" + base64.urlsafe_b64encode(digest).decode().rstrip("="))
  PY
  ```

- [ ] 根据 [Android 模板](../server/auth/association/assetlinks.example.json) 生成 `/var/www/notes-auth/.well-known/assetlinks.json`，使用实际包名和冒号分隔指纹。认证模板提供 `delegate_permission/common.get_login_creds`，可按官方示例在同一条声明追加 `delegate_permission/common.handle_all_urls`。将脚本输出的 Origin 填入 `NOTES_AUTH_ORIGINS`。官网的 `/home` 和 `/email` 深链继续使用官网域名的 App Links 声明。 [Android Credential Manager 官方说明](https://developer.android.com/identity/credential-manager/prerequisites)
- [ ] 检查正式包的合并 Manifest 与签名实机结果。仓库当前 Android Manifest 只声明官网深链，未配置 `asset_statements`。如果按 Credential Manager 指南配置网站与应用的密码共享，还需补齐官方给出的应用端声明。 [Android 应用端关联配置](https://developer.android.com/identity/credential-manager/prerequisites#4-update-the-apps-manifest)

### iOS

- [ ] 在 Apple Developer 为实际 App ID 启用 Associated Domains，核对正式签名包 entitlement 含 `webcredentials:auth.notes.zhangls.me`。用签名 App 的完整 `application-identifier` 填写关联文件，通常形如 `TEAM_ID.BUNDLE_ID`。
- [ ] 根据 [iOS 模板](../server/auth/association/apple-app-site-association.example.json) 生成 `/var/www/notes-auth/.well-known/apple-app-site-association`，把完整标识填入 `webcredentials.apps`。
- [ ] 认证域名的两个文件使用有效证书、直接返回 HTTP 200、响应类型为 JSON。验证阶段观察 Apple 的关联文件缓存生效时间，再用实际签名设备测试 Passkey。官网域名上的深链关联文件由 Pages 独立生成。参考 [Apple Associated Domains](https://developer.apple.com/documentation/xcode/supporting-associated-domains) 与 [官网域名关联配置](notes-web-deployment.md#站点预览与绑定)。

## 5. 启动 systemd 服务

1. 建立专用系统服务账号 `notes-auth`，授予它对发布目录的读取和遍历权限。将 `/opt/notes/current` 指向本次已验证的发布目录。
2. 建立由 root 持有的 `/etc/notes-auth` 目录，权限设为 `0700`。将上面的环境变量填入 `/etc/notes-auth/notes-auth.env`，采用每行一个 `KEY=value` 的格式，并将文件权限设为 `0600`。生产可另加 `NODE_ENV=production`。
3. 写入 `/etc/systemd/system/notes-auth.service`。按源站实际安装路径替换 `ExecStart` 中的 Node.js 路径，确保该路径可由服务账号执行。

```ini
[Unit]
Description=Notes authentication service
Wants=network-online.target
After=network-online.target

[Service]
Type=simple
User=notes-auth
Group=notes-auth
WorkingDirectory=/opt/notes/current/server/auth
EnvironmentFile=/etc/notes-auth/notes-auth.env
ExecStart=/usr/local/bin/node src/main.ts
Restart=on-failure
RestartSec=3s

[Install]
WantedBy=multi-user.target
```

4. 校验、启动并检查回环监听。启用可信回环代理模式后，直接访问 Node.js 的测试请求也需给出 `X-Notes-Client-IP`。以下请求预期返回 **401**，说明路由可达且需要会话。数据库和 SMTP 连通性由后续注册验收确认。

```bash
sudo systemd-analyze verify /etc/systemd/system/notes-auth.service
sudo systemctl daemon-reload
sudo systemctl enable --now notes-auth
sudo systemctl status notes-auth --no-pager
sudo journalctl -u notes-auth -n 50 --no-pager
curl -i -H 'X-Notes-Client-IP: 127.0.0.1' http://127.0.0.1:8080/v1/auth/me
```

替换服务环境变量或发布目录后，重启服务并复查日志。修改 unit 文件后先运行 `systemctl daemon-reload`，再重启服务。参考 [systemd 服务执行环境](https://www.freedesktop.org/software/systemd/man/latest/systemd.exec.html)。

## 6. 配置 Nginx HTTPS

先使用已有证书管理流程为认证域名签发并验证证书。使用 HTTP-01 时，先让 80 端口的临时站点或既有 ACME 入口能响应验证路径，再启用下面引用证书文件的 443 配置。示例采用 Let’s Encrypt 常见路径。按源站情况调整证书路径、续期方式和 Nginx include 位置。

```nginx
# 位于 http 上下文，同名 zone 只声明一次。
limit_req_zone $binary_remote_addr zone=notes_auth_ip:10m rate=5r/s;

server {
    listen 80;
    server_name auth.notes.zhangls.me;

    location ^~ /.well-known/acme-challenge/ {
        root /var/www/acme;
    }

    location / {
        return 301 https://auth.notes.zhangls.me$request_uri;
    }
}

server {
    listen 443 ssl;
    server_name auth.notes.zhangls.me;

    ssl_certificate /etc/letsencrypt/live/auth.notes.zhangls.me/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/auth.notes.zhangls.me/privkey.pem;

    location = /.well-known/assetlinks.json {
        root /var/www/notes-auth;
        default_type application/json;
        try_files $uri =404;
    }

    location = /.well-known/apple-app-site-association {
        root /var/www/notes-auth;
        default_type application/json;
        try_files $uri =404;
    }

    location /v1/auth/ {
        client_max_body_size 256k;
        limit_req zone=notes_auth_ip burst=15 nodelay;
        limit_req_status 429;
        proxy_set_header Host $host;
        proxy_set_header X-Notes-Client-IP $remote_addr;
        proxy_cache off;
        proxy_pass http://127.0.0.1:8080;
    }

    location / {
        return 404;
    }
}
```

- [ ] 核对 Nginx 可以读取证书和关联文件。保留 `proxy_pass` 示例中端口后无路径的写法，以转发原始 `/v1/auth/…` 路径。代理会覆盖客户端发送的 `X-Notes-Client-IP`，服务端按该值限流。
- [ ] 校验配置后 reload。从源站之外检查 HTTPS、证书和接口。关联文件预期返回 200 和 `application/json`，无需跟随重定向。匿名资料请求预期为 401，认证响应包含 `Cache-Control: no-store`。确认续期任务和续期后的 Nginx reload 已配置。参考 [Nginx 反向代理](https://nginx.org/en/docs/http/ngx_http_proxy_module.html) 与 [请求限流](https://nginx.org/en/docs/http/ngx_http_limit_req_module.html)。

```bash
sudo nginx -t
sudo systemctl reload nginx
curl -i https://auth.notes.zhangls.me/.well-known/assetlinks.json
curl -i https://auth.notes.zhangls.me/.well-known/apple-app-site-association
curl -i https://auth.notes.zhangls.me/v1/auth/me
```

## 7. 上线前验收

- [ ] **默认构建**：Android 和 iOS 的 Debug、Release 与各渠道默认使用 `https://auth.notes.zhangls.me`，提供注册并启用 Passkey。使用实际签名核对域名关联，覆盖开发包和分发包。见 [共享构建配置](../composeApp/build.gradle.kts) 与 [iOS 默认 entitlement](../iosApp/iosApp/iosApp.entitlements)。
- [ ] **测试服务构建**：显式设置 `NOTES_AUTH_BASE_URL=https://auth-staging.zhangls.me`。仅验证密码登录时设置 `NOTES_AUTH_PASSKEYS_ENABLED=false`。在 iOS 验证测试域名的 Passkey 时使用 `CODE_SIGN_ENTITLEMENTS=PasskeyTesting.entitlements` 与对应的 `NOTES_PASSKEY_RP_ID`。测试服务端 Origin 和关联文件登记测试包的实际签名。
- [ ] **真实邮箱注册**：用测试邮箱从独立注册页收信并完成两步注册。验证错误、过期和重复使用的验证码。使用已注册邮箱请求发码时，确认页面提示并阻止继续验证。用已验证邮箱重新登录，并确认用户名无法登录。
- [ ] **资料编辑**：核对个人信息页显示已验证邮箱。更换邮箱时先试错密码，再用正确密码收取新邮箱验证码。换绑后验证旧邮箱无法登录，新邮箱可使用密码和原有 Passkey 登录。上传头像后用另一台设备登录，确认资料同步。断网和上传失败时核对本地旧头像。
- [ ] **Passkey 与会话**：Android、iOS 分别用正式签名等效包完成密码登录、Passkey 注册与登录、刷新及退出。管理 Passkey 时需在最近五分钟内完成登录验证。测试退出后旧凭据访问受保护接口的结果。
- [ ] **部署记录**：记录实际提交、环境、验证的设备及签名、数据库测试结果、真实发信和关联文件结果。只记录测试结果或请求 ID。完成认证服务验收后，再进行官网与应用版本发布。 [认证接口契约](../server/auth/openapi.yaml) · [官网及版本发布顺序](notes-web-deployment.md)

## 8. 运行维护与排查

| 表现 | 检查 |
| --- | --- |
| Nginx 返回 502 | 看 `systemctl status notes-auth`、进程监听端口、Node.js 24 的路径，以及环境变量是否缺失。 |
| API 返回 `invalid_client_ip` | 检查可信回环代理模式和 Nginx 的请求头覆盖设置。若在本机直连 Node 测试，给出有效的 `X-Notes-Client-IP`。 |
| 资料、注册或发码返回 500 | 按 `X-Request-Id` 查日志，核对数据库地址、角色与七张表是否存在。匿名 `/me` 的 401 只能验证 HTTP 与认证入口。 |
| 注册提示已提交但收不到邮件，或返回 `email_unavailable` | 检查 SMTP 投递记录、垃圾邮件、沙箱白名单、发送域验证、凭据 URI 编码和出站连通性。已注册邮箱的发码请求返回 `409 email_already_registered`。 |
| 密码可登录，Passkey 不可用 | 对照构建渠道、RP ID、客户端 Origin、APK 签名指纹、iOS entitlement 和两个关联文件。 |
| 添加 Passkey 的验证请求返回 401 | 按响应的 `X-Request-Id` 在 `notes-auth` 日志查找 `auth_passkey_registration_failed`。该事件仅记录请求 ID 和固定失败类别。 |
| 更换头像返回 413 或 415 | 检查请求大小限制与 `Content-Type: image/jpeg`。服务限制 256 KiB，且 JPEG 每边最多 512 像素。 |
| 返回 429 | 区分 Nginx 的入口限流和应用的账号、邮箱或 IP 限流。等待相应时间窗口再用测试账号重试。 |

- [ ] 定期用 `pg_dump -Fc` 将数据库备份到受限、加密的位置。保留单独的凭据备份。定期把备份恢复到授权的隔离空库，用登录、头像和资料读取验证备份可用。恢复时由备份恢复表结构与数据。 [PostgreSQL pg_dump](https://www.postgresql.org/docs/current/app-pgdump.html) · [PostgreSQL pg_restore](https://www.postgresql.org/docs/current/app-pgrestore.html)
- [ ] 发布前保留上一版应用目录、配置和可用备份。服务回退时先核对旧版本与现有数据库结构兼容，再按已授权的部署流程切换 `/opt/notes/current` 并重启验证。保持生产 Passkey RP ID 稳定。
- [ ] 日志保留请求 ID、状态和必要的投递诊断。密码、验证码、会话 token、SMTP 凭据和完整数据库 URI 只经各自受保护的配置渠道流转。
