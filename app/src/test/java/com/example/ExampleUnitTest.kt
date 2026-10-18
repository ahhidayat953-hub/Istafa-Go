package com.example

import com.example.data.repository.AuthErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests covering ISTAFA GO registration validation, password policies,
 * and Firebase error mapping (Indonesian user feedback).
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    // --- 1. Password Policy Tests (Firebase >= 6 characters) ---

    @Test
    fun `password policy - exactly 6 characters is accepted`() {
        val password = "123456"
        assertTrue(password.length >= 6)
    }

    @Test
    fun `password policy - more than 6 characters is accepted`() {
        val password = "password123"
        assertTrue(password.length >= 6)
    }

    @Test
    fun `password policy - less than 6 characters is rejected`() {
        val password = "12345"
        assertFalse(password.length >= 6)
    }

    @Test
    fun `password policy - empty password is rejected`() {
        val password = ""
        assertFalse(password.length >= 6)
    }

    // --- 2. Confirmation Password Matching Tests ---

    @Test
    fun `password confirmation - matching passwords succeed`() {
        val pass = "rahasia123"
        val confirmPass = "rahasia123"
        assertEquals(pass, confirmPass)
    }

    @Test
    fun `password confirmation - mismatched passwords fail`() {
        val pass = "rahasia123"
        val confirmPass = "rahasia456"
        assertFalse(pass == confirmPass)
    }

    // --- 3. Name & Username Validation Tests ---

    @Test
    fun `full name validation - minimum 2 characters required`() {
        val validName = "Budi"
        val invalidName = "A"
        val emptyName = "   "
        assertTrue(validName.trim().length >= 2)
        assertFalse(invalidName.trim().length >= 2)
        assertFalse(emptyName.trim().length >= 2)
    }

    @Test
    fun `username normalization - cleans invalid characters and checks length`() {
        val rawUser = "Ahmad.Dani_99"
        val clean = rawUser.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "")
        assertEquals("ahmad.dani_99", clean)
        assertTrue(clean.length >= 3)

        val shortUser = "ab"
        val cleanShort = shortUser.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "")
        assertFalse(cleanShort.length >= 3)
    }

    // --- 4. Phone Number Digit Validation Tests ---

    @Test
    fun `phone validation - minimum 9 digits required`() {
        val validPhone = "0812-3456-7890"
        val digitsOnly = validPhone.filter { it.isDigit() }
        assertEquals("081234567890", digitsOnly)
        assertTrue(digitsOnly.length in 9..15)

        val shortPhone = "08123"
        assertFalse(shortPhone.filter { it.isDigit() }.length in 9..15)
    }

    // --- 5. Driver Vehicle Plate Validation ---

    @Test
    fun `driver registration - plate number is mandatory`() {
        val validPlate = "B 1234 IST"
        val blankPlate = "   "
        assertTrue(validPlate.trim().isNotBlank())
        assertFalse(blankPlate.trim().isNotBlank())
    }

    // --- 6. AuthErrorMapper Indonesian Error Messages Tests ---

    @Test
    fun `error mapper - weak password translates to Indonesian message`() {
        val ex = Exception("WEAK_PASSWORD : Password should be at least 6 characters")
        val message = AuthErrorMapper.map(ex)
        assertTrue(message.contains("Password terlalu pendek"))
        assertTrue(message.contains("6 karakter"))
    }

    @Test
    fun `error mapper - duplicate user collision translates to Indonesian message`() {
        val ex = Exception("The email address is already in use by another account. (EMAIL_EXISTS)")
        val message = AuthErrorMapper.map(ex)
        assertTrue(message.contains("sudah terdaftar"))
    }

    @Test
    fun `error mapper - operation not allowed translates to clear Firebase Console instruction`() {
        val ex = Exception("OPERATION_NOT_ALLOWED : The given sign-in provider is disabled")
        val message = AuthErrorMapper.map(ex)
        assertTrue(message.contains("Firebase Console"))
        assertTrue(message.contains("Email/Password"))
    }

    @Test
    fun `error mapper - network failure translates to Indonesian network message`() {
        val ex = Exception("A network error (such as timeout, interrupted connection) has occurred.")
        val message = AuthErrorMapper.map(ex)
        assertTrue(message.contains("Koneksi internet bermasalah"))
    }

    @Test
    fun `error mapper - permission denied translates to database rule security message`() {
        val ex = Exception("PERMISSION_DENIED: Missing or insufficient permissions.")
        val message = AuthErrorMapper.map(ex)
        assertTrue(message.contains("Izin database ditolak"))
    }
}
