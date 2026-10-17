# PANDUAN LENGKAP PENGGUNAAN & FONDASI ISTAFA GO ANDROID

Selamat datang di repositori fondasi **ISTAFA GO**, aplikasi transportasi dan pengantaran lokal (Gojek-style) berbasis Android Native dengan Kotlin dan Jetpack Compose, terhubung ke backend Google Firebase (Firestore Enterprise & Firebase Authentication).

---

## 1. Arsitektur & Struktur Folder Proyek

```
/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml                  # Izin lokasi, internet, launcher config
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt                  # Entry point, reactive auth router, role switcher
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/Models.kt              # Data class Firestore (User, Order, DriverProfile, dsb)
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── AuthRepository.kt        # Autentikasi Google, sinkronisasi profil, admin check
│   │   │   │   │   │   ├── OrderRepository.kt       # Pemesanan trip, radar order searching, status lifecycle
│   │   │   │   │   │   ├── DriverRepository.kt      # Pendaftaran driver, switch online/offline, koordinat
│   │   │   │   │   │   └── AdminRepository.kt       # Verifikasi pengajuan driver, monitor live order, tarif
│   │   │   │   │   └── util/FirestoreErrorHandler.kt # Structured JSON error logger
│   │   │   │   └── ui/
│   │   │   │       ├── common/Components.kt         # Badge status, badge layanan, card error, dialog konfirmasi
│   │   │   │       ├── auth/AuthScreen.kt           # Login Google & gesture 5-tap Super Admin portal
│   │   │   │       ├── customer/CustomerScreen.kt   # Pemesanan GoRide, GoCar, GoSend, live trip tracker
│   │   │   │       ├── driver/DriverScreen.kt       # Verifikasi berkas, radar order, eksekusi perjalanan
│   │   │   │       ├── admin/AdminScreen.kt         # Super Admin console (verifikasi driver, tarif, audit)
│   │   │   │       └── theme/                       # Palet warna Putih & Biru ISTAFA GO (M3)
│   │   │   └── res/
│   │   │       ├── drawable/                        # Logo resmi ISTAFA GO & background adaptive icon
│   │   │       ├── mipmap-*/                        # Ikon launcher APK untuk semua kerapatan layar
│   │   │       └── values/strings.xml               # String resources & firebase_applet_config.xml
│   │   └── test/java/com/example/
│   │       ├── base/FirestoreEmulatorTestBase.kt    # Base harness pengujian lokal dengan emulator
│   │       └── IstafaGoRuleTest.kt                  # Pengujian Robolectric integritas repository & rules
│   └── build.gradle.kts                             # Dependensi Gradle, test timeout & config
├── functions/
│   ├── index.js                                     # Cloud Functions komisi atomik & bootstrap admin
│   └── package.json                                 # Dependensi backend Cloud Functions
├── firebase-blueprint.json                          # Intermediate Schema Representation Firestore
├── firestore.rules                                  # Zero-trust ABAC Security Rules Firestore
├── firestore.test.js                                # Unit test cepat JavaScript untuk keamanan rules
├── firebase.json                                    # Konfigurasi multi-database Firestore & Emulator
└── security_spec.md                                 # Threat model & Dirty Dozen payload analysis
```

---

## 2. Tiga Peran Pengguna & Cara Pengujian

Aplikasi mendukung 3 peran pengguna yang terisolasi secara aman:

### A. Pelanggan (Customer)
1. Buka aplikasi, pilih tab **Pelanggan**, lalu tekan **Masuk dengan Google**.
2. Pilih layanan: **GoRide** (Ojek Motor), **GoCar** (Mobil), atau **GoSend** (Kirim Barang).
3. Masukkan alamat jemput dan alamat tujuan (tersedia chip cepat seperti Lokasi GPS, Bandara Soetta, Monas, UI).
4. Aplikasi menghitung jarak (km) dan tarif secara otomatis berdasarkan konfigurasi `pricing_settings` di Firestore.
5. Tekan **Pesan Sekarang**. Pesanan akan berstatus `SEARCHING` dan muncul kartu pelacak aktif (*Active Trip Tracker*).
6. Pelanggan dapat membatalkan pesanan selama status masih `SEARCHING` atau `ACCEPTED`.

### B. Mitra Driver (Driver)
1. Logout atau buka dengan akun lain, pilih tab **Mitra Driver**, lalu tekan **Masuk dengan Google**.
2. **Pendaftaran Awal**:
   - Isi formulir identitas: Nama, No. HP, No. KTP (NIK), No. SIM, Jenis Kendaraan, dan Plat Nomor.
   - Tekan **Kirim Dokumen Pendaftaran**.
   - Akun akan berstatus `PENDING_VERIFICATION`. Driver **belum dapat menerima order** hingga disetujui Super Admin.
3. **Setelah Disetujui Super Admin**:
   - Muncul Dashboard Driver lengkap dengan saldo dompet, jumlah tarikan, dan rating.
   - Aktifkan sakelar **Driver Online**.
   - Pada bagian **Radar Pesanan Masuk**, pesanan pelanggan terdekat yang berstatus `SEARCHING` akan muncul secara real-time.
   - Tekan **Terima Order**.
   - Driver dapat memperbarui status perjalanan langkah demi langkah:
     - *"Saya Sudah Tiba di Titik Jemput"* (`PICKING_UP`)
     - *"Mulai Antar ke Tujuan"* (`ON_THE_WAY`)
     - *"Selesaikan Perjalanan & Terima Ongkos"* (`COMPLETED`)

### C. Super Admin
1. **Akses Tersembunyi**: Pada halaman awal (Auth Screen), **ketuk tulisan "ISTAFA GO" sebanyak 5 kali berturut-turut**.
2. Dialog rahasia **Portal Akses Super Admin** akan terbuka.
3. Tekan **Masuk Akun Admin** menggunakan akun Google admin yang telah terdaftar.
4. **Fitur Super Admin**:
   - **Verifikasi Driver**: Melihat dokumen KTP, SIM, plat nomor pendaftar. Tekan **Setujui** untuk mengizinkan driver beroperasi, atau **Tolak** dengan menyertakan alasan.
   - **Pantau Pesanan**: Melihat seluruh pesanan yang sedang aktif di sistem secara langsung.
   - **Tarif & Komisi**: Mengubah tarif dasar, tarif per km, tarif minimum, dan persentase potongan komisi platform untuk GoRide, GoCar, dan GoSend.
   - **Audit Logs**: Rekaman jejak audit yang tidak bisa dimanipulasi untuk setiap tindakan admin.

---

## 3. Prosedur Pendaftaran Akun Super Admin Pertama (Bootstrap Admin)

Sesuai instruksi keamanan, username dan password admin **tidak pernah ditanam langsung di kode klien**. Untuk menetapkan akun Anda sebagai Super Admin pertama:

### Metode 1: Menggunakan Email Pemilik Proyek (Otomatis)
Security rules dan `AuthRepository.isSuperAdmin` telah memvalidasi email akun pengembang Anda (`ahhidayat953@gmail.com`) sebagai admin awal yang terverifikasi secara aman.

### Metode 2: Melalui Firebase Console (Manual Dokumen Admin)
1. Buka [Firebase Console](https://console.firebase.google.com/).
2. Pilih proyek Firebase Anda (`gen-lang-client-0345884091`).
3. Masuk ke menu **Firestore Database**.
4. Di samping kolom koleksi, klik **Start collection** dengan nama: `admins`.
5. Masukkan **Document ID**: masukkan `UID` dari akun Google pengguna (dapat dilihat di tab *Authentication* -> *Users*).
6. Tambahkan fields:
   - `email` (string): email akun admin (contoh: `admin@istafago.com`)
   - `role` (string): `"SUPER_ADMIN"`
   - `grantedAt` (timestamp): waktu saat ini
7. Klik **Save**. Akun tersebut kini memiliki hak akses penuh Super Admin.

---

## 4. Konfigurasi Google Maps SDK API Key

Aplikasi telah disiapkan dengan Android Location Client dan izin GPS (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`).

Untuk mengaktifkan peta Google Maps Native:
1. Buka [Google Cloud Console - Credentials](https://console.cloud.google.com/apis/credentials).
2. Buat API Key baru dan aktifkan **Maps SDK for Android** serta **Places API**.
3. Batasi API Key ke nama paket Android: `com.aistudio.istafago.trvkzp` dengan SHA-1: `28:EE:16:B8:14:02:C2:D5:FA:79:DB:05:AB:CB:D5:EC:49:6E:54:CE`.
4. Masukkan API Key pada panel **Secrets** di AI Studio dengan nama key: `MAPS_API_KEY`.

---

## 5. Deployment Firebase Cloud Functions

File fungsi backend tersedia di direktori `functions/index.js`. Fungsi ini menangani kalkulasi komisi otomatis dan pencatatan ledger keuangan.

Untuk mendeploy Cloud Functions:
```bash
cd functions
npm install
firebase deploy --only functions
```

---

## 6. Cara Menyimpan Kode ke GitHub

1. Buka menu terminal atau gunakan git di workspace:
   ```bash
   git init
   git add .
   git commit -m "feat: Inisialisasi fondasi lengkap ISTAFA GO Android (Auth, Firestore, M3 UI, Rules)"
   ```
2. Hubungkan ke repositori GitHub Anda:
   ```bash
   git remote add origin https://github.com/<username-anda>/istafa-go-android.git
   git branch -M main
   git push -u origin main
   ```
3. Atau melalui AI Studio UI: Buka menu titik tiga di kanan atas -> Pilih **Push to GitHub**.

---

## 7. Cara Membangun dan Menginstal APK

1. Untuk mengunduh APK langsung dari AI Studio:
   - Klik menu pengaturan di AI Studio.
   - Pilih **Download APK** atau **Export Project as ZIP**.
2. Untuk membangun APK melalui terminal:
   ```bash
   gradle assembleDebug
   ```
   File APK hasil kompilasi akan berada di:
   `app/build/outputs/apk/debug/app-debug.apk`
3. Pasang APK pada perangkat HP Android dengan mengaktifkan opsi pengembang (*Developer Options*) dan *Install via USB* atau kirim file `.apk` langsung ke ponsel.

---
*ISTAFA GO — Transportasi dan Pengantaran Lokal Handal, Aman, dan Modern.*
