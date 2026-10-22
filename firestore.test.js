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

test("Unauthenticated user: cannot read products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("products").get());
});

test("Authenticated user: cannot read another user's products", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("products").doc("prod_1").set({
      name: "Riz 25kg",
      category: "Alimentation",
      purchasePriceUSD: 20.0,
      sellingPriceUSD: 24.0,
      stockQuantity: 15,
      minStockAlert: 5,
      unit: "sac"
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("products").doc("prod_1").get());
});

test("Authenticated user: can create and read their own product", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_alice").set({
    name: "Huile végétale 5L",
    category: "Alimentation",
    purchasePriceUSD: 8.5,
    sellingPriceUSD: 10.0,
    stockQuantity: 20,
    minStockAlert: 4,
    unit: "bidon"
  }));

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("products").get());
});

test("Authenticated user: can create a valid sale", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("sales").doc("sale_1").set({
    items: [{ productId: "p1", productName: "Huile", quantity: 2, unitPriceUSD: 10, totalUSD: 20 }],
    totalUSD: 20.0,
    totalFC: 57000.0,
    currencyUsed: "USD",
    exchangeRate: 2850.0,
    grossProfitUSD: 3.0,
    paymentType: "CASH",
    isPaid: true
  }));
});
