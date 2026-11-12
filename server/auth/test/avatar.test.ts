import assert from "node:assert/strict";
import test from "node:test";
import sharp from "sharp";
import { maxAvatarBytes, normalizeAvatar } from "../src/avatar.ts";

const image = (width: number, height: number) =>
  sharp({ create: { width, height, channels: 3, background: "#6578a1" } });

test("avatar decoder rejects malformed files, false JPEGs and oversized images", async () => {
  await assert.rejects(normalizeAvatar(Buffer.from([0xff, 0xd8, 0xff, 0xd9])), { message: "invalid_avatar" });
  await assert.rejects(normalizeAvatar(await image(48, 48).png().toBuffer()), { message: "invalid_avatar" });
  await assert.rejects(normalizeAvatar(await image(513, 32).jpeg().toBuffer()), { message: "invalid_avatar" });
  await assert.rejects(normalizeAvatar(Buffer.alloc(maxAvatarBytes + 1)), { message: "request_too_large" });
});

test("avatar output is JPEG with normalized orientation and no EXIF metadata", async () => {
  const input = await image(64, 48).withMetadata({ orientation: 6 }).jpeg().toBuffer();
  const output = await normalizeAvatar(input);
  const { width, height, format, exif } = await sharp(output).metadata();
  assert.deepEqual({ width, height, format, exif }, { width: 48, height: 64, format: "jpeg", exif: undefined });
});
