import { randomUUID } from "node:crypto";
import pg from "pg";
import { hashPassword, normalizeAccount, readPassword } from "./security.ts";

const connectionString = process.env.DATABASE_URL;
if (!connectionString) throw new Error("DATABASE_URL is required");
const account = normalizeAccount(process.argv[2]);
if (process.stdin.isTTY) throw new Error("Supply the password through standard input");

let input = "";
process.stdin.setEncoding("utf8");
for await (const chunk of process.stdin) {
  input += chunk;
  if (Buffer.byteLength(input) > 1026) throw new Error("Password is too long");
}
const password = readPassword(input.replace(/\r?\n$/, ""));
if ([...password].length < 15) throw new Error("Password must contain at least 15 characters");
const database = new pg.Pool({ connectionString });
try {
  const result = await database.query(
    `INSERT INTO auth_users (id, account, password_hash) VALUES ($1, $2, $3)
     ON CONFLICT (account) DO UPDATE SET password_hash = EXCLUDED.password_hash
     WHERE auth_users.password_hash IS NULL`,
    [randomUUID(), account, await hashPassword(password)],
  );
  if (result.rowCount !== 1) throw new Error("This account already has a password");
  console.log("Account is ready for password sign-in");
} finally {
  await database.end();
}
