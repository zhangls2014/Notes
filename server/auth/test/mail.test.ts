import { test } from "node:test";
import assert from "node:assert/strict";
import { readMailSend, initialMail } from "../src/mail.ts";

test("seed contains populated example messages", () => {
  const mail = initialMail();
  assert.ok(mail.length >= 10);
  assert.ok(mail.every(item => item.body.length && item.subject.length));
});

test("send rejects invalid recipients, missing content and invalid source", () => {
  const valid = { operationId: "12345678-1234-1234-1234-123456789abc", recipientIds: [2], subject: "Hello", body: "Hi", mode: "new" };
  assert.equal(readMailSend(valid).body, "Hi");
  for (const change of [{recipientIds: []}, {recipientIds: [999]}, {body: " "}, {mode: "reply"}, {mode: "bad"}]) {
    assert.throws(() => readMailSend({...valid, ...change}));
  }
});

test("mail IDs reject unsafe integers and send canonicalizes recipients", async () => {
  const { mailId } = await import("../src/mail.ts");
  for (const id of [0, -1, 1.5, Number.MAX_SAFE_INTEGER + 1, "1"]) assert.throws(() => mailId(id));
  assert.deepEqual(readMailSend({ operationId: "12345678-1234-1234-1234-123456789abc", recipientIds: [3, 2, 3], subject: "", body: "Text", mode: "forward", sourceId: 1 }).recipientIds, [2, 3]);
});
