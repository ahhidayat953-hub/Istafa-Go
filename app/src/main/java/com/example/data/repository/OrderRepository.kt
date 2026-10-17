package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.DriverProfile
import com.example.data.model.Merchant
import com.example.data.model.Order
import com.example.data.model.PricingSetting
import com.example.data.model.SupportTicket
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class OrderRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google")
    }

    /**
     * Membuat pesanan perjalanan, pengantaran barang, atau makanan.
     */
    suspend fun createOrder(
        customerName: String,
        customerPhone: String,
        serviceType: String,
        pickupAddress: String,
        pickupLat: Double,
        pickupLng: Double,
        dropoffAddress: String,
        dropoffLat: Double,
        dropoffLng: Double,
        fare: Double,
        distanceKm: Double,
        paymentMethod: String = "CASH",
        // GoSend Barang parameters
        itemType: String = "",
        itemDescription: String = "",
        recipientName: String = "",
        recipientPhone: String = "",
        // GoFood Makanan parameters
        merchantId: String = "",
        merchantName: String = "",
        foodItemsSummary: String = "",
        tripNotes: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val ordersRef = db.collection("orders")
        val orderMap = hashMapOf<String, Any>(
            "customerId" to uid,
            "customerName" to customerName,
            "customerPhone" to customerPhone,
            "serviceType" to serviceType,
            "status" to "SEARCHING",
            "pickupAddress" to pickupAddress,
            "pickupLat" to pickupLat,
            "pickupLng" to pickupLng,
            "dropoffAddress" to dropoffAddress,
            "dropoffLat" to dropoffLat,
            "dropoffLng" to dropoffLng,
            "fare" to fare,
            "distanceKm" to distanceKm,
            "paymentMethod" to paymentMethod,
            "paymentStatus" to "PENDING",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (itemType.isNotBlank()) orderMap["itemType"] = itemType
        if (itemDescription.isNotBlank()) orderMap["itemDescription"] = itemDescription
        if (recipientName.isNotBlank()) orderMap["recipientName"] = recipientName
        if (recipientPhone.isNotBlank()) orderMap["recipientPhone"] = recipientPhone
        if (merchantId.isNotBlank()) orderMap["merchantId"] = merchantId
        if (merchantName.isNotBlank()) orderMap["merchantName"] = merchantName
        if (foodItemsSummary.isNotBlank()) orderMap["foodItemsSummary"] = foodItemsSummary
        if (tripNotes.isNotBlank()) orderMap["tripNotes"] = tripNotes

        return try {
            val docRef = ordersRef.add(orderMap).await()
            // Kirim notifikasi sistem ke pengguna
            sendNotification(
                recipientUserId = uid,
                title = "Pesanan Dibuat",
                body = "Sedang mencari driver terdekat untuk pesanan $serviceType Anda.",
                type = "ORDER_CREATED"
            )
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ordersRef.path)
            Result.failure(e)
        }
    }

    fun observeCustomerActiveOrders(customerId: String): Flow<List<Order>> {
        val query = db.collection("orders").whereEqualTo("customerId", customerId)
        return query.snapshots().map { snapshot ->
            snapshot.toObjects(Order::class.java).sortedByDescending { it.createdAt?.seconds ?: 0L }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "orders")
            emit(emptyList())
        }
    }

    fun observeSearchingOrders(): Flow<List<Order>> {
        val query = db.collection("orders").whereEqualTo("status", "SEARCHING")
        return query.snapshots().map { snapshot ->
            snapshot.toObjects(Order::class.java).sortedByDescending { it.createdAt?.seconds ?: 0L }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "orders")
            emit(emptyList())
        }
    }

    fun observeDriverActiveOrders(driverId: String): Flow<List<Order>> {
        val query = db.collection("orders").whereEqualTo("driverId", driverId)
        return query.snapshots().map { snapshot ->
            snapshot.toObjects(Order::class.java).filter {
                it.status in listOf("ACCEPTED", "PICKING_UP", "ARRIVED", "ON_THE_WAY")
            }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "orders")
            emit(emptyList())
        }
    }

    fun observeDriverPastOrders(driverId: String): Flow<List<Order>> {
        val query = db.collection("orders").whereEqualTo("driverId", driverId)
        return query.snapshots().map { snapshot ->
            snapshot.toObjects(Order::class.java)
                .filter { it.status in listOf("COMPLETED", "CANCELLED") }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }.catch { e ->
            if (e is Exception) handleFirestoreError(e, OperationType.LIST, "orders")
            emit(emptyList())
        }
    }

    /**
     * Menerima pesanan secara ATOMIK menggunakan Firestore Transaction:
     * Menjamin jika dua driver mengklik 'Terima' pada pesanan yang sama,
     * hanya SATU yang berhasil, dan driver kedua akan diberi tahu bahwa order sudah diambil.
     */
    suspend fun acceptOrderAtomically(
        orderId: String,
        driverId: String,
        driverName: String,
        driverPhone: String = "",
        driverPlateNumber: String = ""
    ): Result<Unit> {
        val docRef = db.collection("orders").document(orderId)
        var customerId = ""
        var serviceType = ""

        return try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentStatus = snapshot.getString("status")
                if (currentStatus != "SEARCHING") {
                    throw IllegalStateException("Pesanan sudah diambil oleh driver lain atau telah dibatalkan.")
                }
                customerId = snapshot.getString("customerId") ?: ""
                serviceType = snapshot.getString("serviceType") ?: "Layanan"

                transaction.update(
                    docRef,
                    mapOf(
                        "status" to "ACCEPTED",
                        "driverId" to driverId,
                        "driverName" to driverName,
                        "driverPhone" to driverPhone,
                        "driverPlateNumber" to driverPlateNumber,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            if (customerId.isNotBlank()) {
                sendNotification(
                    recipientUserId = customerId,
                    title = "Driver Ditemukan!",
                    body = "Driver $driverName ($driverPlateNumber) telah menerima pesanan Anda dan sedang menuju titik jemput.",
                    type = "ORDER_ACCEPTED"
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // Alias for compatibility
    suspend fun acceptOrder(orderId: String, driverId: String, driverName: String): Result<Unit> {
        return acceptOrderAtomically(orderId, driverId, driverName)
    }

    /**
     * Memperbarui status perjalanan secara berurutan:
     * ACCEPTED -> PICKING_UP -> ARRIVED -> ON_THE_WAY -> COMPLETED
     */
    suspend fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        paymentStatus: String = "PENDING"
    ): Result<Unit> {
        val docRef = db.collection("orders").document(orderId)
        return try {
            val updatePayload = mutableMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newStatus == "COMPLETED") {
                updatePayload["paymentStatus"] = "PAID"
                val snap = docRef.get().await()
                val driverId = snap.getString("driverId") ?: ""
                val fare = snap.getDouble("fare") ?: 0.0
                val netEarning = fare * 0.85
                if (driverId.isNotBlank() && fare > 0) {
                    try {
                        val driverRef = db.collection("driver_profiles").document(driverId)
                        db.runTransaction { tr ->
                            val dSnap = tr.get(driverRef)
                            if (dSnap.exists()) {
                                val currentBalance = dSnap.getDouble("walletBalance") ?: 0.0
                                val currentTrips = dSnap.getLong("totalTrips") ?: 0L
                                tr.update(driverRef, mapOf(
                                    "walletBalance" to (currentBalance + netEarning),
                                    "totalTrips" to (currentTrips + 1L),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                ))
                            }
                        }.await()
                    } catch (_: Exception) {}
                }
            }
            docRef.update(updatePayload).await()

            // Dapatkan informasi order untuk mengirimkan notifikasi ke pelanggan
            val snap = docRef.get().await()
            val customerId = snap.getString("customerId") ?: ""
            if (customerId.isNotBlank()) {
                val (title, body) = when (newStatus) {
                    "PICKING_UP" -> Pair("Driver Menuju Lokasi", "Driver sedang menuju lokasi penjemputan Anda.")
                    "ARRIVED" -> Pair("Driver Tiba", "Driver telah sampai di titik penjemputan Anda!")
                    "ON_THE_WAY" -> Pair("Dalam Perjalanan", "Perjalanan menuju tujuan telah dimulai. Harap patuhi keselamatan.")
                    "COMPLETED" -> Pair("Perjalanan Selesai", "Terima kasih telah menggunakan ISTAFA GO. Semoga perjalanan Anda menyenangkan.")
                    else -> Pair("Status Pesanan Diperbarui", "Status pesanan saat ini: $newStatus")
                }
                sendNotification(recipientUserId = customerId, title = title, body = body, type = "STATUS_UPDATE")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Memberikan rating dan ulasan bintang 1-5 untuk driver
     */
    suspend fun submitOrderRating(
        orderId: String,
        driverId: String,
        rating: Double,
        review: String
    ): Result<Unit> {
        return try {
            val orderRef = db.collection("orders").document(orderId)
            orderRef.update(
                mapOf(
                    "rating" to rating,
                    "review" to review,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (driverId.isNotBlank() && rating > 0.0) {
                val driverRef = db.collection("driver_profiles").document(driverId)
                db.runTransaction { tr ->
                    val dSnap = tr.get(driverRef)
                    if (dSnap.exists()) {
                        val currentRating = dSnap.getDouble("rating") ?: 5.0
                        val totalTrips = dSnap.getLong("totalTrips") ?: 1L
                        val newRating = if (totalTrips > 0) {
                            ((currentRating * totalTrips) + rating) / (totalTrips + 1)
                        } else {
                            rating
                        }
                        tr.update(driverRef, mapOf(
                            "rating" to (Math.round(newRating * 10.0) / 10.0),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ))
                    }
                }.await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
            Result.failure(e)
        }
    }

    /**
     * Membatalkan pesanan dengan pencatatan audit siapa yang membatalkan dan alasannya.
     */
    suspend fun cancelOrder(
        orderId: String,
        cancelledBy: String = "CUSTOMER",
        reason: String = "Dibatalkan oleh pengguna"
    ): Result<Unit> {
        val docRef = db.collection("orders").document(orderId)
        return try {
            docRef.update(
                mapOf(
                    "status" to "CANCELLED",
                    "cancelledBy" to cancelledBy,
                    "cancelReason" to reason,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val snap = docRef.get().await()
            val customerId = snap.getString("customerId") ?: ""
            val driverId = snap.getString("driverId") ?: ""

            if (cancelledBy == "CUSTOMER" && driverId.isNotBlank()) {
                sendNotification(
                    recipientUserId = driverId,
                    title = "Pesanan Dibatalkan",
                    body = "Pelanggan telah membatalkan pesanan. Alasan: $reason",
                    type = "ORDER_CANCELLED"
                )
            } else if (cancelledBy == "DRIVER" && customerId.isNotBlank()) {
                sendNotification(
                    recipientUserId = customerId,
                    title = "Driver Membatalkan Pesanan",
                    body = "Driver membatalkan pesanan. Alasan: $reason. Silakan buat pesanan baru.",
                    type = "ORDER_CANCELLED"
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getPricingSettings(serviceType: String): PricingSetting {
        return try {
            val doc = db.collection("pricing_settings").document(serviceType).get().await()
            if (doc.exists()) {
                doc.toObject(PricingSetting::class.java) ?: PricingSetting(serviceType = serviceType)
            } else {
                when (serviceType) {
                    "GO_CAR" -> PricingSetting(serviceType = "GO_CAR", baseFare = 15000.0, perKmRate = 4500.0, minimumFare = 18000.0)
                    "GO_SEND" -> PricingSetting(serviceType = "GO_SEND", baseFare = 10000.0, perKmRate = 2800.0, minimumFare = 12000.0)
                    "GO_FOOD" -> PricingSetting(serviceType = "GO_FOOD", baseFare = 10000.0, perKmRate = 2500.0, minimumFare = 12000.0)
                    else -> PricingSetting(serviceType = "GO_RIDE", baseFare = 8000.0, perKmRate = 2500.0, minimumFare = 10000.0)
                }
            }
        } catch (e: Exception) {
            PricingSetting(serviceType = serviceType)
        }
    }

    /**
     * Notifikasi in-app
     */
    suspend fun sendNotification(
        recipientUserId: String,
        title: String,
        body: String,
        type: String = "INFO"
    ) {
        val notifRef = db.collection("notifications")
        try {
            notifRef.add(
                mapOf(
                    "recipientUserId" to recipientUserId,
                    "title" to title,
                    "body" to body,
                    "type" to type,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {}
    }

    fun observeUserNotifications(userId: String): Flow<List<AppNotification>> {
        val query = db.collection("notifications").whereEqualTo("recipientUserId", userId)
        return query.snapshots().map { snapshot ->
            snapshot.toObjects(AppNotification::class.java).sortedByDescending { it.createdAt?.seconds ?: 0L }
        }.catch { emit(emptyList()) }
    }

    suspend fun markNotificationAsRead(notifId: String) {
        try {
            db.collection("notifications").document(notifId).update("isRead", true).await()
        } catch (_: Exception) {}
    }

    /**
     * Bantuan & Tiket Pengaduan (Support Tickets)
     */
    suspend fun createSupportTicket(category: String, message: String, orderId: String = ""): Result<Unit> {
        val uid = requireUserId()
        val ticketRef = db.collection("support_tickets")
        return try {
            ticketRef.add(
                mapOf(
                    "userId" to uid,
                    "orderId" to orderId,
                    "category" to category,
                    "message" to message,
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ticketRef.path)
            Result.failure(e)
        }
    }

    /**
     * Merchant Katalog GoFood
     */
    fun observeMerchants(): Flow<List<Merchant>> {
        return db.collection("merchants").snapshots().map { snapshot ->
            snapshot.toObjects(Merchant::class.java).filter { it.status == "APPROVED" }
        }.catch { emit(emptyList()) }
    }

    suspend fun registerMerchant(name: String, address: String, phone: String, category: String): Result<Unit> {
        val uid = requireUserId()
        val merchantRef = db.collection("merchants")
        return try {
            merchantRef.add(
                mapOf(
                    "ownerUserId" to uid,
                    "name" to name,
                    "address" to address,
                    "phone" to phone,
                    "category" to category,
                    "isOpen" to true,
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, merchantRef.path)
            Result.failure(e)
        }
    }

    /**
     * Rumus Haversine untuk menghitung jarak akurat dalam kilometer antar koordinat GPS
     */
    companion object {
        fun calculateHaversineDistanceKm(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double
        ): Double {
            val r = 6371.0 // Radius bumi dalam KM
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}
