import { randomUUID } from "node:crypto";
import pg from "pg";
import { hashPassword, normalizeEmail, normalizeUsername, readNewPassword } from "./security.ts";

const connectionString = process.env.DATABASE_URL;
if (!connectionString) throw new Error("DATABASE_URL is required");
const account = normalizeUsername(process.argv[2]);
const email = normalizeEmail(process.argv[3]);
if (process.stdin.isTTY) throw new Error("Supply the password through standard input");

let input = "";
process.stdin.setEncoding("utf8");
for await (const chunk of process.stdin) {
  input += chunk;
  if (Buffer.byteLength(input) > 1026) throw new Error("Password is too long");
}
const password = readNewPassword(input.replace(/\r?\n$/, ""));
const database = new pg.Pool({ connectionString });
try {
  const result = await database.query(
    `INSERT INTO auth_users (id, account, email, password_hash) VALUES ($1, $2, $3, $4)
     ON CONFLICT DO NOTHING`,
    [randomUUID(), account, email, await hashPassword(password)],
  );
  if (result.rowCount !== 1) throw new Error("This username or email is already in use");
  console.log("Account is ready for password sign-in");
} finally {
  await database.end();
}
