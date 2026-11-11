CREATE TABLE auth_users (
  id uuid PRIMARY KEY,
  email text NOT NULL UNIQUE
);

CREATE TABLE auth_email_codes (
  email text PRIMARY KEY,
  digest bytea NOT NULL,
  expires_at timestamptz NOT NULL,
  attempts_left smallint NOT NULL
);

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
  verified_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE auth_passkeys (
  id text PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES auth_users(id),
  public_key bytea NOT NULL,
  sign_count bigint NOT NULL,
  transports text[],
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
