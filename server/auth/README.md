# Notes authentication service

Notes supports password sign-in and Passkeys for the same account. Users register with a verified email. PostgreSQL stores usernames, emails, password hashes, avatars, public keys, challenges, rate limits and session token digests. The [OpenAPI contract](openapi.yaml) documents the endpoints.

## Local testing

1. In this directory, use Node.js 24 and run `npm ci`, `npm run check` and `npm test`.
2. After obtaining authorization, point `NOTES_AUTH_TEST_DATABASE_URL` at an empty PostgreSQL database dedicated to testing. Initialize it with the same single schema file used for production.

```bash
psql "$NOTES_AUTH_TEST_DATABASE_URL" -v ON_ERROR_STOP=1 --single-transaction -f sql/schema.sql
npm test
```

The optional PostgreSQL test creates and cleans up its own isolated rows. It exercises password verification, Passkey enrollment and login, avatar storage and retrieval, replay rejection, revocation and session rotation.

To seed a test account manually, set `DATABASE_URL` to the authorized test database and provide a username and email. Use a username of 3–64 letters, digits, dots, underscores or hyphens, starting with a letter or digit.

```zsh
read -rs 'notes_test_password?Test password (at least 15 characters): '
printf '%s' "$notes_test_password" | npm run provision-account -- notes_tester notes_tester@example.com
unset notes_test_password
```

Run `npm start` with the process variables below. Keep the local service bound to loopback and give the emulator or phone an HTTPS address it can reach and trust. Build the app with `-Pnotes.authBaseUrl=https://<auth-host>` or set `NOTES_AUTH_BASE_URL` in the build environment. An unset endpoint leaves the offline demo flow available.

## Service and Passkey configuration

| Variable | Purpose |
| --- | --- |
| `DATABASE_URL` | PostgreSQL connection URL |
| `NOTES_AUTH_RP_ID` | HTTPS relying party hostname without scheme or port |
| `NOTES_AUTH_ORIGINS` | Comma-separated allowlist of the HTTPS origin and Android signing origins in `android:apk-key-hash:<base64url-SHA256>` form |
| `NOTES_AUTH_CODE_KEY` | Random secret of at least 32 bytes for verification-code digests and rate-limit identifiers |
| `NOTES_AUTH_SMTP_URL` | Authenticated SMTP submission URL. Use `smtps://` for implicit TLS or `smtp://` for required STARTTLS |
| `NOTES_AUTH_SMTP_FROM` | Sender address approved by the SMTP provider |
| `NOTES_AUTH_HOST`, `NOTES_AUTH_PORT` | Listen address and port, defaulting to 127.0.0.1:8080 |

For device Passkey testing, configure the reverse proxy to forward the API and serve the [Android](association/assetlinks.example.json) and [Apple](association/apple-app-site-association.example.json) association files under `/.well-known/`. Fill them with the actual test identifiers and signing values. For iOS, pass `CODE_SIGN_ENTITLEMENTS=PasskeyTesting.entitlements NOTES_PASSKEY_RP_ID=<test-domain>` to Xcode. After configuring both the domain and the signed app, build with `-Pnotes.authPasskeysEnabled=true` or set `NOTES_AUTH_PASSKEYS_ENABLED=true` in the build environment. This enables the login entry on supported devices. Keep the flag off for a local password-only test.

The service limits password attempts by account and socket IP. For production reverse proxy configuration, see [Production origin](#production-origin). Passkey challenges expire in five minutes and are consumed on first use. Enrollment and removal require a session verified in the previous five minutes. Access tokens last 15 minutes and refresh tokens support one-time rotation for up to 30 days. The service does not apply schema changes at startup.

## Account profile

The current account is derived from the bearer session. `GET /v1/auth/me` returns
account ID, username, verified email and the nullable avatar revision. `GET /v1/auth/me/avatar`
downloads the account's JPEG. `PUT /v1/auth/me/avatar` accepts up to 256 KiB and
512 pixels per side, verifies and re-encodes JPEG input, and returns the stored JPEG
and its revision as an ETag. Avatar uploads are limited to ten attempts per account
per ten minutes. Avatar responses use `Cache-Control: no-store`.

Registration uses a six-digit code and then a single-use proof. Both expire
after ten minutes. Code attempts are limited to five and the resend interval is
one minute. Changing email requires the current password before sending a code
to the new address. Codes for email changes are bound to the authenticated session.
Email comparison preserves the local part and lowercases the domain.

Use `npm ci` with optional dependencies enabled so sharp loads the binary for
the server platform.

## Production origin

For a step-by-step production runbook in Chinese, see [认证服务部署与配置清单](../../docs/notes-auth-deployment.md).

Serve the existing API on `https://auth.notes.zhangls.me`. Set
`NOTES_AUTH_RP_ID=auth.notes.zhangls.me` and include
`https://auth.notes.zhangls.me` plus the actual APK signing origin in
`NOTES_AUTH_ORIGINS`. The Android signing origin is
`android:apk-key-hash:` followed by the base64url-encoded SHA-256 digest of the
signing certificate. Keep the database URL and code key in the service environment.
Use a separate RP ID, database and signing allowlist for staging. Initialize an approved empty
database with `sql/schema.sql` and configure SMTP before starting the service.

Use Nginx on the same host with a trusted certificate for the auth hostname.
Bind the Node.js service to `127.0.0.1:8080`, set
`NOTES_AUTH_TRUST_LOOPBACK_PROXY=true`, and configure the HTTPS virtual host
to overwrite the internal IP header. This setup assumes DNS points to the
HTTPS reverse proxy and the proxy sees the client's socket IP.

```nginx
# In the http context
limit_req_zone $binary_remote_addr zone=notes_auth_ip:10m rate=5r/s;

# In the auth HTTPS server block
location /v1/auth/ {
    client_max_body_size 256k;
    limit_req zone=notes_auth_ip burst=15 nodelay;
    limit_req_status 429;
    proxy_set_header X-Notes-Client-IP $remote_addr;
    proxy_pass http://127.0.0.1:8080;
}
```

Serve the two production credential association files under `/.well-known/`
on the auth domain, directly as JSON over HTTPS. Generate them from the
[Android](association/assetlinks.example.json) and
[Apple](association/apple-app-site-association.example.json) examples with the
actual distribution identities. The website domain publishes its separate
navigation association files through the [Pages build](../../docs/notes-web-deployment.md).
