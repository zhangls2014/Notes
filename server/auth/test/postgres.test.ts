import assert from "node:assert/strict";
import { generateKeyPairSync, createHash, randomBytes, randomUUID, sign } from "node:crypto";
import test from "node:test";
import pg from "pg";
import sharp from "sharp";
import { isoCBOR } from "@simplewebauthn/server/helpers";
import { createAuthService } from "../src/service.ts";
import { hashPassword, rateLimitDigest } from "../src/security.ts";

const databaseUrl = process.env.NOTES_AUTH_TEST_DATABASE_URL;
const rpId = "notes-test.example.com";
const origin = "https://" + rpId;
const sha256 = (input: Buffer | string) => createHash("sha256").update(input).digest();
const base64url = (input: Buffer | Uint8Array) => Buffer.from(input).toString("base64url");

test("PostgreSQL password and Passkey sign-in share an account and enforce expiry, replay and revocation",
  { skip: !databaseUrl && "Requires an authorized test database initialized with schema.sql" }, async (t) => {
    const database = new pg.Pool({ connectionString: databaseUrl });
    const testId = randomUUID();
    const account = "notes-" + testId;
    const email = account + "@example.com";
    const password = randomBytes(32).toString("base64url");
    const codeKey = randomBytes(32).toString("hex");
    const issuedChallenges: string[] = [];
    const userId = randomUUID();
    t.after(async () => {
      try {
        await database.query("DELETE FROM auth_challenges WHERE id = ANY($1::uuid[])", [issuedChallenges]);
        for (const [kind, key] of [
          ["password-ip", testId], ["password-account", account], ["registration", userId], ["authentication", testId], ["avatar-account", userId],
        ] as const) {
          if (!key) continue;
          const bucket = kind + ":" + rateLimitDigest(codeKey, kind, key).toString("base64url");
          await database.query("DELETE FROM auth_rate_limits WHERE bucket = $1", [bucket]);
        }
        await database.query("DELETE FROM auth_passkeys WHERE user_id IN (SELECT id FROM auth_users WHERE account = $1)", [account]);
        await database.query("DELETE FROM auth_sessions WHERE user_id IN (SELECT id FROM auth_users WHERE account = $1)", [account]);
        await database.query("DELETE FROM auth_users WHERE account = $1", [account]);
      } finally {
        await database.end();
      }
    });
    const service = createAuthService({
      database, rpId, origins: [origin], codeKey, async sendEmail() { assert.fail("Unexpected email"); },
    });
    await database.query("INSERT INTO auth_users (id, account, email, password_hash) VALUES ($1, $2, $3, $4)",
      [userId, account, email, await hashPassword(password)]);
    await assert.rejects(service.signInWithPassword(account, "wrong-password", testId),
      { message: "invalid_credentials" });
    const session = await service.signInWithPassword(account, password, testId);
    assert.deepEqual(session.user, { id: userId, account });
    assert.deepEqual(await service.getProfile(session.accessToken), { id: userId, account, email, avatarRevision: null });
    await assert.rejects(service.getAvatar(session.accessToken), { message: "avatar_not_found" });
    await assert.rejects(service.getProfile("X".repeat(43)), { message: "invalid_session" });
    const image = await sharp({ create: { width: 32, height: 32, channels: 3, background: "red" } }).jpeg().toBuffer();
    const avatar = await service.updateAvatar(session.accessToken, image);
    assert.deepEqual(await service.getAvatar(session.accessToken), avatar);
    assert.deepEqual(await service.getProfile(session.accessToken), { id: userId, account, email, avatarRevision: avatar.revision });


    const { publicKey, privateKey } = generateKeyPairSync("ec", { namedCurve: "prime256v1" });
    const jwk = publicKey.export({ format: "jwk" });
    const credentialId = randomBytes(32);
    const id = base64url(credentialId);
    const key = isoCBOR.encode(new Map<number, number | Uint8Array>([
      [1, 2], [3, -7], [-1, 1], [-2, Buffer.from(jwk.x!, "base64url")], [-3, Buffer.from(jwk.y!, "base64url")],
    ]));
    const registration = await service.registrationOptions(session.accessToken);
    issuedChallenges.push(registration.requestId);
    const attestation = isoCBOR.encode(new Map<string, string | Uint8Array | Map<string, string>>([
      ["fmt", "none"],
      ["attStmt", new Map()],
      ["authData", Buffer.concat([sha256(rpId), Buffer.from([0x45]), Buffer.alloc(4), Buffer.alloc(16),
        Buffer.from([0, credentialId.length]), credentialId, key])],
    ]));
    await service.registerPasskey(session.accessToken, registration.requestId, {
      id, rawId: id, type: "public-key", clientExtensionResults: {},
      response: {
        clientDataJSON: base64url(Buffer.from(JSON.stringify({
          type: "webauthn.create", origin, challenge: registration.publicKey.challenge,
        }))),
        attestationObject: base64url(attestation),
      },
    });
    assert.equal((await service.listPasskeys(session.accessToken)).passkeys.length, 1);

    function assertion(challenge: string, assertionOrigin: string, validSignature = true) {
      const clientData = Buffer.from(JSON.stringify({ type: "webauthn.get", challenge, origin: assertionOrigin }));
      const authData = Buffer.concat([sha256(rpId), Buffer.from([0x05]), Buffer.alloc(4)]);
      return {
        id, rawId: id, type: "public-key" as const, clientExtensionResults: {},
        response: {
          clientDataJSON: base64url(clientData),
          authenticatorData: base64url(authData),
          signature: base64url(validSignature ? sign("sha256", Buffer.concat([authData, sha256(clientData)]), privateKey) :
            randomBytes(64)),
          userHandle: registration.publicKey.user.id,
        },
      };
    }
    async function authenticationOptions() {
      const options = await service.authenticationOptions(testId);
      issuedChallenges.push(options.requestId);
      return options;
    }

    const incorrectOrigin = await authenticationOptions();
    await assert.rejects(service.authenticatePasskey(incorrectOrigin.requestId,
      assertion(incorrectOrigin.publicKey.challenge, "https://wrong.example.com")), { message: "invalid_passkey" });
    const incorrectSignature = await authenticationOptions();
    await assert.rejects(service.authenticatePasskey(incorrectSignature.requestId,
      assertion(incorrectSignature.publicKey.challenge, origin, false)), { message: "invalid_passkey" });

    const auth = await authenticationOptions();
    const response = assertion(auth.publicKey.challenge, origin);
    const verified = await service.authenticatePasskey(auth.requestId, response);
    assert.equal(verified.user.id, session.user.id);
    assert.equal((await service.getProfile(verified.accessToken)).avatarRevision, avatar.revision);
    assert.deepEqual(await service.getAvatar(verified.accessToken), avatar);
    await assert.rejects(service.authenticatePasskey(auth.requestId, response), { message: "invalid_challenge" });

    const renewed = await service.refreshSession(verified.refreshToken);
    await assert.rejects(service.refreshSession(verified.refreshToken), { message: "invalid_session" });
    await service.logout(renewed.refreshToken);
    await assert.rejects(service.refreshSession(renewed.refreshToken), { message: "invalid_session" });

    await service.deletePasskey(session.accessToken, id);
    const revoked = await authenticationOptions();
    await assert.rejects(service.authenticatePasskey(revoked.requestId,
      assertion(revoked.publicKey.challenge, origin)), { message: "invalid_passkey" });

    const expired = await authenticationOptions();
    await database.query("UPDATE auth_challenges SET expires_at = now() WHERE id = $1", [expired.requestId]);
    await assert.rejects(service.authenticatePasskey(expired.requestId,
      assertion(expired.publicKey.challenge, origin)), { message: "invalid_challenge" });
  });

test("PostgreSQL consumes signup proof once and verifies email changes for the current session",
  { skip: !databaseUrl && "Requires an authorized test database initialized with schema.sql" }, async (t) => {
    const database = new pg.Pool({ connectionString: databaseUrl });
    const testId = randomUUID();
    const email = testId + "@example.com";
    const newEmail = "new-" + email;
    const username = "notes-" + testId;
    const password = randomBytes(32).toString("base64url");
    const codeKey = randomBytes(32).toString("hex");
    let userId: string | undefined;
    const delivered = new Map<string, string>();
    const service = createAuthService({
      database, rpId, origins: [origin], codeKey,
      async sendEmail(recipient, _subject, text) {
        delivered.set(recipient, text.match(/\b[0-9]{6}\b/)![0]);
      },
    });
    t.after(async () => {
      try {
        await database.query("DELETE FROM auth_email_codes WHERE email = ANY($1::text[])", [[email, newEmail]]);
        if (userId) {
          await database.query("DELETE FROM auth_sessions WHERE user_id = $1", [userId]);
          await database.query("DELETE FROM auth_users WHERE id = $1", [userId]);
        }
        for (const [kind, key] of [
          ["signup-ip", testId], ["signup-email", email], ["signup-verify-ip", testId],
          ["signup-finish-ip", testId], ["rename-account", userId],
          ["email-change-account", userId], ["email-change-email", newEmail], ["email-change-verify", userId],
        ] as const) {
          if (key) await database.query("DELETE FROM auth_rate_limits WHERE bucket = $1",
            [kind + ":" + rateLimitDigest(codeKey, kind, key).toString("base64url")]);
        }
      } finally {
        await database.end();
      }
    });
    await service.sendSignupCode(email, testId);
    const signupCode = delivered.get(email)!;
    const incorrect = signupCode === "000000" ? "000001" : "000000";
    await assert.rejects(service.verifySignupCode(email, incorrect, testId), { message: "invalid_email_code" });
    const { proof } = await service.verifySignupCode(email, signupCode, testId);
    await assert.rejects(service.verifySignupCode(email, signupCode, testId), { message: "invalid_email_code" });
    const results = await Promise.allSettled([
      service.registerAccount(email, proof, username, password, testId),
      service.registerAccount(email, proof, username + "-2", password, testId),
    ]);
    const registered = results.filter(result => result.status === "fulfilled");
    assert.equal(registered.length, 1);
    const { user, accessToken } = registered[0].value;
    userId = user.id;
    assert.equal((await service.getProfile(accessToken)).email, email);
    await service.updateUsername(accessToken, username + "-renamed");
    assert.equal((await service.getProfile(accessToken)).account, username + "-renamed");
    await assert.rejects(service.sendEmailChangeCode(accessToken, newEmail, "wrong"),
      { message: "invalid_credentials" });
    await service.sendEmailChangeCode(accessToken, newEmail, password);
    const changeCode = delivered.get(newEmail)!;
    assert.equal((await service.getProfile(accessToken)).email, email);
    await service.changeEmail(accessToken, newEmail, changeCode);
    assert.equal((await service.getProfile(accessToken)).email, newEmail);
    await assert.rejects(service.changeEmail(accessToken, newEmail, changeCode), { message: "invalid_email_code" });
  });
