BEGIN;
CREATE TABLE mailboxes (
  user_id uuid PRIMARY KEY REFERENCES auth_users(id),
  initialized_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE simulated_mail (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES mailboxes(user_id),
  sender_id integer NOT NULL CHECK (sender_id BETWEEN 1 AND 5),
  recipient_ids jsonb NOT NULL,
  subject text NOT NULL,
  body text NOT NULL,
  mailbox text NOT NULL DEFAULT 'INBOX' CHECK (mailbox IN ('INBOX', 'SENT', 'TRASH')),
  important boolean NOT NULL DEFAULT false,
  thread_id bigint,
  source_id bigint,
  mode text NOT NULL DEFAULT 'new' CHECK (mode IN ('new', 'reply', 'replyAll', 'forward')),
  operation_id uuid,
  request_payload jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (user_id, operation_id)
);
CREATE INDEX simulated_mail_user_order ON simulated_mail(user_id, created_at DESC, id DESC);
CREATE INDEX simulated_mail_thread ON simulated_mail(user_id, thread_id);
COMMIT;
