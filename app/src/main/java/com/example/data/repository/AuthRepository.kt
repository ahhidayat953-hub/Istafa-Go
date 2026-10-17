package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.User
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

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

    fun observeUserProfile(userId: String): Flow<User?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots().map { snapshot ->
            if (snapshot.exists()) snapshot.toObject(User::class.java) else null
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.GET, docRef.path)
            emit(null)
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
                val assignedRole = if (preferredRole == "DRIVER") "DRIVER" else "CUSTOMER"
                val newUserMap = hashMapOf<String, Any>(
                    "userId" to user.uid,
                    "name" to (user.displayName ?: "Pengguna ISTAFA"),
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
        if (email == "ahhidayat953@gmail.com") return true
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
