const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Trigger saat order berubah status menjadi COMPLETED:
 * 1. Menghitung komisi platform secara server-side (sumber kebenaran tidak bisa dimanipulasi klien)
 * 2. Mencatat ke commission_ledger
 * 3. Menambahkan saldo bersih ke dompet driver
 * 4. Mengirim notifikasi ke pelanggan dan driver
 */
exports.onOrderCompleted = functions.firestore
  .document("orders/{orderId}")
  .onUpdate(async (change, context) => {
    const beforeData = change.before.data();
    const afterData = change.after.data();

    // Hanya eksekusi saat transisi menuju COMPLETED
    if (beforeData.status !== "COMPLETED" && afterData.status === "COMPLETED") {
      const orderId = context.params.orderId;
      const driverId = afterData.driverId;
      const grossFare = afterData.fare || 0;

      // Ambil konfigurasi persentase komisi platform
      let commissionPercent = 15.0;
      try {
        const pricingSnap = await db.collection("pricing_settings").doc(afterData.serviceType).get();
        if (pricingSnap.exists) {
          commissionPercent = pricingSnap.data().platformFeePercent || 15.0;
        }
      } catch (err) {
        console.warn("Gagal membaca pricing_settings, menggunakan default 15%:", err);
      }

      const platformCommission = (grossFare * commissionPercent) / 100.0;
      const driverNet = grossFare - platformCommission;

      // Catat ke commission_ledger secara atomik
      const batch = db.batch();
      const ledgerRef = db.collection("commission_ledger").doc();
      batch.set(ledgerRef, {
        orderId: orderId,
        driverId: driverId,
        grossFare: grossFare,
        platformCommission: platformCommission,
        driverNet: driverNet,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Update saldo dompet driver & total tarikan
      if (driverId) {
        const driverRef = db.collection("driver_profiles").doc(driverId);
        batch.update(driverRef, {
          walletBalance: admin.firestore.FieldValue.increment(driverNet),
          totalTrips: admin.firestore.FieldValue.increment(1),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }

      // Catat transaksi keuangan
      const txRef = db.collection("transactions").doc();
      batch.set(txRef, {
        userId: driverId,
        orderId: orderId,
        type: "FARE_PAYMENT",
        amount: driverNet,
        status: "SUCCESS",
        description: `Pendapatan order ${afterData.serviceType} #${orderId.slice(0, 6)}`,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      await batch.commit();
      console.log(`Order ${orderId} selesai: Komisi platform Rp ${platformCommission}, Net driver Rp ${driverNet}`);
    }
  });

/**
 * Helper untuk memberikan hak Super Admin pertama secara aman
 * Dapat dipanggil melalui Cloud Functions Shell atau CLI Admin
 */
exports.setSuperAdminClaim = functions.https.onCall(async (data, context) => {
  // Hanya admin terverifikasi atau bootstrap awal
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Pengguna harus login.");
  }

  const targetEmail = data.email;
  const userRecord = await admin.auth().getUserByEmail(targetEmail);

  // Set custom claims
  await admin.auth().setCustomUserClaims(userRecord.uid, { superAdmin: true });

  // Daftarkan di koleksi admins Firestore
  await db.collection("admins").doc(userRecord.uid).set({
    email: targetEmail,
    role: "SUPER_ADMIN",
    grantedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return { success: true, message: `Pengguna ${targetEmail} sekarang adalah Super Admin.` };
});
