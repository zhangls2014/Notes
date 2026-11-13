import assert from "node:assert/strict";
import test from "node:test";
import { once } from "node:events";
import type { Pool } from "pg";
import { createAuthHttpServer } from "../src/http.ts";
import { createAuthService } from "../src/service.ts";
import { RequestError } from "../src/security.ts";

test("auth HTTP boundary validates input and never returns secrets on errors", async (t) => {
  const accounts: string[] = [];
  const clientIps: string[] = [];
  const errors: string[] = [];
  const registrationFailures: [string, string][] = [];
  const expired = "A".repeat(43);
  const successfulSession = { user: { id: "user", account: "notes_tester" }, accessToken: "access", refreshToken: "refresh" };
  const revision = "0af7af37-ab71-408a-be8b-a43b23f74034";
  const profile = { id: "user", account: "notes_tester", email: "notes@example.com", avatarRevision: revision };
  const jpeg = Buffer.from([0xff, 0xd8, 0xff, 0xd9]);
  const unsupported = () => Promise.reject(new Error("unexpected request"));
  const registrationService = createAuthService({
    database: {
      async query(sql: string) {
        if (sql.includes("FROM auth_sessions")) {
          return { rows: [{ id: "user", session_id: revision, verified_at: new Date() }] };
        }
        assert.match(sql, /DELETE FROM auth_challenges/);
        return { rows: [{ challenge: "expected-challenge" }] };
      },
    } as unknown as Pool,
    rpId: "auth.example.com",
    origins: ["https://auth.example.com"],
    codeKey: "test-code-key",
    sendEmail: unsupported,
  });
  const service: ReturnType<typeof createAuthService> = {
    async signInWithPassword(account: string, password: string, ip: string) {
      accounts.push(account);
      clientIps.push(ip);
      if (password !== "Example passphrase 42") throw new RequestError(401, "invalid_credentials");
      return successfulSession;
    },
    async getProfile(access) {
      assert.equal(access, expired);
      return profile;
    },
    async getAvatar(access) {
      assert.equal(access, expired);
      return { revision, jpeg };
    },
    async updateAvatar(access, uploaded) {
      assert.equal(access, expired);
      assert.deepEqual(uploaded, jpeg);
      return { revision, jpeg };
    },
    async sendSignupCode(email) {
      if (email === "registered@example.com") throw new RequestError(409, "email_already_registered");
      assert.equal(email, "notes@example.com");
    },
    async verifySignupCode(email, code) {
      assert.equal(email, "notes@example.com");
      assert.equal(code, "123456");
      return { proof: expired };
    },
    async registerAccount(email, proof, account) {
      assert.equal(email, "notes@example.com");
      assert.equal(proof, expired);
      assert.equal(account, "notes_tester");
      return successfulSession;
    },
    async updateUsername(access, account) {
      assert.equal(access, expired);
      assert.equal(account, "new_name");
    },
    async sendEmailChangeCode(access, email, password) {
      assert.equal(access, expired);
      assert.equal(email, "new@example.com");
      assert.equal(password, "current password");
    },
    async changeEmail(access, email, code) {
      assert.equal(access, expired);
      assert.equal(email, "new@example.com");
      assert.equal(code, "123456");
    },
    registrationOptions: unsupported,
    registerPasskey: registrationService.registerPasskey,
    async authenticationOptions(email) {
      assert.equal(email, "notes@example.com");
      throw new RequestError(401, "invalid_passkey");
    },
    authenticatePasskey: unsupported,
    listPasskeys: unsupported,
    deletePasskey: unsupported,
    async refreshSession() { throw new RequestError(401, "invalid_session"); },
    logout: unsupported,
  };
  const server = createAuthHttpServer(service, (requestId: string, reason?: string) => {
    if (reason) registrationFailures.push([requestId, reason]);
    else errors.push(requestId);
  }, { trustLoopbackProxy: true });
  server.listen(0, "127.0.0.1");
  await once(server, "listening");
  t.after(() => new Promise<void>((resolve) => server.close(() => resolve())));
  const address = server.address();
  assert(address && typeof address === "object");
  const base = "http://127.0.0.1:" + address.port + "/v1/auth";
  const post = (path: string, body: object, ip = "192.0.2.1") => fetch(base + path, {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-Notes-Client-IP": ip },
    body: JSON.stringify(body),
  });

  assert.equal((await post("/password/login", { account: "notes_tester", password: "Example passphrase 42" })).status, 400);
  assert.equal((await post("/password/login", { account: "user@example.com", password: "" })).status, 400);
  const wrong = await post("/password/login", { account: "user@example.com", password: "wrong" });
  assert.equal(wrong.status, 401);
  assert.deepEqual(await wrong.json(), { error: "invalid_credentials" });
  assert.deepEqual(await (await post("/password/login", {
    account: " Notes@EXAMPLE.COM ", password: "Example passphrase 42",
  }, "2001:db8::2")).json(), successfulSession);
  assert.deepEqual(accounts, ["user@example.com", "Notes@example.com"]);
  assert.deepEqual(clientIps, ["192.0.2.1", "2001:db8::2"]);
  assert.equal((await post("/password/login", {
    account: "notes@example.com", password: "wrong",
  }, "192.0.2.1, 192.0.2.2")).status, 400);
  assert.equal((await post("/email/verify", { email: "user@example.com", code: "012345" })).status, 404);
  assert.equal((await fetch(base + "/passkeys")).status, 400);
  assert.equal((await fetch(base + "/passkeys", { headers: { "X-Notes-Client-IP": "192.0.2.1" } })).status, 401);
  const headers = { "X-Notes-Client-IP": "192.0.2.1" };
  assert.equal((await fetch(base + "/me", { headers })).status, 401);
  assert.equal((await fetch(base + "/me/avatar", { method: "PUT", headers, body: jpeg })).status, 401);
  const authenticated = { ...headers, Authorization: "Bearer " + expired };
  assert.equal((await post("/signup/code", { email: "notes@EXAMPLE.COM" })).status, 204);
  assert.deepEqual(await (await post("/signup/verify", { email: "notes@example.com", code: "123456" })).json(),
    { proof: expired });
  const duplicate = await post("/signup/code", { email: "registered@EXAMPLE.COM" });
  assert.equal(duplicate.status, 409);
  assert.deepEqual(await duplicate.json(), { error: "email_already_registered" });
  assert.equal((await post("/passkeys/authentication/options", {})).status, 400);
  assert.equal((await post("/passkeys/authentication/options", { email: "notes@EXAMPLE.COM" })).status, 401);
  const signup = { email: "notes@example.com", proof: expired, account: "Notes_Tester", password: "long enough password" };
  assert.equal((await post("/signup", { ...signup, password: "short" })).status, 400);
  assert.equal((await post("/signup", signup)).status, 201);
  const edit = (method: string, path: string, body: object) => fetch(base + path, {
    method, headers: { ...authenticated, "Content-Type": "application/json" }, body: JSON.stringify(body),
  });
  const invalidBrand = await edit("POST", "/passkeys/registration/verify", {
    requestId: revision, credential: {}, deviceBrand: "x".repeat(65),
  });
  assert.equal(invalidBrand.status, 400);
  assert.deepEqual(await invalidBrand.json(), { error: "invalid_device_brand" });
  const badOrigin = await edit("POST", "/passkeys/registration/verify", {
    requestId: revision,
    credential: {
      id: "Y3JlZGVudGlhbA",
      rawId: "Y3JlZGVudGlhbA",
      type: "public-key",
      response: {
        clientDataJSON: Buffer.from(JSON.stringify({
          type: "webauthn.create", challenge: "expected-challenge", origin: "https://unexpected.example",
        })).toString("base64url"),
      },
    },
  });
  assert.equal(badOrigin.status, 401);
  assert.deepEqual(await badOrigin.json(), { error: "invalid_passkey" });
  assert.deepEqual(registrationFailures, [[badOrigin.headers.get("X-Request-Id"), "origin_mismatch"]]);
  assert.equal((await edit("PUT", "/me/username", { account: "New_Name" })).status, 204);
  assert.equal((await edit("POST", "/me/email/code", { email: "new@example.com", password: "current password" })).status, 204);
  assert.equal((await edit("PUT", "/me/email", { email: "new@example.com", code: "123456" })).status, 204);
  assert.deepEqual(await (await fetch(base + "/me", { headers: authenticated })).json(), profile);
  const avatar = await fetch(base + "/me/avatar", { headers: authenticated });
  assert.equal(avatar.headers.get("Content-Type"), "image/jpeg");
  assert.equal(avatar.headers.get("Cache-Control"), "no-store");
  assert.equal(avatar.headers.get("ETag"), '"' + revision + '"');
  assert.deepEqual(Buffer.from(await avatar.arrayBuffer()), jpeg);
  const upload = (body: Buffer, contentType = "image/jpeg") => fetch(base + "/me/avatar", {
    method: "PUT", headers: { ...authenticated, "Content-Type": contentType }, body: Uint8Array.from(body),
  });
  assert.equal((await upload(jpeg, "image/png")).status, 415);
  assert.equal((await upload(Buffer.alloc(256 * 1024 + 1))).status, 413);
  assert.equal((await upload(jpeg)).status, 200);
  const rejected = await post("/session/refresh", { refreshToken: expired });
  assert.equal(rejected.status, 401);
  assert.deepEqual(await rejected.json(), { error: "invalid_session" });
  const failure = await post("/session/logout", { refreshToken: expired });
  assert.equal(failure.status, 500);
  assert.deepEqual(await failure.json(), { error: "internal_error" });
  assert.deepEqual(errors, [failure.headers.get("X-Request-Id")]);
});
