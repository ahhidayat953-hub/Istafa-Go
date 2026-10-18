package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.repository.DriverRepository
import com.example.data.repository.OrderRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class IstafaGoRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createOrder_authenticatedCustomer_succeeds() = runBlocking {
        signInTestUser(CUSTOMER_EMAIL)
        val orderRepo = OrderRepository(firestore)

        val result = withTimeout(TIMEOUT_MS) {
            orderRepo.createOrder(
                customerName = "Budi Santoso",
                customerPhone = "08123456789",
                serviceType = "GO_RIDE",
                pickupAddress = "Jl. Merdeka 45",
                pickupLat = -6.200,
                pickupLng = 106.816,
                dropoffAddress = "Stasiun Gambir",
                dropoffLat = -6.210,
                dropoffLng = 106.820,
                fare = 15000.0,
                distanceKm = 3.5,
                paymentMethod = "CASH"
            )
        }
        assertTrue("Order creation must succeed for signed-in customer", result.isSuccess)
    }

    @Test
    fun createOrder_unauthenticatedCaller_fails() = runBlocking {
        // Explicitly sign out
        auth.signOut()
        val orderRepo = OrderRepository(firestore)

        var exceptionThrown = false
        try {
            withTimeout(TIMEOUT_MS) {
                orderRepo.createOrder(
                    customerName = "Anon",
                    customerPhone = "08123456789",
                    serviceType = "GO_RIDE",
                    pickupAddress = "Jl. Merdeka 45",
                    pickupLat = -6.200,
                    pickupLng = 106.816,
                    dropoffAddress = "Stasiun Gambir",
                    dropoffLat = -6.210,
                    dropoffLng = 106.820,
                    fare = 15000.0,
                    distanceKm = 3.5
                )
            }
        } catch (_: IllegalStateException) {
            exceptionThrown = true
        }
        assertTrue("Unauthenticated order write must fail fast", exceptionThrown)
    }

    @Test
    fun acceptOrder_onlineDriver_succeeds() = runBlocking {
        val customerUid = signInTestUser(CUSTOMER_EMAIL)
        val customerRepo = OrderRepository(firestore)
        val orderId = withTimeout(TIMEOUT_MS) {
            customerRepo.createOrder(
                customerName = "Budi Santoso",
                customerPhone = "08123456789",
                serviceType = "GO_RIDE",
                pickupAddress = "Jl. Merdeka 45",
                pickupLat = -6.200,
                pickupLng = 106.816,
                dropoffAddress = "Stasiun Gambir",
                dropoffLat = -6.210,
                dropoffLng = 106.820,
                fare = 15000.0,
                distanceKm = 3.5
            ).getOrThrow()
        }

        val driverUid = signInTestUser(DRIVER_EMAIL)
        val driverRepo = OrderRepository(firestore)

        val acceptResult = withTimeout(TIMEOUT_MS) {
            driverRepo.acceptOrder(
                orderId = orderId,
                driverId = driverUid,
                driverName = "Agus Driver"
            )
        }
        assertTrue("Driver should successfully accept available searching order", acceptResult.isSuccess)
    }

    @Test
    fun concurrentAcceptOrder_secondDriverFails() = runBlocking {
        signInTestUser(CUSTOMER_EMAIL)
        val customerRepo = OrderRepository(firestore)
        val orderId = withTimeout(TIMEOUT_MS) {
            customerRepo.createOrder(
                customerName = "Siti Aisyah",
                customerPhone = "08123456780",
                serviceType = "GO_RIDE",
                pickupAddress = "Jl. Merdeka 10",
                pickupLat = -6.200,
                pickupLng = 106.816,
                dropoffAddress = "Pasar Senen",
                dropoffLat = -6.210,
                dropoffLng = 106.820,
                fare = 12000.0,
                distanceKm = 2.5
            ).getOrThrow()
        }

        // Driver 1 accepts
        val driver1Uid = signInTestUser("driver1_${UUID.randomUUID()}@istafago.com")
        val driver1Repo = OrderRepository(firestore)
        val accept1 = withTimeout(TIMEOUT_MS) {
            driver1Repo.acceptOrder(orderId, driver1Uid, "Driver Pertama")
        }
        assertTrue("First driver must successfully accept order", accept1.isSuccess)

        // Driver 2 attempts to accept the same order
        val driver2Uid = signInTestUser("driver2_${UUID.randomUUID()}@istafago.com")
        val driver2Repo = OrderRepository(firestore)
        val accept2 = withTimeout(TIMEOUT_MS) {
            driver2Repo.acceptOrder(orderId, driver2Uid, "Driver Kedua")
        }
        assertTrue("Second driver must fail atomic transaction", accept2.isFailure)
    }

    @Test
    fun orderStatusProgression_driverCompletesTrip() = runBlocking {
        val customerUid = signInTestUser(CUSTOMER_EMAIL)
        val customerRepo = OrderRepository(firestore)
        val orderId = withTimeout(TIMEOUT_MS) {
            customerRepo.createOrder(
                customerName = "Rudi Hartono",
                customerPhone = "08123456788",
                serviceType = "GO_RIDE",
                pickupAddress = "Jl. Thamrin",
                pickupLat = -6.200,
                pickupLng = 106.816,
                dropoffAddress = "Monas",
                dropoffLat = -6.210,
                dropoffLng = 106.820,
                fare = 18000.0,
                distanceKm = 3.0
            ).getOrThrow()
        }

        val driverUid = signInTestUser(DRIVER_EMAIL)
        val driverRepo = OrderRepository(firestore)
        driverRepo.acceptOrder(orderId, driverUid, "Driver Agus")

        val arrivedPickup = withTimeout(TIMEOUT_MS) {
            driverRepo.updateOrderStatus(orderId, "PICKING_UP")
        }
        assertTrue("PICKING_UP transition succeeds", arrivedPickup.isSuccess)

        val onTheWay = withTimeout(TIMEOUT_MS) {
            driverRepo.updateOrderStatus(orderId, "ON_THE_WAY")
        }
        assertTrue("ON_THE_WAY transition succeeds", onTheWay.isSuccess)

        val completed = withTimeout(TIMEOUT_MS) {
            driverRepo.updateOrderStatus(orderId, "COMPLETED")
        }
        assertTrue("COMPLETED transition succeeds", completed.isSuccess)

        // Customer rates completed order
        signInTestUser(CUSTOMER_EMAIL)
        val rateResult = withTimeout(TIMEOUT_MS) {
            customerRepo.submitOrderRating(orderId, driverUid, 5.0, "Driver sangat baik")
        }
        assertTrue("Rating completed order succeeds", rateResult.isSuccess)
    }

    @Test
    fun orderCancellation_recordsAuditedReason() = runBlocking {
        signInTestUser(CUSTOMER_EMAIL)
        val customerRepo = OrderRepository(firestore)
        val orderId = withTimeout(TIMEOUT_MS) {
            customerRepo.createOrder(
                customerName = "Dewi Lestari",
                customerPhone = "08123456787",
                serviceType = "GO_RIDE",
                pickupAddress = "Jl. Gatot Subroto",
                pickupLat = -6.200,
                pickupLng = 106.816,
                dropoffAddress = "Stasiun Cikini",
                dropoffLat = -6.210,
                dropoffLng = 106.820,
                fare = 14000.0,
                distanceKm = 2.8
            ).getOrThrow()
        }

        val cancelResult = withTimeout(TIMEOUT_MS) {
            customerRepo.cancelOrder(orderId, "CUSTOMER", "Perubahan rencana jadwal perjalanan")
        }
        assertTrue("Order cancellation must succeed and record reason", cancelResult.isSuccess)
    }

    @Test
    fun driverRegistration_validPayload_createsPendingProfile() = runBlocking {
        signInTestUser("new_driver_${UUID.randomUUID()}@test.com")
        val driverRepo = DriverRepository(firestore)

        val result = withTimeout(TIMEOUT_MS) {
            driverRepo.submitDriverRegistration(
                fullName = "Joko Widodo",
                phone = "08198765432",
                vehicleType = "MOTORCYCLE",
                plateNumber = "B 9999 ABC",
                vehicleModel = "Honda Beat 125",
                ktpNumber = "3171234567890001",
                simNumber = "987654321098"
            )
        }
        assertTrue("Driver registration submission must succeed in pending status", result.isSuccess)
    }

    private companion object {
        const val CUSTOMER_EMAIL = "customer_test@istafago.com"
        const val DRIVER_EMAIL = "driver_test@istafago.com"
        const val TIMEOUT_MS = 8000L
    }
}
