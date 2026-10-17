const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CUSTOMER_UID = "customer_123";
const DRIVER_UID = "driver_456";
const STRANGER_UID = "stranger_789";
const ADMIN_UID = "admin_master";

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

// 1. Unauthenticated Security Bounds
test("Unauthenticated user: cannot read users collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
});

test("Unauthenticated user: cannot create an order", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("orders").doc("order_unauth").set({
      customerId: "anyone",
      customerName: "Anonymous",
      serviceType: "GO_RIDE",
      status: "SEARCHING",
      pickupAddress: "Jl. Merdeka",
      dropoffAddress: "Stasiun",
      fare: 15000,
      distanceKm: 3.5,
      paymentMethod: "CASH",
      paymentStatus: "PENDING",
      createdAt: new Date(),
    })
  );
});

// 2. User Isolation & Role Escalation Prevention
test("Customer: can create own profile as CUSTOMER", async () => {
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  await assertSucceeds(
    customerDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      name: "Budi Santoso",
      email: "budi@example.com",
      role: "CUSTOMER",
      status: "ACTIVE",
      createdAt: new Date(),
    })
  );
});

test("Customer: cannot escalate own role to ADMIN on profile creation", async () => {
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  await assertFails(
    customerDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      name: "Hacker User",
      email: "hacker@example.com",
      role: "ADMIN",
      status: "ACTIVE",
      createdAt: new Date(),
    })
  );
});

test("Customer: cannot read another user's profile document", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc("secret_user").set({
      userId: "secret_user",
      name: "Secret User",
      email: "secret@example.com",
      role: "CUSTOMER",
      status: "ACTIVE",
      createdAt: new Date(),
    });
  });

  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  await assertFails(customerDb.collection("users").doc("secret_user").get());
});

// 3. Driver Profile Security & Verification Status Lock
test("Driver: can register own profile in PENDING_VERIFICATION status", async () => {
  const driverDb = testEnv.authenticatedContext(DRIVER_UID).firestore();
  await assertSucceeds(
    driverDb.collection("driver_profiles").doc(DRIVER_UID).set({
      userId: DRIVER_UID,
      fullName: "Agus Driver",
      phone: "08123456789",
      vehicleType: "MOTORCYCLE",
      plateNumber: "B 1234 XYZ",
      status: "PENDING_VERIFICATION",
      isOnline: false,
      createdAt: new Date(),
    })
  );
});

test("Driver: cannot self-approve own profile to APPROVED status", async () => {
  const driverDb = testEnv.authenticatedContext(DRIVER_UID).firestore();
  await assertFails(
    driverDb.collection("driver_profiles").doc(DRIVER_UID).set({
      userId: DRIVER_UID,
      fullName: "Agus Driver",
      phone: "08123456789",
      vehicleType: "MOTORCYCLE",
      plateNumber: "B 1234 XYZ",
      status: "APPROVED", // Malicious self-approval
      isOnline: true,
      createdAt: new Date(),
    })
  );
});

// 4. Order Lifecycle & Fare Tampering Prevention
test("Customer: can create a valid SEARCHING order", async () => {
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  await assertSucceeds(
    customerDb.collection("orders").doc("order_valid_1").set({
      customerId: CUSTOMER_UID,
      customerName: "Budi Santoso",
      serviceType: "GO_RIDE",
      status: "SEARCHING",
      pickupAddress: "Jl. Sudirman No. 10",
      dropoffAddress: "Mall Istafa",
      fare: 15000,
      distanceKm: 4.2,
      paymentMethod: "CASH",
      paymentStatus: "PENDING",
      createdAt: new Date(),
    })
  );
});

test("Stranger: cannot tamper with or modify fare of customer order", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("orders").doc("order_1").set({
      customerId: CUSTOMER_UID,
      customerName: "Budi Santoso",
      serviceType: "GO_RIDE",
      status: "SEARCHING",
      pickupAddress: "Jl. Sudirman No. 10",
      dropoffAddress: "Mall Istafa",
      fare: 15000,
      distanceKm: 4.2,
      paymentMethod: "CASH",
      paymentStatus: "PENDING",
      createdAt: new Date(),
    });
  });

  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  await assertFails(
    strangerDb.collection("orders").doc("order_1").update({
      fare: 500, // Malicious fare modification
      updatedAt: new Date(),
    })
  );
});

test("Driver: can accept a SEARCHING order without altering the fare", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("orders").doc("order_to_accept").set({
      customerId: CUSTOMER_UID,
      customerName: "Budi Santoso",
      serviceType: "GO_RIDE",
      status: "SEARCHING",
      pickupAddress: "Jl. Sudirman",
      dropoffAddress: "Mall Istafa",
      fare: 15000,
      distanceKm: 4.2,
      paymentMethod: "CASH",
      paymentStatus: "PENDING",
      createdAt: new Date(),
    });
  });

  const driverDb = testEnv.authenticatedContext(DRIVER_UID).firestore();
  await assertSucceeds(
    driverDb.collection("orders").doc("order_to_accept").update({
      status: "ACCEPTED",
      driverId: DRIVER_UID,
      driverName: "Agus Driver",
      updatedAt: new Date(),
    })
  );
});

// 5. Admin Privilege & Audit Log Protection
test("Non-admin: cannot write to audit_logs or app_settings", async () => {
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  await assertFails(
    customerDb.collection("audit_logs").doc("log_1").set({
      adminUid: CUSTOMER_UID,
      action: "DELETE_USER",
      targetResource: "users",
      targetId: "some_user",
      timestamp: new Date(),
    })
  );
});
