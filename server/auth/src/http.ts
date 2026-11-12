import { createServer } from "node:http";
import type { IncomingMessage, ServerResponse } from "node:http";
import { randomUUID } from "node:crypto";
import { isIP } from "node:net";
import type { AuthenticationResponseJSON, RegistrationResponseJSON } from "@simplewebauthn/server";
import {
  normalizeAccount, normalizeEmail, normalizeUsername, readNewPassword, readPassword, RequestError,
} from "./security.ts";
import { maxAvatarBytes } from "./avatar.ts";
import type { createAuthService } from "./service.ts";

type AuthService = ReturnType<typeof createAuthService>;
const api = "/v1/auth";
const maxBodyBytes = 64 * 1024;

async function body(request: IncomingMessage): Promise<Record<string, unknown>> {
  if (request.headers["content-type"]?.split(";")[0]?.trim() !== "application/json") {
    throw new RequestError(415, "json_required");
  }
  const bytes = await readBody(request, maxBodyBytes);
  let value: unknown;
  try {
    value = JSON.parse(bytes.toString("utf8"));
  } catch {
    throw new RequestError(400, "invalid_json");
  }
  return object(value);
}

async function readBody(request: IncomingMessage, limit: number): Promise<Buffer> {
  const chunks: Buffer[] = [];
  let size = 0;
  // Keep the socket open long enough to return the error on oversized payloads.
  for await (const chunk of request.iterator({ destroyOnReturn: false })) {
    size += chunk.length;
    if (size > limit) {
      request.resume();
      throw new RequestError(413, "request_too_large");
    }
    chunks.push(chunk);
  }
  return Buffer.concat(chunks);
}

function sendAvatar(response: ServerResponse, avatar: { revision: string; jpeg: Buffer }) {
  response.writeHead(200, {
    "Content-Type": "image/jpeg",
    "Content-Length": avatar.jpeg.length,
    "X-Content-Type-Options": "nosniff",
    ETag: '"' + avatar.revision + '"',
  });
  response.end(avatar.jpeg);
}

function object(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value)) throw new RequestError(400, "invalid_request");
  return value as Record<string, unknown>;
}

function requirePattern(value: unknown, pattern: RegExp, error: string) {
  if (typeof value !== "string" || !pattern.test(value)) throw new RequestError(400, error);
  return value;
}

function requestId(input: unknown) {
  return requirePattern(input, /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i, "invalid_request_id");
}

function emailCode(input: unknown) {
  return requirePattern(input, /^[0-9]{6}$/, "invalid_email_code");
}

function refreshToken(input: unknown) {
  return requirePattern(input, /^[A-Za-z0-9_-]{43}$/, "invalid_token");
}

function bearer(request: IncomingMessage) {
  const token = request.headers.authorization?.match(/^Bearer ([A-Za-z0-9_-]{43})$/i)?.[1];
  if (!token) throw new RequestError(401, "invalid_session");
  return token;
}

function send(response: ServerResponse, status: number, data?: unknown) {
  response.writeHead(status, data === undefined ? {} : { "Content-Type": "application/json" });
  response.end(data === undefined ? undefined : JSON.stringify(data));
}

function clientIp(request: IncomingMessage, trustLoopbackProxy: boolean): string {
  const peer = request.socket.remoteAddress ?? "unknown";
  if (!trustLoopbackProxy || peer !== "127.0.0.1") return peer;
  const forwarded = request.headers["x-notes-client-ip"];
  if (typeof forwarded !== "string" || !isIP(forwarded)) throw new RequestError(400, "invalid_client_ip");
  return forwarded;
}

export function createAuthHttpServer(
  service: AuthService,
  logError: (requestId: string) => void,
  { trustLoopbackProxy = false } = {},
) {
  return createServer(async (request, response) => {
    const id = randomUUID();
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("X-Request-Id", id);
    const path = request.url?.split("?")[0];

    try {
      const ip = clientIp(request, trustLoopbackProxy);
      switch (request.method + " " + path) {
        case "POST " + api + "/signup/code": {
          const input = await body(request);
          await service.sendSignupCode(normalizeEmail(input.email), ip);
          send(response, 204);
          return;
        }
        case "POST " + api + "/signup/verify": {
          const input = await body(request);
          send(response, 200, await service.verifySignupCode(normalizeEmail(input.email), emailCode(input.code), ip));
          return;
        }
        case "POST " + api + "/signup": {
          const input = await body(request);
          send(response, 201, await service.registerAccount(
            normalizeEmail(input.email), requirePattern(input.proof, /^[A-Za-z0-9_-]{43}$/, "invalid_email_proof"),
            normalizeUsername(input.account), readNewPassword(input.password), ip,
          ));
          return;
        }
        case "PUT " + api + "/me/username": {
          const access = bearer(request);
          const input = await body(request);
          await service.updateUsername(access, normalizeUsername(input.account));
          send(response, 204);
          return;
        }
        case "POST " + api + "/me/email/code": {
          const access = bearer(request);
          const input = await body(request);
          await service.sendEmailChangeCode(access, normalizeEmail(input.email), readPassword(input.password));
          send(response, 204);
          return;
        }
        case "PUT " + api + "/me/email": {
          const access = bearer(request);
          const input = await body(request);
          await service.changeEmail(access, normalizeEmail(input.email), emailCode(input.code));
          send(response, 204);
          return;
        }
        case "POST " + api + "/password/login": {
          const input = await body(request);
          send(response, 200, await service.signInWithPassword(
            normalizeAccount(input.account), readPassword(input.password), ip,
          ));
          return;
        }
        case "POST " + api + "/passkeys/registration/options":
          send(response, 200, await service.registrationOptions(bearer(request)));
          return;
        case "POST " + api + "/passkeys/registration/verify": {
          const access = bearer(request);
          const input = await body(request);
          await service.registerPasskey(access, requestId(input.requestId),
            object(input.credential) as unknown as RegistrationResponseJSON);
          send(response, 204);
          return;
        }
        case "POST " + api + "/passkeys/authentication/options":
          send(response, 200, await service.authenticationOptions(ip));
          return;
        case "POST " + api + "/passkeys/authentication/verify": {
          const input = await body(request);
          const credential = object(input.credential);
          requirePattern(credential.id, /^[A-Za-z0-9_-]{1,1400}$/, "invalid_credential_id");
          send(response, 200, await service.authenticatePasskey(requestId(input.requestId),
            credential as unknown as AuthenticationResponseJSON));
          return;
        }
        case "GET " + api + "/me":
          send(response, 200, await service.getProfile(bearer(request)));
          return;
        case "GET " + api + "/me/avatar":
          sendAvatar(response, await service.getAvatar(bearer(request)));
          return;
        case "PUT " + api + "/me/avatar": {
          const access = bearer(request);
          if (request.headers["content-type"] !== "image/jpeg") throw new RequestError(415, "jpeg_required");
          sendAvatar(response, await service.updateAvatar(access, await readBody(request, maxAvatarBytes)));
          return;
        }
        case "GET " + api + "/passkeys":
          send(response, 200, await service.listPasskeys(bearer(request)));
          return;
        case "POST " + api + "/session/refresh": {
          const input = await body(request);
          send(response, 200, await service.refreshSession(refreshToken(input.refreshToken)));
          return;
        }
        case "POST " + api + "/session/logout": {
          const input = await body(request);
          await service.logout(refreshToken(input.refreshToken));
          send(response, 204);
          return;
        }
        default: {
          const credentialId = path?.slice((api + "/passkeys/").length);
          if (request.method === "DELETE" && path?.startsWith(api + "/passkeys/") &&
            credentialId && /^[A-Za-z0-9_-]{1,1400}$/.test(credentialId)) {
            await service.deletePasskey(bearer(request), credentialId);
            send(response, 204);
            return;
          }
          throw new RequestError(404, "not_found");
        }
      }
    } catch (error) {
      if (error instanceof RequestError) {
        send(response, error.status, { error: error.code });
      } else {
        logError(id);
        send(response, 500, { error: "internal_error" });
      }
    }
  });
}
