import { randomInt } from "node:crypto";
import type { Pool, PoolClient } from "pg";
import { rateLimitDigest, RequestError, token, tokenDigest } from "./security.ts";

export type SendEmail = (email: string, subject: string, text: string) => Promise<void>;

export function createEmailCodes(database: Pool, key: string, sendEmail: SendEmail) {
  const digest = (scope: string, email: string, code: string) =>
    rateLimitDigest(key, "email-code:" + scope, email + "\0" + code);

  return {
    async send(scope: string, email: string) {
      await database.query("DELETE FROM auth_email_codes WHERE expires_at <= now()");
      const code = randomInt(1_000_000).toString().padStart(6, "0");
      const hashed = digest(scope, email, code);
      const result = await database.query(
        `INSERT INTO auth_email_codes(scope, email, digest, expires_at, attempts_left)
         VALUES ($1, $2, $3, now() + interval '10 minutes', 5)
         ON CONFLICT (scope, email) DO UPDATE SET
           digest = EXCLUDED.digest, proof_digest = NULL, sent_at = now(),
           expires_at = EXCLUDED.expires_at, attempts_left = EXCLUDED.attempts_left
         WHERE auth_email_codes.sent_at <= now() - interval '1 minute'`,
        [scope, email, hashed],
      );
      if (!result.rowCount) return;
      try {
        await sendEmail(email, "Notes verification code / 验证码",
          `Your Notes verification code is ${code}. It expires in 10 minutes.\n\n你的 Notes 验证码是 ${code}，10 分钟内有效。`);
      } catch {
        await database.query("DELETE FROM auth_email_codes WHERE scope = $1 AND email = $2 AND digest = $3",
          [scope, email, hashed]);
        throw new RequestError(503, "email_unavailable");
      }
    },

    async verify(scope: string, email: string, code: string): Promise<string> {
      const proof = token();
      const { rows: [row] } = await database.query<{ verified: boolean }>(
        `UPDATE auth_email_codes SET
           digest = CASE WHEN digest = $3 THEN NULL ELSE digest END,
           proof_digest = CASE WHEN digest = $3 THEN $4 ELSE proof_digest END,
           expires_at = CASE WHEN digest = $3 THEN now() + interval '10 minutes' ELSE expires_at END,
           attempts_left = attempts_left - 1
         WHERE scope = $1 AND email = $2 AND digest IS NOT NULL AND attempts_left > 0 AND expires_at > now()
         RETURNING proof_digest = $4 AS verified`,
        [scope, email, digest(scope, email, code), tokenDigest(proof)],
      );
      if (!row?.verified) throw new RequestError(400, "invalid_email_code");
      return proof;
    },

    async consume(db: PoolClient, scope: string, email: string, proof: string) {
      const result = await db.query(
        `DELETE FROM auth_email_codes
         WHERE scope = $1 AND email = $2 AND proof_digest = $3 AND expires_at > now()`,
        [scope, email, tokenDigest(proof)],
      );
      if (!result.rowCount) throw new RequestError(400, "invalid_email_proof");
    },
  };
}
