ALTER TABLE auth_sessions
  ADD COLUMN refresh_recovery_digest bytea UNIQUE,
  ADD COLUMN refresh_recovery_payload bytea,
  ADD COLUMN refresh_recovery_expires_at timestamptz;
