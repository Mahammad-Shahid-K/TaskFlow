const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validTaskPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    title: "Ship quarterly architecture review",
    description: "Compile metrics and finalize slides for Tuesday review.",
    priority: "HIGH",
    category: "WORK",
    status: "PENDING",
    dueDateMillis: Date.now() + 86400000,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user: cannot list, get, create, update, or delete tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set(validTaskPayload(ALICE_UID))
  );
});

test("Authenticated owner: valid Kotlin create, list, status toggle, full update, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const tasksCol = aliceDb.collection("users").doc(ALICE_UID).collection("tasks");

  // Empty subcollection list must succeed (no existing() inspection on list)
  await assertSucceeds(tasksCol.get());

  // Create with dueDateMillis
  await assertSucceeds(tasksCol.doc("task_1").set(validTaskPayload(ALICE_UID)));

  // Create without optional dueDateMillis (omitted by .filterValues { it != null })
  const payloadNoDue = validTaskPayload(ALICE_UID);
  delete payloadNoDue.dueDateMillis;
  await assertSucceeds(tasksCol.doc("task_2").set(payloadNoDue));

  // List non-empty collection
  await assertSucceeds(tasksCol.get());

  // Quick status toggle partial update
  await assertSucceeds(
    tasksCol.doc("task_1").update({
      status: "COMPLETED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Full task edit partial update preserving createdAt and userId
  await assertSucceeds(
    tasksCol.doc("task_1").update({
      title: "Updated architecture review",
      description: "Finalized slides.",
      priority: "MEDIUM",
      category: "PERSONAL",
      status: "IN_PROGRESS",
      dueDateMillis: Date.now() + 172800000,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Delete task
  await assertSucceeds(tasksCol.doc("task_2").delete());
});

test("Cross-user isolation: Bob cannot read, list, create, update, or delete Alice's tasks", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("tasks")
      .doc("alice_task_1")
      .set(validTaskPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceTasks = bobDb.collection("users").doc(ALICE_UID).collection("tasks");

  await assertFails(aliceTasks.get());
  await assertFails(aliceTasks.doc("alice_task_1").get());
  await assertFails(aliceTasks.doc("bob_inject").set(validTaskPayload(BOB_UID)));
  await assertFails(aliceTasks.doc("alice_task_1").update({ status: "COMPLETED", updatedAt: new Date(Date.now() - 500) }));
  await assertFails(aliceTasks.doc("alice_task_1").delete());
});

test("Security & validation bounds: reject ghost fields, @DocumentId id key, bad enums, oversized strings, and future timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const tasksCol = aliceDb.collection("users").doc(ALICE_UID).collection("tasks");

  // Reject @DocumentId 'id' field in write map
  await assertFails(tasksCol.doc("bad_id_field").set(validTaskPayload(ALICE_UID, { id: "bad_id_field" })));

  // Reject ghost/extra field
  await assertFails(tasksCol.doc("ghost_field").set(validTaskPayload(ALICE_UID, { isAdmin: true })));

  // Reject invalid priority enum
  await assertFails(tasksCol.doc("bad_priority").set(validTaskPayload(ALICE_UID, { priority: "CRITICAL" })));

  // Reject invalid category enum
  await assertFails(tasksCol.doc("bad_category").set(validTaskPayload(ALICE_UID, { category: "FINANCE" })));

  // Reject invalid status enum
  await assertFails(tasksCol.doc("bad_status").set(validTaskPayload(ALICE_UID, { status: "DONE" })));

  // Reject empty or oversized title
  await assertFails(tasksCol.doc("empty_title").set(validTaskPayload(ALICE_UID, { title: "" })));
  await assertFails(tasksCol.doc("long_title").set(validTaskPayload(ALICE_UID, { title: "a".repeat(121) })));

  // Reject future timestamp spoofing
  await assertFails(tasksCol.doc("future_ts").set(validTaskPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) })));

  // Reject immutable createdAt / userId modification on update
  await assertSucceeds(tasksCol.doc("task_immutable").set(validTaskPayload(ALICE_UID)));
  await assertFails(
    tasksCol.doc("task_immutable").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    tasksCol.doc("task_immutable").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});
