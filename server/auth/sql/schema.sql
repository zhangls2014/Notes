CREATE TABLE auth_users (
  id uuid PRIMARY KEY,
  account text NOT NULL UNIQUE CHECK (account ~ '^[a-z0-9][a-z0-9._-]{2,63}$'),
  email text NOT NULL UNIQUE,
  password_hash bytea NOT NULL
);

CREATE TABLE auth_email_codes (
  scope text NOT NULL,
  email text NOT NULL,
  digest bytea,
  proof_digest bytea,
  sent_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NOT NULL,
  attempts_left smallint NOT NULL CHECK (attempts_left >= 0),
  PRIMARY KEY (scope, email)
);
CREATE INDEX auth_email_codes_expiry ON auth_email_codes(expires_at);

CREATE TABLE auth_rate_limits (
  bucket text PRIMARY KEY,
  hits integer NOT NULL,
  expires_at timestamptz NOT NULL
);

CREATE TABLE auth_sessions (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES auth_users(id),
  access_digest bytea NOT NULL UNIQUE,
  refresh_digest bytea NOT NULL UNIQUE,
  access_expires_at timestamptz NOT NULL,
  refresh_expires_at timestamptz NOT NULL,
  refresh_recovery_digest bytea UNIQUE,
  refresh_recovery_payload bytea,
  refresh_recovery_expires_at timestamptz,
  verified_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE auth_passkeys (
  id text PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES auth_users(id),
  public_key bytea NOT NULL,
  sign_count bigint NOT NULL,
  transports text[],
  aaguid uuid,
  device_brand varchar(64),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX auth_passkeys_user ON auth_passkeys(user_id);

CREATE TABLE auth_challenges (
  id uuid PRIMARY KEY,
  challenge text NOT NULL,
  purpose text NOT NULL CHECK (purpose IN ('registration', 'authentication')),
  session_id uuid REFERENCES auth_sessions(id) ON DELETE CASCADE,
  expires_at timestamptz NOT NULL
);

CREATE TABLE auth_user_avatars (
  user_id uuid PRIMARY KEY REFERENCES auth_users(id) ON DELETE CASCADE,
  revision uuid NOT NULL,
  jpeg bytea NOT NULL CHECK (octet_length(jpeg) BETWEEN 1 AND 262144)
);
