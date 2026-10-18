/**
 * Skrip Resmi Pembuatan Akun Super Admin Pertama ISTAFA GO
 * Menggunakan Firebase Admin SDK (Lingkungan Backend Tepercaya)
 *
 * Cara Menjalankan:
 * 1. Di direktori functions, instal dependensi jika belum: npm install
 * 2. Siapkan file kunci service account (serviceAccountKey.json) dari Firebase Console:
 *    Project Settings > Service Accounts > Generate new private key
 * 3. Jalankan perintah:
 *    node bootstrap_superadmin.js <email_admin> <password_rahasia> <nama_admin>
 *
 * Contoh:
 *    node bootstrap_superadmin.js admin@istafago.id PasswordKuatAdmin2026! "Super Admin Utama"
 */

const admin = require("firebase-admin");

// Inisialisasi Firebase Admin
// Jika dijalankan di Cloud Functions / Cloud Shell dengan GOOGLE_APPLICATION_CREDENTIALS:
if (!admin.apps.length) {
  try {
    const serviceAccount = require("./serviceAccountKey.json");
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
  } catch (err) {
    // Fallback ke Application Default Credentials jika serviceAccountKey.json belum ada di lokal
    admin.initializeApp();
  }
}

const auth = admin.auth();
const db = admin.firestore();

async function bootstrapSuperAdmin(email, password, displayName) {
  if (!email || !password) {
    console.error("Gunakan format: node bootstrap_superadmin.js <email> <password> [displayName]");
    process.exit(1);
  }

  if (password.length < 6) {
    console.error("Error: Password minimal harus 6 karakter sesuai kebijakan keamanan Firebase.");
    process.exit(1);
  }

  console.log(`Memulai proses inisialisasi akun Super Admin untuk: ${email}...`);

  let userRecord;
  try {
    // 1. Cek apakah akun Auth sudah terdaftar
    userRecord = await auth.getUserByEmail(email);
    console.log(`Akun Firebase Auth sudah ada (UID: ${userRecord.uid}). Memperbarui profil...`);
    userRecord = await auth.updateUser(userRecord.uid, {
      password: password,
      displayName: displayName || "Super Admin ISTAFA"
    });
  } catch (err) {
    if (err.code === "auth/user-not-found") {
      // 2. Buat akun baru di Firebase Auth jika belum ada
      console.log("Akun belum ada di Firebase Auth. Membuat kredensial baru...");
      userRecord = await auth.createUser({
        email: email,
        password: password,
        displayName: displayName || "Super Admin ISTAFA",
        emailVerified: true
      });
      console.log(`Kredensial berhasil dibuat dengan UID: ${userRecord.uid}`);
    } else {
      console.error("Gagal memeriksa akun Firebase Auth:", err);
      process.exit(1);
    }
  }

  const uid = userRecord.uid;

  // 3. Tetapkan Custom User Claim untuk keamanan token level-0
  console.log("Menetapkan Custom User Claims { superAdmin: true }...");
  await auth.setCustomUserClaims(uid, {
    superAdmin: true,
    role: "ADMIN"
  });

  // 4. Daftarkan di koleksi admins Firestore (diperiksa oleh firestore.rules)
  console.log(`Mendaftarkan hak akses di database Firestore: admins/${uid}...`);
  await db.collection("admins").doc(uid).set({
    email: email,
    role: "SUPER_ADMIN",
    grantedAt: admin.firestore.FieldValue.serverTimestamp(),
    grantedBy: "BOOTSTRAP_CLI"
  }, { merge: true });

  // 5. Daftarkan profil pengguna di koleksi users Firestore
  const username = email.split("@")[0].toLowerCase().replace(/[^a-z0-9_.]/g, "");
  console.log(`Menyimpan profil master di database Firestore: users/${uid}...`);
  await db.collection("users").doc(uid).set({
    userId: uid,
    name: displayName || "Super Admin ISTAFA",
    username: username,
    email: email,
    phone: "081200000000",
    role: "ADMIN",
    status: "ACTIVE",
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });

  console.log("\n========================================================");
  console.log("SUKSES: Akun Super Admin Resmi Berhasil Dikonfigurasi!");
  console.log(`- Email/Username : ${email} (atau username: '${username}')`);
  console.log(`- User ID (UID)  : ${uid}`);
  console.log("- Hak Akses      : SUPER_ADMIN (Custom Claims & admins/{uid})");
  console.log("========================================================\n");
}

const args = process.argv.slice(2);
const emailArg = args[0] || "admin@istafago.id";
const passArg = args[1] || "AdminIstafa2026!";
const nameArg = args[2] || "Super Admin Utama";

bootstrapSuperAdmin(emailArg, passArg, nameArg).catch((e) => {
  console.error("Fatal Error saat bootstrap:", e);
  process.exit(1);
});
