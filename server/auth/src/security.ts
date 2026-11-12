import { createHash, createHmac, randomBytes, scrypt, timingSafeEqual } from "node:crypto";

export class RequestError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string) {
    super(code);
    this.status = status;
    this.code = code;
  }
}

const accountName = /^[a-z0-9][a-z0-9._-]{2,63}$/;
const legacyEmail = /^[a-z0-9.!#$%&'*+/=?^_`{|}~-]{1,64}@[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+$/i;

export function normalizeAccount(input: unknown): string {
  const account = typeof input === "string" ? input.trim().toLowerCase() : "";
  if (account.length > 254 || !(accountName.test(account) || legacyEmail.test(account))) {
    throw new RequestError(400, "invalid_account");
  }
  return account;
}

export function normalizeUsername(input: unknown): string {
  const account = typeof input === "string" ? input.trim().toLowerCase() : "";
  if (!accountName.test(account)) throw new RequestError(400, "invalid_account");
  return account;
}

export function normalizeEmail(input: unknown): string {
  const email = typeof input === "string" ? input.trim() : "";
  const at = email.lastIndexOf("@");
  if (email.length > 254 || !legacyEmail.test(email) || email.slice(0, at).split(".").some(part => !part)) {
    throw new RequestError(400, "invalid_email");
  }
  return email.slice(0, at + 1) + email.slice(at + 1).toLowerCase();
}

export function readNewPassword(input: unknown): string {
  const password = readPassword(input);
  if ([...password].length < 15) throw new RequestError(400, "invalid_password");
  return password;
}

export function token(): string {
  return randomBytes(32).toString("base64url");
}

export function tokenDigest(value: string): Buffer {
  return createHash("sha256").update(value).digest();
}

export function rateLimitDigest(secret: string, kind: string, key: string): Buffer {
  return createHmac("sha256", secret).update(kind).update("\0").update(key).digest();
}

export function constantTimeEqual(a: Buffer, b: Buffer): boolean {
  return a.length === b.length && timingSafeEqual(a, b);
}

const saltBytes = 16;
const digestBytes = 32;
const dummyHash = randomBytes(saltBytes + digestBytes);

export function readPassword(input: unknown): string {
  if (typeof input !== "string" || !input || Buffer.byteLength(input) > 1024) {
    throw new RequestError(400, "invalid_password");
  }
  return input;
}

function derivePassword(password: string, salt: Buffer): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    scrypt(password, salt, digestBytes, { N: 2 ** 17, r: 8, p: 1, maxmem: 160 * 1024 ** 2 }, (error, digest) => {
      if (error) reject(error);
      else resolve(digest);
    });
  });
}

export async function hashPassword(password: string): Promise<Buffer> {
  const salt = randomBytes(saltBytes);
  return Buffer.concat([salt, await derivePassword(password, salt)]);
}

export async function verifyPassword(password: string, hash: Buffer | null): Promise<boolean> {
  const stored = hash?.length === dummyHash.length ? hash : dummyHash;
  const digest = await derivePassword(password, stored.subarray(0, saltBytes));
  return constantTimeEqual(digest, stored.subarray(saltBytes)) && stored !== dummyHash;
}
