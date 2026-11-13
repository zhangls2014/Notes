BEGIN;

ALTER TABLE auth_passkeys
  ADD COLUMN IF NOT EXISTS aaguid uuid,
  ADD COLUMN IF NOT EXISTS device_brand varchar(64);

COMMIT;
