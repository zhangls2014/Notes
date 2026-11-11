import assert from "node:assert/strict";
import test from "node:test";
import { once } from "node:events";
import { createAuthHttpServer } from "../src/http.ts";
import type { createAuthService } from "../src/service.ts";
import { RequestError } from "../src/security.ts";

test("auth HTTP boundary validates input and never returns secrets on errors", async (t) => {
  const accounts: string[] = [];
  const errors: string[] = [];
  const expired = "A".repeat(43);
  const successfulSession = { user: { id: "user", account: "notes_tester" }, accessToken: "access", refreshToken: "refresh" };
  const unsupported = () => Promise.reject(new Error("unexpected request"));
  const service: ReturnType<typeof createAuthService> = {
    async signInWithPassword(account: string, password: string) {
      accounts.push(account);
      if (password !== "Example passphrase 42") throw new RequestError(401, "invalid_credentials");
      return successfulSession;
    },
    registrationOptions: unsupported,
    registerPasskey: unsupported,
    authenticationOptions: unsupported,
    authenticatePasskey: unsupported,
    listPasskeys: unsupported,
    deletePasskey: unsupported,
    async refreshSession() { throw new RequestError(401, "invalid_session"); },
    logout: unsupported,
  };
  const server = createAuthHttpServer(service, (requestId) => errors.push(requestId));
  server.listen(0, "127.0.0.1");
  await once(server, "listening");
  t.after(() => new Promise<void>((resolve) => server.close(() => resolve())));
  const address = server.address();
  assert(address && typeof address === "object");
  const base = "http://127.0.0.1:" + address.port + "/v1/auth";
  const post = (path: string, body: object) => fetch(base + path, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body),
  });

  assert.equal((await post("/password/login", { account: "invalid account", password: "Example passphrase 42" })).status, 400);
  assert.equal((await post("/password/login", { account: "user@example.com", password: "" })).status, 400);
  const wrong = await post("/password/login", { account: "user@example.com", password: "wrong" });
  assert.equal(wrong.status, 401);
  assert.deepEqual(await wrong.json(), { error: "invalid_credentials" });
  assert.deepEqual(await (await post("/password/login", {
    account: " Notes_Tester ", password: "Example passphrase 42",
  })).json(), successfulSession);
  assert.deepEqual(accounts, ["user@example.com", "notes_tester"]);
  assert.equal((await post("/email/verify", { email: "user@example.com", code: "012345" })).status, 404);
  assert.equal((await fetch(base + "/passkeys")).status, 401);
  const rejected = await post("/session/refresh", { refreshToken: expired });
  assert.equal(rejected.status, 401);
  assert.deepEqual(await rejected.json(), { error: "invalid_session" });
  const failure = await post("/session/logout", { refreshToken: expired });
  assert.equal(failure.status, 500);
  assert.deepEqual(await failure.json(), { error: "internal_error" });
  assert.deepEqual(errors, [failure.headers.get("X-Request-Id")]);
});
