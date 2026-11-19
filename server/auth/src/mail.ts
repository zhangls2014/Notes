import type { PoolClient } from "pg";
import { RequestError } from "./security.ts";

const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export function mailId(value: unknown): number {
  if (typeof value !== "number" || !Number.isSafeInteger(value) || value < 1) throw new RequestError(400, "invalid_mail_id");
  return value;
}
export function readMailSend(input: Record<string, unknown>) {
  const { operationId, recipientIds, subject, body, mode, sourceId } = input;
  if (typeof operationId !== "string" || !uuid.test(operationId) ||
    !Array.isArray(recipientIds) || recipientIds.length < 1 || recipientIds.length > 5 ||
    !recipientIds.every(id => Number.isInteger(id) && id >= 1 && id <= 5) ||
    typeof subject !== "string" || subject.length > 500 || typeof body !== "string" || !body.trim() || body.length > 20000 ||
    !["new", "reply", "replyAll", "forward"].includes(String(mode))) throw new RequestError(400, "invalid_mail");
  if (mode !== "new") mailId(sourceId);
  else if (sourceId !== undefined && sourceId !== null) throw new RequestError(400, "invalid_mail_source");
  return { operationId, recipientIds: [...new Set(recipientIds as number[])].sort(), subject, body, mode: String(mode), sourceId: mode === "new" ? null : mailId(sourceId) };
}
export function initialMail() {
  return Array.from({length: 16}, (_, index) => ({
    senderId: 2 + index % 4,
    subject: ["Weekend plans", "Project update", "Reading list", "Coffee next week"][index % 4] + " #" + (index + 1),
    body: ["Shall we take a walk this weekend?", "The latest draft is ready for review.", "Here are some ideas for our next reading session.", "Let me know when you are free for coffee."][index % 4]!,
  }));
}
function contacts(account: string) {
  return [account, "Alex", "Sam", "Taylor", "Jordan"].map((name, index) => ({
    id: index + 1, firstName: name, lastName: "", email: index === 0 ? "me@example.invalid" : name.toLowerCase() + "@example.invalid",
    altEmail: "", avatar: "avatar_" + index, isDefault: index === 0,
  }));
}
type Row = { id: string; sender_id: number; recipient_ids: number[]; subject: string; body: string; mailbox: string; important: boolean; thread_id: string | null; created_at: Date; request_payload: unknown };
function model(row: Row, people: ReturnType<typeof contacts>) {
  return { id: Number(row.id), sender: people[row.sender_id - 1], recipients: row.recipient_ids.map(id => people[id - 1]),
    subject: row.subject, body: row.body, mailbox: row.mailbox, isImportant: row.important, isStarred: false,
    createdAt: row.created_at.toISOString(), threads: [] };
}

/** Called inside the authenticated transaction. Every query is scoped to its owner. */
export async function mailOperation(db: PoolClient, userId: string, account: string, input: Record<string, unknown>) {
  // Serialize initialization and mutations for this user, including duplicate operation IDs.
  await db.query("SELECT id FROM auth_users WHERE id = $1 FOR UPDATE", [userId]);
  const initialized = await db.query("INSERT INTO mailboxes(user_id) VALUES ($1) ON CONFLICT DO NOTHING RETURNING user_id", [userId]);
  if (initialized.rowCount) {
    for (const [index, item] of initialMail().entries()) {
      const seeded = await db.query<{id: string}>(`INSERT INTO simulated_mail(user_id, sender_id, recipient_ids, subject, body, created_at)
        VALUES ($1,$2,'[1]',$3,$4,now() - $5 * interval '1 hour') RETURNING id`, [userId, item.senderId, item.subject, item.body, index + 1]);
      if (index === 0) {
        await db.query(`INSERT INTO simulated_mail(user_id,sender_id,recipient_ids,subject,body,mailbox,thread_id,mode)
          VALUES ($1,1,'[2]',$2,'Sounds good. Let us meet in the morning.','SENT',$3,'reply')`,
          [userId, "Re: " + item.subject, seeded.rows[0]!.id]);
      }
    }
  }
  const people = contacts(account);
  if (input.action === "contacts") return people;
  if (input.action === "send") {
    const draft = readMailSend(input);
    const payload = JSON.stringify(draft);
    const previous = await db.query<Row>("SELECT * FROM simulated_mail WHERE user_id=$1 AND operation_id=$2", [userId, draft.operationId]);
    if (previous.rows[0]) {
      // JSONB equality ignores key ordering.
      const same = await db.query("SELECT id FROM simulated_mail WHERE user_id=$1 AND operation_id=$2 AND request_payload=$3::jsonb", [userId, draft.operationId, payload]);
      if (!same.rowCount) throw new RequestError(409, "mail_operation_conflict");
      return model(previous.rows[0], people);
    }
    let threadId: string | null = null;
    if (draft.sourceId !== null) {
      const source = await db.query<Row>("SELECT * FROM simulated_mail WHERE user_id=$1 AND id=$2 AND mailbox <> 'TRASH'", [userId, draft.sourceId]);
      if (!source.rows[0]) throw new RequestError(404, "mail_not_found");
      if (draft.mode !== "forward") threadId = source.rows[0].thread_id ?? source.rows[0].id;
    }
    const result = await db.query<Row>(`INSERT INTO simulated_mail(user_id,sender_id,recipient_ids,subject,body,mailbox,thread_id,source_id,mode,operation_id,request_payload)
      VALUES ($1,1,$2,$3,$4,'SENT',$5,$6,$7,$8,$9) RETURNING *`,
      [userId, JSON.stringify(draft.recipientIds), draft.subject, draft.body, threadId, draft.sourceId, draft.mode, draft.operationId, payload]);
    return model(result.rows[0]!, people);
  }
  if (input.action === "detail") {
    const result = await db.query<Row>("SELECT * FROM simulated_mail WHERE user_id=$1 AND id=$2 AND mailbox <> 'TRASH'", [userId, mailId(input.id)]);
    if (!result.rows[0]) throw new RequestError(404, "mail_not_found");
    return model(result.rows[0], people);
  }
  if (input.action === "favorite" || input.action === "trash") {
    if (!Array.isArray(input.ids) || !input.ids.length || input.ids.length > 100) throw new RequestError(400, "invalid_mail_ids");
    const ids = input.ids.map(mailId);
    const selected = await db.query("SELECT id FROM simulated_mail WHERE user_id=$1 AND id=ANY($2::bigint[])", [userId, ids]);
    if (selected.rowCount !== new Set(ids).size) throw new RequestError(404, "mail_not_found");
    if (input.action === "trash") await db.query("UPDATE simulated_mail SET mailbox='TRASH' WHERE user_id=$1 AND id=ANY($2::bigint[])", [userId, ids]);
    else {
      if (input.important !== undefined && typeof input.important !== "boolean") throw new RequestError(400, "invalid_favorite");
      await db.query("UPDATE simulated_mail SET important=COALESCE($3,NOT important) WHERE user_id=$1 AND id=ANY($2::bigint[])", [userId, ids, input.important ?? null]);
    }
    return {};
  }
  if (input.action === "list") {
    const offset = input.offset ?? 0;
    if (typeof offset !== "number" || !Number.isSafeInteger(offset) || offset < 0 || offset > 100000 ||
      (input.query !== undefined && (typeof input.query !== "string" || input.query.length > 500)) ||
      (input.favorite !== undefined && typeof input.favorite !== "boolean")) throw new RequestError(400, "invalid_mail_query");
    let thread: string | null = null;
    if (input.threadId !== undefined) {
      const source = await db.query<Row>("SELECT * FROM simulated_mail WHERE user_id=$1 AND id=$2 AND mailbox <> 'TRASH'", [userId, mailId(input.threadId)]);
      if (!source.rows[0]) throw new RequestError(404, "mail_not_found");
      thread = source.rows[0].thread_id ?? source.rows[0].id;
    }
    const result = await db.query<Row>(`SELECT * FROM simulated_mail WHERE user_id=$1 AND mailbox <> 'TRASH'
      AND ($2::boolean IS NOT TRUE OR important) AND ($3::text IS NULL OR strpos(lower(subject || ' ' || body),lower($3)) > 0)
      AND ($4::bigint IS NULL OR (COALESCE(thread_id,id)=$4 AND id <> $5))
      ORDER BY created_at DESC,id DESC LIMIT 20 OFFSET $6`, [userId, input.favorite ?? false, input.query ?? null, thread, input.threadId ?? null, offset]);
    return result.rows.map(row => model(row, people));
  }
  throw new RequestError(400, "invalid_mail_action");
}
