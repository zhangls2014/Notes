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
const service = createAuthService({ database, rpId, origins, codeKey });
const server = createAuthHttpServer(service, (id) => console.error("auth_request_failed", id));
const port = Number(process.env.NOTES_AUTH_PORT ?? "8080");
server.listen(port, process.env.NOTES_AUTH_HOST ?? "127.0.0.1");
for (const signal of ["SIGINT", "SIGTERM"] as const) {
  process.once(signal, () => {
    server.close(() => { void database.end(); });
  });
}
