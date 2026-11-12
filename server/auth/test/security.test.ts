import assert from "node:assert/strict";
import test from "node:test";
import { hashPassword, verifyPassword, constantTimeEqual, rateLimitDigest, normalizeAccount, tokenDigest } from "../src/security.ts";

test("rate-limit identifiers are bound to the bucket and server key", () => {
  const key = "key";
  const first = rateLimitDigest(key, "one@example.com", "012345");
  assert(constantTimeEqual(first, rateLimitDigest(key, "one@example.com", "012345")));
  assert(!constantTimeEqual(first, rateLimitDigest(key, "two@example.com", "012345")));
  assert(!constantTimeEqual(first, rateLimitDigest("another", "one@example.com", "012345")));
});

test("account names are normalized and legacy email-style accounts still work", () => {
  assert.equal(normalizeAccount(" USER+notes@Example.com "), "user+notes@example.com");
  assert.throws(() => normalizeAccount("user@example.com\r\nBcc:other@example.com"));
  assert.equal(normalizeAccount(" Notes_Tester "), "notes_tester");
  assert.throws(() => normalizeAccount("ab"));
  assert.throws(() => normalizeAccount("invalid account"));
  assert.throws(() => normalizeAccount("a".repeat(65)));
});

test("opaque token digests are fixed size and can be compared safely", () => {
  assert.equal(tokenDigest("example").length, 32);
  assert(!constantTimeEqual(tokenDigest("example"), Buffer.alloc(1)));
  assert(!constantTimeEqual(tokenDigest("example"), tokenDigest("different")));
});

test("password hashes are salted and require an exact password match", async () => {
  const password = "Leading and trailing spaces stay exact ";
  const hash = await hashPassword(password);
  assert.notDeepEqual(hash, await hashPassword(password));
  assert.equal(await verifyPassword(password, hash), true);
  assert.equal(await verifyPassword(password.trim(), hash), false);
  assert.equal(await verifyPassword(password, null), false);
});

test("registration validates email and username as separate identifiers", async () => {
  const { normalizeEmail, normalizeUsername } = await import("../src/security.ts");
  assert.equal(normalizeEmail(" Alex+notes@EXAMPLE.COM "), "Alex+notes@example.com");
  assert.equal(normalizeUsername(" Demo_User "), "demo_user");
  assert.throws(() => normalizeEmail("alex@example.com, other@example.com"), /invalid_email/);
  assert.throws(() => normalizeUsername("alex@example.com"), /invalid_account/);
});
