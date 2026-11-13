import assert from "node:assert/strict";
import test from "node:test";
import { expandAndroidPasskeyOrigins, hashPassword, verifyPassword, constantTimeEqual, rateLimitDigest, normalizeUsername, tokenDigest } from "../src/security.ts";

test("only configured Android signing digests get a standard Base64 Origin alias", () => {
  const web = "https://auth.example.com";
  const prefix = "android:apk-key-hash:";
  const android = prefix + "YdTv2mOXLm-jNnm_hcnmdrvDU1h4eaNDtwRgIdiV0xM";
  const xiaomi = prefix + "YdTv2mOXLm+jNnm/hcnmdrvDU1h4eaNDtwRgIdiV0xM";
  const sameEncoding = prefix + "A".repeat(43);
  assert.deepEqual(expandAndroidPasskeyOrigins([web, android, sameEncoding]), [web, android, xiaomi, sameEncoding]);
});

test("rate-limit identifiers are bound to the bucket and server key", () => {
  const key = "key";
  const first = rateLimitDigest(key, "one@example.com", "012345");
  assert(constantTimeEqual(first, rateLimitDigest(key, "one@example.com", "012345")));
  assert(!constantTimeEqual(first, rateLimitDigest(key, "two@example.com", "012345")));
  assert(!constantTimeEqual(first, rateLimitDigest("another", "one@example.com", "012345")));
});

test("usernames are normalized and constrained", () => {
  assert.equal(normalizeUsername(" Notes_Tester "), "notes_tester");
  assert.throws(() => normalizeUsername("ab"));
  assert.throws(() => normalizeUsername("invalid account"));
  assert.throws(() => normalizeUsername("a".repeat(65)));
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

test("new passwords need 8 code points and no emoji", async () => {
  const { readNewPassword } = await import("../src/security.ts");
  assert.equal(readNewPassword("abcdefg1"), "abcdefg1");
  assert.throws(() => readNewPassword("abcdef1"), /invalid_password/);
  // 表情由多个码点拼成，逗号区段与组件字符都要拦住
  ["abcdefg1😀", "👍🏽abcdefg1", "👨‍👩‍👧abcdefg1", "❤️abcdefg1", "1️⃣abcdefg1", "⭐abcdefg1"]
    .forEach((password) => assert.throws(() => readNewPassword(password), /invalid_password/, password));
  // U+20000 不属于表情符号，按码点而不是 UTF-16 单元计数
  const ideographs = "𠀀".repeat(7) + "1";
  assert.equal(readNewPassword(ideographs), ideographs);
});
