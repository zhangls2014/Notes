import { randomUUID } from "node:crypto";
import {
  generateAuthenticationOptions,
  generateRegistrationOptions,
  verifyAuthenticationResponse,
  verifyRegistrationResponse,
} from "@simplewebauthn/server";
import type {
  AuthenticationResponseJSON,
  AuthenticatorTransport,
  RegistrationResponseJSON,
} from "@simplewebauthn/server";
import type { Pool, PoolClient } from "pg";
import { normalizeAvatar } from "./avatar.ts";
import { createEmailCodes } from "./email-codes.ts";
import type { SendEmail } from "./email-codes.ts";
import { hashPassword, rateLimitDigest, RequestError, token, tokenDigest, verifyPassword } from "./security.ts";

type Session = { session_id: string; id: string; account: string; email: string; verified_at: Date };
type User = Pick<Session, "id" | "account">;
type Challenge = { challenge: string };
type Passkey = {
  id: string;
  user_id: string;
  account: string;
  public_key: Buffer;
  sign_count: string;
  transports: AuthenticatorTransport[] | null;
};

function registrationFailureReason(error: unknown) {
  if (!(error instanceof Error)) return "verification_error";
  const code = "code" in error ? error.code : undefined;
  if (code === "UNEXPECTED_ORIGIN") return "origin_mismatch";
  if (code === "UNEXPECTED_RP_ID") return "rp_id_mismatch";
  if (code === "BAD_ARGUMENT") return "invalid_verifier_configuration";
  if (error.message.startsWith("Unexpected registration response challenge ")) return "challenge_mismatch";
  if (error.message.startsWith("User presence was required")) return "user_presence_missing";
  if (error.message.startsWith("User verification was required")) return "user_verification_missing";
  if (code === "RUNTIME_NO_PQC_SUPPORT" || error.message.startsWith("Unexpected public key alg ")) {
    return "unsupported_public_key_algorithm";
  }
  if (error.message.startsWith("Unsupported Attestation Format:")) return "unsupported_attestation_format";
  return "verification_error";
}

export class PasskeyRegistrationError extends RequestError {
  readonly reason: ReturnType<typeof registrationFailureReason> | "attestation_rejected";

  constructor(reason: PasskeyRegistrationError["reason"]) {
    super(401, "invalid_passkey");
    this.reason = reason;
  }
}

export function createAuthService(config: {
  database: Pool;
  rpId: string;
  origins: string[];
  codeKey: string;
  sendEmail: SendEmail;
}) {
  const { database, rpId, origins, codeKey, sendEmail } = config;
  const emailCodes = createEmailCodes(database, codeKey, sendEmail);
  const signup = "signup";

  async function transaction<T>(operation: (db: PoolClient) => Promise<T>): Promise<T> {
    const db = await database.connect();
    try {
      await db.query("BEGIN");
      const result = await operation(db);
      await db.query("COMMIT");
      return result;
    } catch (error) {
      await db.query("ROLLBACK");
      throw error;
    } finally {
      db.release();
    }
  }

  async function withinLimit(kind: string, key: string, limit: number, minutes: number) {
    const bucket = kind + ":" + rateLimitDigest(codeKey, kind, key).toString("base64url");
    const result = await database.query(
      `INSERT INTO auth_rate_limits(bucket, hits, expires_at)
       VALUES ($1, 1, now() + $3 * interval '1 minute')
       ON CONFLICT (bucket) DO UPDATE SET
         hits = CASE WHEN auth_rate_limits.expires_at <= now() THEN 1 ELSE auth_rate_limits.hits + 1 END,
         expires_at = CASE WHEN auth_rate_limits.expires_at <= now() THEN EXCLUDED.expires_at
           ELSE auth_rate_limits.expires_at END
       WHERE auth_rate_limits.expires_at <= now() OR auth_rate_limits.hits < $2
       RETURNING bucket`,
      [bucket, limit, minutes],
    );
    return result.rowCount === 1;
  }

  async function issueSession(db: Pool | PoolClient, user: User) {
    const accessToken = token();
    const refreshToken = token();
    await db.query(
      `INSERT INTO auth_sessions(id, user_id, access_digest, refresh_digest, access_expires_at, refresh_expires_at)
       VALUES ($1, $2, $3, $4, now() + interval '15 minutes', now() + interval '30 days')`,
      [randomUUID(), user.id, tokenDigest(accessToken), tokenDigest(refreshToken)],
    );
    return { user: { id: user.id, account: user.account }, accessToken, refreshToken };
  }

  async function requireSession(accessToken: string): Promise<Session> {
    const { rows: [session] } = await database.query<Session>(
      `SELECT s.id AS session_id, u.id, u.account, u.email, s.verified_at
       FROM auth_sessions s JOIN auth_users u ON u.id = s.user_id
       WHERE s.access_digest = $1 AND s.access_expires_at > now()`,
      [tokenDigest(accessToken)],
    );
    if (!session) throw new RequestError(401, "invalid_session");
    return session;
  }

  function requireRecentVerification(session: Session) {
    if (Date.now() - session.verified_at.getTime() > 5 * 60 * 1000) {
      throw new RequestError(403, "recent_verification_required");
    }
  }

  async function saveChallenge(challenge: string, purpose: string, sessionId: string | null) {
    const requestId = randomUUID();
    await database.query(
      `INSERT INTO auth_challenges (id, challenge, purpose, session_id, expires_at)
       VALUES ($1, $2, $3, $4, now() + interval '5 minutes')`,
      [requestId, challenge, purpose, sessionId],
    );
    return requestId;
  }

  async function takeChallenge(id: string, purpose: string, sessionId: string | null) {
    const { rows: [row] } = await database.query<Challenge>(
      `DELETE FROM auth_challenges WHERE id = $1 AND purpose = $2
       AND session_id IS NOT DISTINCT FROM $3::uuid AND expires_at > now() RETURNING challenge`,
      [id, purpose, sessionId],
    );
    if (!row) throw new RequestError(401, "invalid_challenge");
    return row.challenge;
  }

  function emailChangeScope(session: Session) {
    return "email-change:" + session.session_id;
  }

  async function emailIsRegistered(email: string): Promise<boolean> {
    const result = await database.query("SELECT 1 FROM auth_users WHERE email = $1", [email]);
    return Boolean(result.rowCount);
  }

  return {
    async sendSignupCode(email: string, ip: string) {
      if (!await withinLimit("signup-ip", ip, 20, 15) || !await withinLimit("signup-email", email, 5, 15)) {
        throw new RequestError(429, "try_later");
      }
      if (!await emailIsRegistered(email)) await emailCodes.send(signup, email);
    },

    async verifySignupCode(email: string, code: string, ip: string) {
      if (!await withinLimit("signup-verify-ip", ip, 50, 15)) throw new RequestError(429, "try_later");
      return { proof: await emailCodes.verify(signup, email, code) };
    },

    async registerAccount(email: string, proof: string, account: string, password: string, ip: string) {
      if (!await withinLimit("signup-finish-ip", ip, 25, 15)) throw new RequestError(429, "try_later");
      return transaction(async db => {
        await emailCodes.consume(db, signup, email, proof);
        const result = await db.query<User>(
          `INSERT INTO auth_users (id, account, email, password_hash) VALUES ($1, $2, $3, $4)
           ON CONFLICT DO NOTHING RETURNING id, account`,
          [randomUUID(), account, email, await hashPassword(password)],
        );
        if (!result.rows[0]) throw new RequestError(409, "account_unavailable");
        return issueSession(db, result.rows[0]);
      });
    },

    async updateUsername(accessToken: string, account: string) {
      const session = await requireSession(accessToken);
      if (!await withinLimit("rename-account", session.id, 10, 15)) throw new RequestError(429, "try_later");
      try {
        const result = await database.query(
          `UPDATE auth_users SET account = $1 WHERE id = $2
           AND EXISTS (SELECT 1 FROM auth_sessions WHERE id = $3 AND access_digest = $4 AND access_expires_at > now())`,
          [account, session.id, session.session_id, tokenDigest(accessToken)],
        );
        if (!result.rowCount) throw new RequestError(401, "invalid_session");
      } catch (error) {
        if (error instanceof Error && "code" in error && error.code === "23505") {
          throw new RequestError(409, "account_unavailable");
        }
        throw error;
      }
    },

    async sendEmailChangeCode(accessToken: string, email: string, password: string) {
      const session = await requireSession(accessToken);
      if (email === session.email) throw new RequestError(400, "email_unchanged");
      if (!await withinLimit("email-change-account", session.id, 5, 15) ||
        !await withinLimit("email-change-email", email, 5, 15)) {
        throw new RequestError(429, "try_later");
      }
      const { rows: [user] } = await database.query<{ password_hash: Buffer }>(
        "SELECT password_hash FROM auth_users WHERE id = $1", [session.id],
      );
      if (!await verifyPassword(password, user?.password_hash ?? null)) {
        throw new RequestError(403, "invalid_credentials");
      }
      if (!await emailIsRegistered(email)) await emailCodes.send(emailChangeScope(session), email);
    },

    async changeEmail(accessToken: string, email: string, code: string) {
      const session = await requireSession(accessToken);
      if (!await withinLimit("email-change-verify", session.id, 25, 15)) throw new RequestError(429, "try_later");
      const scope = emailChangeScope(session);
      const proof = await emailCodes.verify(scope, email, code);
      try {
        await transaction(async db => {
          await emailCodes.consume(db, scope, email, proof);
          const result = await db.query(
            `UPDATE auth_users SET email = $1 WHERE id = $2 AND email = $3
             AND EXISTS (SELECT 1 FROM auth_sessions WHERE id = $4 AND access_digest = $5 AND access_expires_at > now())`,
            [email, session.id, session.email, session.session_id, tokenDigest(accessToken)],
          );
          if (!result.rowCount) throw new RequestError(409, "profile_changed");
        });
      } catch (error) {
        if (error instanceof Error && "code" in error && error.code === "23505") {
          throw new RequestError(409, "email_unavailable");
        }
        throw error;
      }
    },

    async signInWithPassword(account: string, password: string, ip: string) {
      if (!await withinLimit("password-ip", ip, 50, 15) ||
        !await withinLimit("password-account", account, 10, 15)) {
        throw new RequestError(429, "try_later");
      }
      const { rows: [user] } = await database.query<User & { password_hash: Buffer | null }>(
        "SELECT id, account, password_hash FROM auth_users WHERE account = $1", [account],
      );
      const verified = await verifyPassword(password, user?.password_hash ?? null);
      if (!user || !verified) throw new RequestError(401, "invalid_credentials");
      return issueSession(database, user);
    },

    async registrationOptions(accessToken: string) {
      const session = await requireSession(accessToken);
      requireRecentVerification(session);
      if (!await withinLimit("registration", session.id, 10, 5)) throw new RequestError(429, "try_later");
      const { rows } = await database.query<Passkey>("SELECT id, transports FROM auth_passkeys WHERE user_id = $1", [session.id]);
      const publicKey = await generateRegistrationOptions({
        rpName: "Notes",
        rpID: rpId,
        userID: Buffer.from(session.id, "utf8"),
        userName: session.account,
        attestationType: "none",
        excludeCredentials: rows.map(({ id, transports }) => ({ id, transports: transports ?? undefined })),
        authenticatorSelection: { residentKey: "required", userVerification: "required" },
        timeout: 120_000,
      });
      return { requestId: await saveChallenge(publicKey.challenge, "registration", session.session_id), publicKey };
    },

    async registerPasskey(accessToken: string, requestId: string, response: RegistrationResponseJSON) {
      const session = await requireSession(accessToken);
      requireRecentVerification(session);
      const challenge = await takeChallenge(requestId, "registration", session.session_id);
      const result = await verifyRegistrationResponse({
        response,
        expectedChallenge: challenge,
        expectedRPID: rpId,
        expectedOrigin: origins,
        requireUserVerification: true,
      }).catch((error: unknown) => {
        throw new PasskeyRegistrationError(registrationFailureReason(error));
      });
      if (!result.verified || !result.registrationInfo) throw new PasskeyRegistrationError("attestation_rejected");
      const credential = result.registrationInfo.credential;
      const inserted = await database.query(
        `INSERT INTO auth_passkeys (id, user_id, public_key, sign_count, transports)
         SELECT $1, s.user_id, $2, $3, $4 FROM auth_sessions s WHERE s.id = $5 AND s.access_digest = $6
         AND s.access_expires_at > now() ON CONFLICT (id) DO NOTHING`,
        [credential.id, Buffer.from(credential.publicKey), credential.counter, credential.transports ?? null,
          session.session_id, tokenDigest(accessToken)],
      );
      if (inserted.rowCount !== 1) throw new RequestError(409, "registration_conflict");
    },

    async authenticationOptions(ip: string) {
      if (!await withinLimit("authentication", ip, 50, 5)) throw new RequestError(429, "try_later");
      const publicKey = await generateAuthenticationOptions({
        rpID: rpId, userVerification: "required", timeout: 120_000,
      });
      return { requestId: await saveChallenge(publicKey.challenge, "authentication", null), publicKey };
    },

    async authenticatePasskey(requestId: string, response: AuthenticationResponseJSON) {
      const challenge = await takeChallenge(requestId, "authentication", null);
      return transaction(async (db) => {
        const { rows: [credential] } = await db.query<Passkey>(
          `SELECT p.*, u.account FROM auth_passkeys p JOIN auth_users u ON u.id = p.user_id
           WHERE p.id = $1 FOR UPDATE OF p`, [response.id],
        );
        const expectedHandle = credential && Buffer.from(credential.user_id, "utf8").toString("base64url");
        if (!credential || response.response?.userHandle !== expectedHandle) {
          throw new RequestError(401, "invalid_passkey");
        }
        const result = await verifyAuthenticationResponse({
          response,
          expectedChallenge: challenge,
          expectedRPID: rpId,
          expectedOrigin: origins,
          requireUserVerification: true,
          credential: {
            id: credential.id,
            publicKey: new Uint8Array(credential.public_key),
            counter: Number(credential.sign_count),
            transports: credential.transports ?? undefined,
          },
        }).catch(() => null);
        if (!result?.verified) throw new RequestError(401, "invalid_passkey");
        await db.query("UPDATE auth_passkeys SET sign_count = $1 WHERE id = $2",
          [result.authenticationInfo.newCounter, credential.id]);
        return issueSession(db, { id: credential.user_id, account: credential.account });
      });
    },

    async getProfile(accessToken: string) {
      const session = await requireSession(accessToken);
      const { rows: [avatar] } = await database.query<{ revision: string }>(
        "SELECT revision FROM auth_user_avatars WHERE user_id = $1", [session.id],
      );
      return { id: session.id, account: session.account, email: session.email, avatarRevision: avatar?.revision ?? null };
    },

    async getAvatar(accessToken: string) {
      const session = await requireSession(accessToken);
      const { rows: [avatar] } = await database.query<{ revision: string; jpeg: Buffer }>(
        "SELECT revision, jpeg FROM auth_user_avatars WHERE user_id = $1", [session.id],
      );
      if (!avatar) throw new RequestError(404, "avatar_not_found");
      return avatar;
    },

    async updateAvatar(accessToken: string, upload: Buffer) {
      const session = await requireSession(accessToken);
      if (!await withinLimit("avatar-account", session.id, 10, 10)) {
        throw new RequestError(429, "rate_limited");
      }
      const jpeg = await normalizeAvatar(upload);
      const revision = randomUUID();
      const result = await database.query(
        `INSERT INTO auth_user_avatars (user_id, revision, jpeg)
         SELECT user_id, $1, $2 FROM auth_sessions
         WHERE id = $3 AND access_digest = $4 AND access_expires_at > now()
         ON CONFLICT (user_id) DO UPDATE SET revision = EXCLUDED.revision, jpeg = EXCLUDED.jpeg`,
        [revision, jpeg, session.session_id, tokenDigest(accessToken)],
      );
      if (!result.rowCount) throw new RequestError(401, "invalid_session");
      return { revision, jpeg };
    },

    async listPasskeys(accessToken: string) {
      const session = await requireSession(accessToken);
      const { rows } = await database.query<{ id: string; createdAt: Date }>(
        'SELECT id, created_at AS "createdAt" FROM auth_passkeys WHERE user_id = $1 ORDER BY created_at, id',
        [session.id],
      );
      return { passkeys: rows };
    },

    async deletePasskey(accessToken: string, id: string) {
      const session = await requireSession(accessToken);
      requireRecentVerification(session);
      await database.query(
        `DELETE FROM auth_passkeys p USING auth_sessions s
         WHERE p.id = $1 AND p.user_id = s.user_id AND s.id = $2
           AND s.access_digest = $3 AND s.access_expires_at > now()`,
        [id, session.session_id, tokenDigest(accessToken)],
      );
    },

    async refreshSession(oldToken: string) {
      const accessToken = token();
      const refreshToken = token();
      const { rows: [user] } = await database.query<User>(
        `UPDATE auth_sessions s SET access_digest = $2, refresh_digest = $3,
           access_expires_at = now() + interval '15 minutes'
         FROM auth_users u WHERE s.user_id = u.id AND s.refresh_digest = $1
           AND s.refresh_expires_at > now() RETURNING u.id, u.account`,
        [tokenDigest(oldToken), tokenDigest(accessToken), tokenDigest(refreshToken)],
      );
      if (!user) throw new RequestError(401, "invalid_session");
      return { user, accessToken, refreshToken };
    },

    async logout(refreshToken: string) {
      await database.query("DELETE FROM auth_sessions WHERE refresh_digest = $1", [tokenDigest(refreshToken)]);
    },
  };
}
