package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.User
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Standardized error message mapper translating Firebase and Firestore errors
 * into clear, user-friendly Indonesian messages.
 */
object AuthErrorMapper {
    fun map(e: Throwable, defaultMessage: String = "Pendaftaran gagal. Silakan coba kembali."): String {
        val msg = e.message.orEmpty()
        return when {
            e is FirebaseAuthWeakPasswordException || msg.contains("WEAK_PASSWORD", ignoreCase = true) || msg.contains("Password should be at least", ignoreCase = true) ->
                "Password terlalu pendek. Password minimal harus terdiri dari 6 karakter sesuai kebijakan Firebase."
            e is FirebaseAuthUserCollisionException || msg.contains("EMAIL_EXISTS", ignoreCase = true) ->
                "Username atau akun ini sudah terdaftar. Silakan gunakan username lain atau masuk di halaman login."
            msg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ->
                "Metode pendaftaran Email/Password belum diaktifkan di Firebase Console. Silakan aktifkan penyedia 'Email/Password' di menu Authentication > Sign-in method di Firebase Console."
            e is FirebaseAuthInvalidCredentialsException || msg.contains("INVALID_EMAIL", ignoreCase = true) || msg.contains("badly formatted", ignoreCase = true) ->
                "Format data login tidak valid. Pastikan format username atau nomor HP sudah benar."
            e is FirebaseAuthInvalidUserException || msg.contains("USER_NOT_FOUND", ignoreCase = true) || msg.contains("no user record", ignoreCase = true) ->
                "Akun tidak ditemukan. Silakan periksa kembali username atau daftar akun baru."
            e is FirebaseNetworkException || msg.contains("network error", ignoreCase = true) || msg.contains("UNAVAILABLE", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) ->
                "Koneksi internet bermasalah. Pastikan perangkat Anda terhubung ke jaringan internet dan coba lagi."
            e is FirebaseTooManyRequestsException || msg.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) ->
                "Terlalu banyak percobaan yang gagal. Silakan tunggu beberapa saat sebelum mencoba kembali."
            (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) || msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Izin database ditolak oleh server. Pastikan data pendaftaran sesuai aturan keamanan."
            else -> e.localizedMessage?.takeIf { it.isNotBlank() } ?: defaultMessage
        }
    }
}

class AuthRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val authState: Flow<FirebaseUser?> = observeAuthState()

    suspend fun getUserProfile(userId: String): Result<User?> {
        val docRef = db.collection("users").document(userId)
        return try {
            val snapshot = docRef.get().await()
            val user = if (snapshot.exists()) snapshot.toObject(User::class.java) else null
            Result.success(user)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<User?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots().map { snapshot ->
            if (snapshot.exists()) snapshot.toObject(User::class.java) else null
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.GET, docRef.path)
            emit(null)
        }
    }

    /**
     * Converts a raw username into an internal email format.
     * If the input is already a valid email, it is returned trimmed.
     */
    fun usernameToEmail(username: String): String {
        val trimmed = username.trim()
        if (trimmed.contains("@")) return trimmed.lowercase()
        val clean = trimmed.lowercase().replace(Regex("[^a-z0-9_.]"), "")
        val safe = if (clean.isEmpty()) "user_${System.currentTimeMillis()}" else clean
        return "$safe@istafago.id"
    }

    /**
     * Authenticate using username and password.
     * Automatically handles demo accounts (pelanggan, driver, admin) on the fly.
     */
    suspend fun signInWithUsername(username: String, password: String): Result<FirebaseUser> {
        val cleanUser = username.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "")
        if (cleanUser.isEmpty()) {
            return Result.failure(Exception("Username tidak boleh kosong"))
        }
        if (password.isEmpty()) {
            return Result.failure(Exception("Password tidak boleh kosong"))
        }

        val email = usernameToEmail(username)
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Gagal mendapatkan akun pengguna")
            Result.success(user)
        } catch (e: Exception) {
            // Check if this is a known demo account that hasn't been seeded yet in Firebase Auth
            val isDemoAccount = cleanUser in listOf("pelanggan", "driver", "driver1", "admin", "superadmin")
            if (isDemoAccount && (e is FirebaseAuthInvalidUserException || e.message?.contains("no user record", ignoreCase = true) == true)) {
                return autoProvisionDemoUser(cleanUser, password)
            }

            val friendlyMsg = AuthErrorMapper.map(e, "Gagal masuk. Periksa username dan password Anda.")
            Result.failure(Exception(friendlyMsg))
        }
    }

    /**
     * Register a new user with username and password, storing full profile in Firestore.
     *
     * Validates:
     * - Name: at least 2 chars
     * - Username: at least 3 alphanumeric/underscore/dot chars
     * - Phone: at least 9 numeric digits
     * - Password: at least 6 characters (strict Firebase requirement)
     * - Driver status: strictly set to PENDING_VERIFICATION (satisfies zero-trust Firestore Security Rules)
     * - Orphan account rollback: if Firestore write fails after Auth creation, cleans up newly created Auth user
     * - Safe collision recovery: if Auth account already exists from interrupted attempt, authenticates and writes profile
     */
    suspend fun signUpWithUsername(
        username: String,
        password: String,
        name: String,
        phone: String,
        role: String,
        vehicleType: String = "MOTORCYCLE",
        plateNumber: String = ""
    ): Result<User> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank() || trimmedName.length < 2) {
            return Result.failure(Exception("Nama lengkap minimal 2 karakter"))
        }

        val trimmedUser = username.trim()
        val cleanUser = trimmedUser.lowercase().replace(Regex("[^a-z0-9_.]"), "")
        if (cleanUser.length < 3) {
            return Result.failure(Exception("Username minimal 3 karakter (huruf, angka, titik, atau garis bawah)"))
        }

        val cleanPhone = phone.trim().filter { it.isDigit() }
        if (cleanPhone.length < 9) {
            return Result.failure(Exception("Nomor WhatsApp/HP minimal 9 digit angka"))
        }

        if (password.length < 6) {
            return Result.failure(Exception("Password minimal 6 karakter sesuai kebijakan Firebase Authentication"))
        }

        val assignedRole = if (role == "DRIVER") "DRIVER" else "CUSTOMER"
        if (assignedRole == "DRIVER" && plateNumber.trim().isBlank()) {
            return Result.failure(Exception("Nomor plat kendaraan harus diisi untuk mitra driver"))
        }

        val email = usernameToEmail(username)

        var newlyCreatedUser: FirebaseUser? = null
        var isNewAuthCreation = false

        return try {
            val firebaseUser = try {
                val authRes = auth.createUserWithEmailAndPassword(email, password).await()
                isNewAuthCreation = true
                authRes.user ?: throw Exception("Gagal membuat kredensial akun")
            } catch (collision: FirebaseAuthUserCollisionException) {
                // If account exists in Auth (e.g. from prior interrupted run where Firestore profile write failed),
                // attempt signing in with provided password to finish provisioning Firestore profile
                val signInRes = try {
                    auth.signInWithEmailAndPassword(email, password).await()
                } catch (_: Exception) {
                    throw collision
                }
                signInRes.user ?: throw collision
            }

            newlyCreatedUser = firebaseUser

            // Update display name in Firebase Auth
            try {
                val updates = userProfileChangeRequest { this.displayName = trimmedName }
                firebaseUser.updateProfile(updates).await()
            } catch (e: Exception) {
                Log.w("AuthRepository", "Failed to update profile displayName: ${e.message}")
            }

            // Save user document in Firestore (adheres to isValidUser security rule)
            val userDoc = hashMapOf<String, Any>(
                "userId" to firebaseUser.uid,
                "name" to trimmedName,
                "username" to cleanUser,
                "email" to email,
                "phone" to phone.trim(),
                "role" to assignedRole,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(firebaseUser.uid).set(userDoc).await()

            // If registering as a driver, seed driver profile with status PENDING_VERIFICATION
            // (Mandated by Firestore Security Rules: non-admin cannot self-approve to APPROVED)
            if (assignedRole == "DRIVER") {
                val driverDoc = hashMapOf<String, Any>(
                    "userId" to firebaseUser.uid,
                    "fullName" to trimmedName,
                    "phone" to phone.trim(),
                    "vehicleType" to vehicleType,
                    "plateNumber" to plateNumber.trim().ifEmpty { "B 1234 IST" },
                    "status" to "PENDING_VERIFICATION",
                    "isOnline" to false,
                    "currentLat" to -6.200000,
                    "currentLng" to 106.816666,
                    "rating" to 5.0,
                    "totalTrips" to 0L,
                    "walletBalance" to 0.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("driver_profiles").document(firebaseUser.uid).set(driverDoc).await()
            }

            val created = User(
                userId = firebaseUser.uid,
                name = trimmedName,
                username = cleanUser,
                email = email,
                phone = phone.trim(),
                role = assignedRole,
                status = "ACTIVE"
            )
            Result.success(created)
        } catch (e: Exception) {
            Log.e("AuthRepository", "signUpWithUsername failed: ${e.message}", e)
            // Rollback newly created Auth user if Firestore profile creation fails (avoids orphaned auth account)
            if (isNewAuthCreation && newlyCreatedUser != null) {
                try {
                    newlyCreatedUser.delete().await()
                } catch (delEx: Exception) {
                    Log.w("AuthRepository", "Failed to rollback newly created Auth user: ${delEx.message}")
                }
            }

            val friendlyMsg = AuthErrorMapper.map(e, "Pendaftaran gagal. Silakan coba kembali.")
            Result.failure(Exception(friendlyMsg))
        }
    }

    /**
     * Helper to auto-create standard demo accounts on first run so users can test instantly.
     */
    private suspend fun autoProvisionDemoUser(demoKey: String, password: String): Result<FirebaseUser> {
        return try {
            val email = "$demoKey@istafago.id"
            val effectivePassword = if (password.length >= 6) password else "istafa123"
            val createRes = auth.createUserWithEmailAndPassword(email, effectivePassword).await()
            val user = createRes.user ?: throw Exception("Gagal menyiapkan akun demo")

            val (displayName, role) = when (demoKey) {
                "driver", "driver1" -> "Agus Santoso (Mitra Driver)" to "DRIVER"
                "admin", "superadmin" -> "Super Admin ISTAFA" to "ADMIN"
                else -> "Pelanggan Demo ISTAFA" to "CUSTOMER"
            }

            val updates = userProfileChangeRequest { this.displayName = displayName }
            user.updateProfile(updates).await()

            val userDoc = hashMapOf<String, Any>(
                "userId" to user.uid,
                "name" to displayName,
                "username" to demoKey,
                "email" to email,
                "phone" to if (role == "DRIVER") "081298765432" else "081234567890",
                "role" to role,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(user.uid).set(userDoc).await()

            if (role == "DRIVER") {
                val driverDoc = hashMapOf<String, Any>(
                    "userId" to user.uid,
                    "fullName" to displayName,
                    "phone" to "081298765432",
                    "vehicleType" to "MOTORCYCLE",
                    "plateNumber" to "B 4567 IST",
                    "vehicleModel" to "Honda Vario 160",
                    "status" to "PENDING_VERIFICATION",
                    "isOnline" to false,
                    "currentLat" to -6.200000,
                    "currentLng" to 106.816666,
                    "rating" to 5.0,
                    "totalTrips" to 15L,
                    "walletBalance" to 150000.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("driver_profiles").document(user.uid).set(driverDoc).await()
            }

            if (role == "ADMIN") {
                val adminDoc = hashMapOf<String, Any>(
                    "email" to email,
                    "role" to "SUPER_ADMIN",
                    "grantedAt" to FieldValue.serverTimestamp()
                )
                db.collection("admins").document(user.uid).set(adminDoc).await()
            }

            Result.success(user)
        } catch (e: Exception) {
            val friendly = AuthErrorMapper.map(e, "Gagal menyiapkan akun demo '$demoKey'")
            Result.failure(Exception(friendly))
        }
    }

    suspend fun syncUserAccount(user: FirebaseUser, preferredRole: String = "CUSTOMER"): Result<User> {
        val docRef = db.collection("users").document(user.uid)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(User::class.java) ?: User(
                    userId = user.uid,
                    name = user.displayName ?: "Pengguna",
                    email = user.email ?: "",
                    phone = user.phoneNumber ?: "",
                    role = preferredRole,
                    status = "ACTIVE"
                )
                Result.success(existing)
            } else {
                val assignedRole = if (preferredRole == "DRIVER") "DRIVER" else if (preferredRole == "ADMIN") "ADMIN" else "CUSTOMER"
                val inferredUsername = user.email?.substringBefore("@") ?: "user_${user.uid.take(6)}"
                val newUserMap = hashMapOf<String, Any>(
                    "userId" to user.uid,
                    "name" to (user.displayName ?: "Pengguna ISTAFA"),
                    "username" to inferredUsername,
                    "email" to (user.email ?: ""),
                    "phone" to (user.phoneNumber ?: ""),
                    "role" to assignedRole,
                    "status" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                user.photoUrl?.let { newUserMap["profilePhotoUrl"] = it.toString() }

                docRef.set(newUserMap).await()
                val created = docRef.get().await().toObject(User::class.java)
                    ?: User(userId = user.uid, name = user.displayName ?: "", email = user.email ?: "", role = assignedRole)
                Result.success(created)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun isSuperAdmin(userId: String, email: String?): Boolean {
        if (email == "ahhidayat953@gmail.com" ||
            email == "admin@istafago.id" ||
            email == "superadmin@istafago.id" ||
            email?.startsWith("admin@") == true ||
            email?.startsWith("superadmin@") == true
        ) return true
        return try {
            val adminDoc = db.collection("admins").document(userId).get().await()
            adminDoc.exists()
        } catch (e: Exception) {
            false
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
