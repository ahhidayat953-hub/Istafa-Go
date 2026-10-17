package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AuditLog
import com.example.data.model.DriverSubmission
import com.example.data.model.Order
import com.example.data.model.PricingSetting
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

class AdminRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAdminId(): String {
        return Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("Admin must be signed in")
    }

    fun observePendingDriverSubmissions(): Flow<List<DriverSubmission>> {
        val query = db.collection("driver_submissions")
        return query.snapshots().map { snapshot ->
            snapshot.toObjects(DriverSubmission::class.java).sortedByDescending { it.submittedAt?.seconds ?: 0L }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "driver_submissions")
            emit(emptyList())
        }
    }

    suspend fun reviewDriverSubmission(
        submissionId: String,
        driverId: String,
        approve: Boolean,
        reason: String = ""
    ): Result<Unit> {
        val adminUid = requireAdminId()
        val submissionRef = db.collection("driver_submissions").document(submissionId)
        val driverProfileRef = db.collection("driver_profiles").document(driverId)

        return try {
            val submissionStatus = if (approve) "APPROVED" else "REJECTED"
            val profileStatus = if (approve) "APPROVED" else "REJECTED"

            submissionRef.update(
                mapOf(
                    "status" to submissionStatus,
                    "rejectionReason" to reason,
                    "reviewedAt" to FieldValue.serverTimestamp(),
                    "reviewedBy" to adminUid
                )
            ).await()

            driverProfileRef.update(
                mapOf(
                    "status" to profileStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            createAuditLog(
                action = if (approve) "APPROVE_DRIVER" else "REJECT_DRIVER",
                targetResource = "driver_submissions",
                targetId = submissionId,
                details = "Driver ID: $driverId, Result: $submissionStatus"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, submissionRef.path)
            Result.failure(e)
        }
    }

    fun observeAllLiveOrders(): Flow<List<Order>> {
        val query = db.collection("orders")
        return query.snapshots().map { snapshot ->
            snapshot.toObjects(Order::class.java).sortedByDescending { it.createdAt?.seconds ?: 0L }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "orders")
            emit(emptyList())
        }
    }

    fun observePricingSettings(): Flow<List<PricingSetting>> {
        val query = db.collection("pricing_settings")
        return query.snapshots().map { snapshot ->
            snapshot.toObjects(PricingSetting::class.java)
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "pricing_settings")
            emit(emptyList())
        }
    }

    suspend fun updatePricingSetting(setting: PricingSetting): Result<Unit> {
        val docRef = db.collection("pricing_settings").document(setting.serviceType)
        return try {
            docRef.set(
                mapOf(
                    "serviceType" to setting.serviceType,
                    "baseFare" to setting.baseFare,
                    "perKmRate" to setting.perKmRate,
                    "minimumFare" to setting.minimumFare,
                    "platformFeePercent" to setting.platformFeePercent,
                    "isActive" to setting.isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            createAuditLog(
                action = "UPDATE_PRICING",
                targetResource = "pricing_settings",
                targetId = setting.serviceType,
                details = "BaseFare: ${setting.baseFare}, PerKmRate: ${setting.perKmRate}"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeAuditLogs(): Flow<List<AuditLog>> {
        val query = db.collection("audit_logs")
        return query.snapshots().map { snapshot ->
            snapshot.toObjects(AuditLog::class.java).sortedByDescending { it.timestamp?.seconds ?: 0L }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "audit_logs")
            emit(emptyList())
        }
    }

    suspend fun createAuditLog(action: String, targetResource: String, targetId: String, details: String): Result<Unit> {
        val adminUid = try { requireAdminId() } catch (e: Exception) { "SYSTEM" }
        val logsRef = db.collection("audit_logs")
        return try {
            logsRef.add(
                mapOf(
                    "adminUid" to adminUid,
                    "action" to action,
                    "targetResource" to targetResource,
                    "targetId" to targetId,
                    "details" to details,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, logsRef.path)
            Result.failure(e)
        }
    }
}
