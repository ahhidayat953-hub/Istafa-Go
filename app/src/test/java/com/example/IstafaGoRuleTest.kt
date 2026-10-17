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
        const val TIMEOUT_MS = 5000L
    }
}
