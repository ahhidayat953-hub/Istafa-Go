package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class User(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "CUSTOMER", // CUSTOMER, DRIVER, ADMIN
    val status: String = "ACTIVE", // ACTIVE, SUSPENDED
    val profilePhotoUrl: String = "",
    val fcmToken: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val email: String = "",
    val role: String = "SUPER_ADMIN",
    @ServerTimestamp val grantedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DriverProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val vehicleType: String = "MOTORCYCLE", // MOTORCYCLE, CAR, VAN
    val plateNumber: String = "",
    val vehicleModel: String = "",
    val status: String = "PENDING_VERIFICATION", // PENDING_VERIFICATION, APPROVED, REJECTED, SUSPENDED
    val isOnline: Boolean = false,
    val currentLat: Double = -6.200000,
    val currentLng: Double = 106.816666,
    val rating: Double = 5.0,
    val totalTrips: Long = 0L,
    val walletBalance: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Vehicle(
    @DocumentId val id: String = "",
    val driverId: String = "",
    val type: String = "MOTORCYCLE",
    val model: String = "",
    val plateNumber: String = "",
    val color: String = "",
    val year: String = ""
)

@IgnoreExtraProperties
data class ServiceArea(
    @DocumentId val id: String = "",
    val name: String = "",
    val city: String = "",
    val isActive: Boolean = true,
    val baseFareBike: Double = 10000.0,
    val baseFareCar: Double = 20000.0
)

@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "", // Private to assigned driver
    val driverId: String = "",
    val driverName: String = "",
    val driverPhone: String = "", // Private to customer
    val driverPlateNumber: String = "",
    val serviceType: String = "GO_RIDE", // GO_RIDE, GO_CAR, GO_SEND, GO_FOOD
    val status: String = "SEARCHING", // SEARCHING, ACCEPTED, PICKING_UP, ARRIVED, ON_THE_WAY, COMPLETED, CANCELLED
    val pickupAddress: String = "",
    val pickupLat: Double = -6.200000,
    val pickupLng: Double = 106.816666,
    val dropoffAddress: String = "",
    val dropoffLat: Double = -6.210000,
    val dropoffLng: Double = 106.820000,
    val fare: Double = 12000.0,
    val distanceKm: Double = 3.2,
    val paymentMethod: String = "CASH", // CASH, WALLET
    val paymentStatus: String = "PENDING", // PENDING, PAID
    // GoSend (Barang) details
    val itemType: String = "",
    val itemDescription: String = "",
    val recipientName: String = "",
    val recipientPhone: String = "",
    // GoFood (Makanan) details
    val merchantId: String = "",
    val merchantName: String = "",
    val foodItemsSummary: String = "",
    val tripNotes: String = "",
    // Rating & Review
    val rating: Double = 0.0,
    val review: String = "",
    // Cancellation audit
    val cancelledBy: String = "", // CUSTOMER or DRIVER
    val cancelReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FoodMenuItem(
    @DocumentId val id: String = "",
    val merchantId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 15000.0,
    val category: String = "Makanan",
    val isAvailable: Boolean = true
)

@IgnoreExtraProperties
data class Merchant(
    @DocumentId val id: String = "",
    val ownerUserId: String = "",
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val category: String = "Makanan Nusantara",
    val isOpen: Boolean = true,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PricingSetting(
    @DocumentId val id: String = "",
    val serviceType: String = "GO_RIDE",
    val baseFare: Double = 9000.0,
    val perKmRate: Double = 2500.0,
    val minimumFare: Double = 10000.0,
    val platformFeePercent: Double = 15.0,
    val isActive: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TransactionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val type: String = "FARE_PAYMENT", // TOPUP, FARE_PAYMENT, COMMISSION_DEDUCTION, WITHDRAWAL
    val amount: Double = 0.0,
    val status: String = "SUCCESS", // SUCCESS, PENDING, FAILED
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CommissionLedger(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val driverId: String = "",
    val grossFare: Double = 0.0,
    val platformCommission: Double = 0.0,
    val driverNet: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DriverSubmission(
    @DocumentId val id: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val phone: String = "",
    val ktpNumber: String = "",
    val simNumber: String = "",
    val vehicleType: String = "MOTORCYCLE",
    val plateNumber: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val rejectionReason: String = "",
    @ServerTimestamp val submittedAt: Timestamp? = null,
    @ServerTimestamp val reviewedAt: Timestamp? = null,
    val reviewedBy: String = ""
)

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientUserId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "ORDER_UPDATE",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val category: String = "TRIP_ISSUE",
    val message: String = "",
    val status: String = "OPEN", // OPEN, IN_PROGRESS, RESOLVED
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppSetting(
    @DocumentId val id: String = "",
    val maintenanceMode: Boolean = false,
    val minDriverAppVersion: Long = 1L,
    val emergencyContact: String = "112",
    val contactPhone: String = "081234567890"
)

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val action: String = "",
    val targetResource: String = "",
    val targetId: String = "",
    val details: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)
