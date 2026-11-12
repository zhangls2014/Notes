import nodemailer from "nodemailer";
import pg from "pg";
import { createAuthHttpServer } from "./http.ts";
import { createAuthService } from "./service.ts";

function required(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) throw new Error("Missing required setting " + name);
  return value;
}

const rpId = required("NOTES_AUTH_RP_ID");
if (!/^[a-z0-9.-]+$/.test(rpId)) throw new Error("Invalid NOTES_AUTH_RP_ID");
const origins = required("NOTES_AUTH_ORIGINS").split(",").map((origin) => origin.trim());
if (origins.some((origin) => !/^android:apk-key-hash:[A-Za-z0-9_-]{43}$/.test(origin) &&
  origin !== "https://" + rpId)) throw new Error("Invalid NOTES_AUTH_ORIGINS");
const codeKey = required("NOTES_AUTH_CODE_KEY");
if (Buffer.byteLength(codeKey) < 32) throw new Error("NOTES_AUTH_CODE_KEY must contain at least 32 bytes");
const database = new pg.Pool({ connectionString: required("DATABASE_URL") });
database.on("error", () => console.error("auth_database_connection_failed"));
const smtpUrl = URL.parse(required("NOTES_AUTH_SMTP_URL"));
if (!smtpUrl?.hostname || !smtpUrl.username || !smtpUrl.password ||
  (smtpUrl.protocol !== "smtp:" && smtpUrl.protocol !== "smtps:")) {
  throw new Error("Invalid SMTP configuration");
}
const from = required("NOTES_AUTH_SMTP_FROM");
const secure = smtpUrl.protocol === "smtps:";
const transport = nodemailer.createTransport({
  host: smtpUrl.hostname,
  port: smtpUrl.port ? Number(smtpUrl.port) : secure ? 465 : 587,
  secure,
  requireTLS: !secure,
  auth: { user: decodeURIComponent(smtpUrl.username), pass: decodeURIComponent(smtpUrl.password) },
  connectionTimeout: 5000,
  greetingTimeout: 5000,
  socketTimeout: 10000,
}, { disableFileAccess: true, disableUrlAccess: true });
const service = createAuthService({
  database, rpId, origins, codeKey,
  async sendEmail(email, subject, text) {
    await transport.sendMail({ from, to: { name: "", address: email }, subject, text });
  },
});
const host = process.env.NOTES_AUTH_HOST ?? "127.0.0.1";
const trustLoopbackProxy = process.env.NOTES_AUTH_TRUST_LOOPBACK_PROXY === "true";
if (trustLoopbackProxy && host !== "127.0.0.1") throw new Error("Trusted proxy requires a loopback listener");
const server = createAuthHttpServer(service, (id, registrationFailure) => {
  if (registrationFailure) console.error("auth_passkey_registration_failed", id, registrationFailure);
  else console.error("auth_request_failed", id);
}, { trustLoopbackProxy });
const port = Number(process.env.NOTES_AUTH_PORT ?? "8080");
server.listen(port, host);
for (const signal of ["SIGINT", "SIGTERM"] as const) {
  process.once(signal, () => {
    server.close(() => { void database.end(); });
  });
}
