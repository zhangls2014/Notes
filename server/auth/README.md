# Notes authentication service

Notes supports password sign-in and Passkeys for the same account. PostgreSQL stores account IDs, password hashes, public keys, challenges, rate limits and session token digests. The [OpenAPI contract](openapi.yaml) documents the endpoints.

## Local testing

1. In this directory, use Node.js 24 and run `npm ci`, `npm run check` and `npm test`.
2. After obtaining authorization, point `NOTES_AUTH_TEST_DATABASE_URL` at a new PostgreSQL database dedicated to testing. Apply the three migrations in order to the new database. For an existing authorized database, apply only migrations that have not already run. The third migration preserves account UUIDs, passwords, and Passkeys.

```bash
psql "$NOTES_AUTH_TEST_DATABASE_URL" -v ON_ERROR_STOP=1 --single-transaction \
  -f sql/001-initial.sql -f sql/002-password-login.sql -f sql/003-account-name.sql
npm test
```

The optional PostgreSQL test creates and cleans up its own isolated rows. It exercises password verification, Passkey enrollment and login for the same account, replay rejection, revocation and session rotation.

To test sign-in manually, set `DATABASE_URL` to the authorized test database URL and provision a test account with the command below in zsh. Choose an account name of 3–64 characters, using letters, digits, dots, underscores or hyphens, starting with a letter or digit. Names are case-insensitive. Existing accounts can keep using their email-style names. The command sets a password for a new or passwordless account while preserving any existing account ID and Passkeys. The password is entered without terminal echo and passed on standard input.

```zsh
read -rs 'notes_test_password?Test password (at least 15 characters): '
printf '%s' "$notes_test_password" | npm run provision-account -- notes_tester
unset notes_test_password
```

Run `npm start` with the process variables below. Keep the local service bound to loopback and give the emulator or phone an HTTPS address it can reach and trust. Build the app with `-Pnotes.authBaseUrl=https://<auth-host>` or set `NOTES_AUTH_BASE_URL` in the build environment. An unset endpoint leaves the offline demo flow available.

## Service and Passkey configuration

| Variable | Purpose |
| --- | --- |
| `DATABASE_URL` | PostgreSQL connection URL |
| `NOTES_AUTH_RP_ID` | HTTPS relying party hostname without scheme or port |
| `NOTES_AUTH_ORIGINS` | Comma-separated allowlist of the HTTPS origin and Android signing origins in `android:apk-key-hash:<base64url-SHA256>` form |
| `NOTES_AUTH_CODE_KEY` | Random secret of at least 32 bytes for rate-limit bucket identifiers |
| `NOTES_AUTH_HOST`, `NOTES_AUTH_PORT` | Listen address and port, defaulting to 127.0.0.1:8080 |

For device Passkey testing, configure the reverse proxy to forward the API and serve the [Android](association/assetlinks.example.json) and [Apple](association/apple-app-site-association.example.json) association files under `/.well-known/`. Fill them with the actual test identifiers and signing values. For iOS, pass `CODE_SIGN_ENTITLEMENTS=PasskeyTesting.entitlements NOTES_PASSKEY_RP_ID=<test-domain>` to Xcode. After configuring both the domain and the signed app, build with `-Pnotes.authPasskeysEnabled=true` or set `NOTES_AUTH_PASSKEYS_ENABLED=true` in the build environment. This enables the login entry on supported devices. Keep the flag off for a local password-only test.

The service limits password attempts by account and socket IP. Behind a reverse proxy, enforce per-client IP limits at the proxy. Passkey challenges expire in five minutes and are consumed on first use. Enrollment and removal require a session verified in the previous five minutes. Access tokens last 15 minutes and refresh tokens support one-time rotation for up to 30 days. The service does not apply schema changes at startup.
