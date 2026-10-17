package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.DriverProfile
import com.example.data.model.DriverSubmission
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DriverRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("Driver must be signed in")
    }

    fun observeDriverProfile(driverId: String): Flow<DriverProfile?> {
        val docRef = db.collection("driver_profiles").document(driverId)
        return docRef.snapshots().map { snapshot ->
            if (snapshot.exists()) snapshot.toObject(DriverProfile::class.java) else null
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.GET, docRef.path)
            emit(null)
        }
    }

    fun observeDriverSubmission(driverId: String): Flow<DriverSubmission?> {
        val query = db.collection("driver_submissions").whereEqualTo("driverId", driverId)
        return query.snapshots().map { snapshot ->
            snapshot.toObjects(DriverSubmission::class.java).firstOrNull()
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "driver_submissions")
            emit(null)
        }
    }

    suspend fun submitDriverRegistration(
        fullName: String,
        phone: String,
        vehicleType: String,
        plateNumber: String,
        vehicleModel: String,
        ktpNumber: String,
        simNumber: String
    ): Result<Unit> {
        val uid = requireUserId()
        val profileRef = db.collection("driver_profiles").document(uid)
        val submissionRef = db.collection("driver_submissions").document(uid)

        val profilePayload = hashMapOf<String, Any>(
            "userId" to uid,
            "fullName" to fullName,
            "phone" to phone,
            "vehicleType" to vehicleType,
            "plateNumber" to plateNumber,
            "vehicleModel" to vehicleModel,
            "status" to "PENDING_VERIFICATION",
            "isOnline" to false,
            "rating" to 5.0,
            "totalTrips" to 0L,
            "walletBalance" to 0.0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val submissionPayload = hashMapOf<String, Any>(
            "driverId" to uid,
            "driverName" to fullName,
            "phone" to phone,
            "ktpNumber" to ktpNumber,
            "simNumber" to simNumber,
            "vehicleType" to vehicleType,
            "plateNumber" to plateNumber,
            "status" to "PENDING",
            "submittedAt" to FieldValue.serverTimestamp()
        )

        return try {
            profileRef.set(profilePayload).await()
            submissionRef.set(submissionPayload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profileRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleOnlineStatus(driverId: String, isOnline: Boolean): Result<Unit> {
        val docRef = db.collection("driver_profiles").document(driverId)
        return try {
            docRef.update(
                mapOf(
                    "isOnline" to isOnline,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateLocation(driverId: String, lat: Double, lng: Double): Result<Unit> {
        val docRef = db.collection("driver_profiles").document(driverId)
        return try {
            docRef.update(
                mapOf(
                    "currentLat" to lat,
                    "currentLng" to lng,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Mencari pengemudi terdekat yang aktif (online) dan telah disetujui (APPROVED).
     */
    suspend fun findNearbyApprovedDrivers(
        pickupLat: Double,
        pickupLng: Double,
        vehicleType: String? = null,
        maxRadiusKm: Double = 15.0
    ): List<Pair<DriverProfile, Double>> {
        return try {
            val query = db.collection("driver_profiles")
                .whereEqualTo("status", "APPROVED")
                .whereEqualTo("isOnline", true)

            val snapshot = query.get().await()
            val drivers = snapshot.toObjects(DriverProfile::class.java)

            drivers
                .filter { vehicleType == null || it.vehicleType == vehicleType }
                .map { driver ->
                    val dist = OrderRepository.calculateHaversineDistanceKm(
                        pickupLat, pickupLng,
                        driver.currentLat, driver.currentLng
                    )
                    Pair(driver, dist)
                }
                .filter { it.second <= maxRadiusKm }
                .sortedBy { it.second }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
