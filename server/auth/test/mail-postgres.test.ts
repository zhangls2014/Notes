import { test } from "node:test";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import pg from "pg";
import { mailOperation } from "../src/mail.ts";

const databaseUrl = process.env.NOTES_AUTH_TEST_DATABASE_URL;
test("PostgreSQL mail initializes once, isolates users and persists idempotent sends and threads", {
  skip: !databaseUrl && "Requires an authorized disposable database with current schema.sql",
}, async () => {
  const pool = new pg.Pool({ connectionString: databaseUrl });
  const db = await pool.connect();
  try {
    await db.query("BEGIN");
    const alice = randomUUID(), bob = randomUUID();
    for (const id of [alice, bob]) await db.query("INSERT INTO auth_users(id,account,email,password_hash) VALUES ($1,$2,$3,$4)",
      [id, "mail-" + id, id + "@example.invalid", Buffer.from("test-only")]);
    const call = (owner: string, input: Record<string, unknown>) => mailOperation(db, owner, "tester", input) as Promise<any>;
    const first = await call(alice, { action: "list" });
    assert.equal(first.length, 17);
    assert.deepEqual(await call(alice, { action: "list" }), first);
    const other = await call(bob, { action: "list" });
    assert.notEqual(first[0].id, other[0].id);
    const source = first.find((item: any) => item.mailbox === "INBOX");
    const send = { action: "send", operationId: randomUUID(), recipientIds: [2], subject: "Re: unique text", body: "Reply content", mode: "reply", sourceId: source.id };
    const reply = await call(alice, send);
    assert.equal(reply.mailbox, "SENT");
    assert.equal(reply.recipients[0].id, 2);
    assert.equal((await call(alice, send)).id, reply.id);
    const thread = await call(alice, { action: "list", threadId: source.id });
    assert.ok(thread.some((item: any) => item.id === reply.id));
    const forwarded = await call(alice, { ...send, operationId: randomUUID(), mode: "forward" });
    assert.ok(!(await call(alice, { action: "list", threadId: source.id })).some((item: any) => item.id === forwarded.id));
    await call(alice, { action: "favorite", ids: [reply.id], important: true });
    assert.equal((await call(alice, { action: "list", favorite: true }))[0].id, reply.id);
    assert.equal((await call(alice, { action: "list", query: "unique text" })).length, 2);
    await call(alice, { action: "trash", ids: first.map((item: any) => item.id).concat([reply.id, forwarded.id]) });
    assert.equal((await call(alice, { action: "list" })).length, 0);
    assert.equal((await call(bob, { action: "list" })).length, 17);
    // Expected SQL-level business rejections are last so transaction state remains valid.
    await assert.rejects(call(alice, { action: "detail", id: other[0].id }), { code: "mail_not_found" });
    await assert.rejects(call(alice, { ...send, body: "changed" }), { code: "mail_operation_conflict" });
  } finally {
    await db.query("ROLLBACK");
    db.release();
    await pool.end();
  }
});
