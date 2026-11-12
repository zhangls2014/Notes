import sharp from "sharp";
import { RequestError } from "./security.ts";

export const maxAvatarBytes = 256 * 1024;
const maxAvatarSide = 512;

export async function normalizeAvatar(upload: Buffer): Promise<Buffer> {
  if (upload.length > maxAvatarBytes) throw new RequestError(413, "request_too_large");
  if (upload[0] !== 0xff || upload[1] !== 0xd8 || upload[2] !== 0xff) {
    throw new RequestError(400, "invalid_avatar");
  }

  try {
    const image = sharp(upload, { limitInputPixels: maxAvatarSide * maxAvatarSide, failOn: "warning" });
    const metadata = await image.metadata();
    if (metadata.format !== "jpeg" || metadata.width > maxAvatarSide || metadata.height > maxAvatarSide) {
      throw new RequestError(400, "invalid_avatar");
    }
    const jpeg = await image.autoOrient().jpeg({ quality: 85 }).toBuffer();
    if (jpeg.length > maxAvatarBytes) throw new RequestError(413, "request_too_large");
    return jpeg;
  } catch (error) {
    if (error instanceof RequestError) throw error;
    throw new RequestError(400, "invalid_avatar");
  }
}
