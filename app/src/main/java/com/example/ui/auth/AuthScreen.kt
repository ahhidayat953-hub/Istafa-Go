package com.example.ui.auth

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.AuthRepository
import com.example.ui.common.ErrorBanner
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BlueDark
import com.example.ui.theme.BluePrimary
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    authRepository: AuthRepository? = null,
    onAuthSuccess: (preferredRole: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember(authRepository) { authRepository ?: AuthRepository(context) }

    // Navigation Tab within Public view: "LOGIN" or "REGISTER"
    var activeTab by remember { mutableStateOf("LOGIN") }

    // Public Login Form State
    var selectedRole by remember { mutableStateOf("CUSTOMER") } // CUSTOMER, DRIVER (Admin is strictly hidden)
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Public Registration Form State
    var regRole by remember { mutableStateOf("CUSTOMER") } // CUSTOMER, DRIVER
    var regFullName by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regVehicleType by remember { mutableStateOf("MOTORCYCLE") }
    var regPlateNumber by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var isRegPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Secret Stealth Super Admin Portal State (Only accessible via 5 rapid taps on logo or title)
    var secretTapCount by remember { mutableIntStateOf(0) }
    var isSuperAdminPortalOpen by remember { mutableStateOf(false) }
    var adminLoginUsername by remember { mutableStateOf("") }
    var adminLoginPassword by remember { mutableStateOf("") }
    var isAdminPasswordVisible by remember { mutableStateOf(false) }
    var isAdminLoading by remember { mutableStateOf(false) }
    var adminErrorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = isSuperAdminPortalOpen || activeTab == "REGISTER") {
        if (isSuperAdminPortalOpen) {
            isSuperAdminPortalOpen = false
            adminErrorMessage = null
        } else if (activeTab == "REGISTER") {
            activeTab = "LOGIN"
            errorMessage = null
            successMessage = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BlueDark,
                        BluePrimary,
                        MaterialTheme.colorScheme.background
                    ),
                    startY = 0f,
                    endY = 850f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Logo & Header (5 taps activates stealth Super Admin Portal)
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        secretTapCount++
                        if (secretTapCount >= 5) {
                            secretTapCount = 0
                            isSuperAdminPortalOpen = true
                            adminErrorMessage = null
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_istafago_logo),
                    contentDescription = "Logo ISTAFA GO",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Discreet Title (5 taps also activates stealth Super Admin Portal without hints)
            Text(
                text = "ISTAFA GO",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .testTag("app_title_text")
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        secretTapCount++
                        if (secretTapCount >= 5) {
                            secretTapCount = 0
                            isSuperAdminPortalOpen = true
                            adminErrorMessage = null
                        }
                    }
            )

            Text(
                text = "Transportasi & Pengantaran Lokal Handal",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // CONDITIONAL: STEALTH SUPER ADMIN PORTAL
            // ==========================================
            if (isSuperAdminPortalOpen) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("super_admin_portal_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = BlueDark,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "PORTAL SUPER ADMIN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Halaman otentikasi administrator terproteksi ISTAFA GO",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        if (adminErrorMessage != null) {
                            ErrorBanner(
                                message = adminErrorMessage!!,
                                onRetry = { adminErrorMessage = null }
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Username Admin Field
                        OutlinedTextField(
                            value = adminLoginUsername,
                            onValueChange = { adminLoginUsername = it },
                            label = { Text("Username / Email Admin") },
                            placeholder = { Text("admin@istafago.id") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = BlueDark
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_username_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BlueDark,
                                focusedLabelColor = BlueDark
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password Admin Field
                        OutlinedTextField(
                            value = adminLoginPassword,
                            onValueChange = { adminLoginPassword = it },
                            label = { Text("Password Admin") },
                            placeholder = { Text("Masukkan kata sandi admin") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = BlueDark
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { isAdminPasswordVisible = !isAdminPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isAdminPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isAdminPasswordVisible) "Sembunyikan password" else "Tampilkan password"
                                    )
                                }
                            },
                            visualTransformation = if (isAdminPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_password_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BlueDark,
                                focusedLabelColor = BlueDark
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Submit Admin Login Button
                        Button(
                            onClick = {
                                if (isAdminLoading) return@Button
                                if (adminLoginUsername.isBlank()) {
                                    adminErrorMessage = "Username admin wajib diisi"
                                    return@Button
                                }
                                if (adminLoginPassword.isBlank()) {
                                    adminErrorMessage = "Password admin wajib diisi"
                                    return@Button
                                }

                                isAdminLoading = true
                                adminErrorMessage = null
                                scope.launch {
                                    val res = repo.signInWithUsername(adminLoginUsername, adminLoginPassword)
                                    if (res.isSuccess) {
                                        val fbUser = res.getOrThrow()
                                        // Strictly verify admin claim in Firestore database
                                        val isRealAdmin = repo.isSuperAdmin(fbUser.uid, fbUser.email)
                                        isAdminLoading = false
                                        if (isRealAdmin) {
                                            onAuthSuccess("ADMIN")
                                        } else {
                                            // Immediately revoke session
                                            repo.signOut()
                                            adminErrorMessage = "Akses ditolak: Akun '${adminLoginUsername}' tidak memiliki hak akses Super Admin yang sah di sistem."
                                        }
                                    } else {
                                        isAdminLoading = false
                                        adminErrorMessage = res.exceptionOrNull()?.localizedMessage ?: "Autentikasi admin gagal."
                                    }
                                }
                            },
                            enabled = !isAdminLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = BlueDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("admin_login_submit_button")
                        ) {
                            if (isAdminLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Masuk sebagai Super Admin",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        TextButton(
                            onClick = {
                                isSuperAdminPortalOpen = false
                                adminErrorMessage = null
                            },
                            modifier = Modifier.testTag("admin_back_to_public_button")
                        ) {
                            Text(
                                text = "← Kembali ke Login Publik",
                                fontSize = 13.sp,
                                color = BluePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                // ==========================================
                // MAIN PUBLIC AUTH CARD (Customer & Driver)
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (errorMessage != null) {
                            ErrorBanner(
                                message = errorMessage!!,
                                onRetry = { errorMessage = null }
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        if (successMessage != null) {
                            Surface(
                                color = AccentGreen.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = successMessage!!,
                                    color = AccentGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // ------------------------------------------
                        // VIEW: PUBLIC LOGIN
                        // ------------------------------------------
                        if (activeTab == "LOGIN") {
                            Text(
                                text = "Masuk dengan Username & Password",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Silakan masukkan username dan password akun Anda",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Role Selector Bar (Publicly only Pelanggan and Mitra Driver)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Pelanggan Chip
                                val isCust = selectedRole == "CUSTOMER"
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCust) BluePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isCust) androidx.compose.foundation.BorderStroke(1.5.dp, BluePrimary) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedRole = "CUSTOMER" }
                                        .testTag("role_customer_chip")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isCust) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Pelanggan",
                                            fontSize = 13.sp,
                                            fontWeight = if (isCust) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCust) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Mitra Driver Chip
                                val isDriver = selectedRole == "DRIVER"
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDriver) BluePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isDriver) androidx.compose.foundation.BorderStroke(1.5.dp, BluePrimary) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedRole = "DRIVER" }
                                        .testTag("role_driver_chip")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TwoWheeler,
                                            contentDescription = null,
                                            tint = if (isDriver) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Mitra Driver",
                                            fontSize = 13.sp,
                                            fontWeight = if (isDriver) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isDriver) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Username Field
                            OutlinedTextField(
                                value = loginUsername,
                                onValueChange = { loginUsername = it },
                                label = { Text("Username") },
                                placeholder = { Text("Masukkan username Anda") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = BluePrimary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("username_input_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BluePrimary,
                                    focusedLabelColor = BluePrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Password Field
                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                label = { Text("Password") },
                                placeholder = { Text("Masukkan password") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = BluePrimary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isPasswordVisible) "Sembunyikan password" else "Tampilkan password"
                                        )
                                    }
                                },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("password_input_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BluePrimary,
                                    focusedLabelColor = BluePrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Submit Login Button
                            Button(
                                onClick = {
                                    if (isLoading) return@Button
                                    if (loginUsername.isBlank()) {
                                        errorMessage = "Masukkan username Anda"
                                        return@Button
                                    }
                                    if (loginPassword.isBlank()) {
                                        errorMessage = "Masukkan password Anda"
                                        return@Button
                                    }

                                    isLoading = true
                                    errorMessage = null
                                    scope.launch {
                                        val loginRes = repo.signInWithUsername(loginUsername, loginPassword)
                                        isLoading = false
                                        loginRes.onSuccess { user ->
                                            val isAdmin = repo.isSuperAdmin(user.uid, user.email)
                                            val targetRole = if (isAdmin) "ADMIN" else selectedRole
                                            onAuthSuccess(targetRole)
                                        }.onFailure { err ->
                                            errorMessage = err.localizedMessage ?: "Gagal masuk. Periksa kembali username & password."
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("login_submit_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Masuk Sekarang",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            TextButton(
                                onClick = { activeTab = "REGISTER" },
                                modifier = Modifier.testTag("switch_to_register_button")
                            ) {
                                Text(
                                    text = "Belum punya akun? Daftar Baru di sini",
                                    fontSize = 13.sp,
                                    color = BluePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // ------------------------------------------
                        // VIEW: REGISTRATION
                        // ------------------------------------------
                        if (activeTab == "REGISTER") {
                            Text(
                                text = "Daftar Akun ISTAFA GO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lengkapi data untuk mendaftar akun baru",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Role Selector for Register
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isCust = regRole == "CUSTOMER"
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCust) BluePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isCust) androidx.compose.foundation.BorderStroke(1.5.dp, BluePrimary) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { regRole = "CUSTOMER" }
                                        .testTag("reg_role_customer")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isCust) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Pelanggan",
                                            fontSize = 13.sp,
                                            fontWeight = if (isCust) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCust) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                val isDriver = regRole == "DRIVER"
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDriver) BluePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isDriver) androidx.compose.foundation.BorderStroke(1.5.dp, BluePrimary) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { regRole = "DRIVER" }
                                        .testTag("reg_role_driver")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TwoWheeler,
                                            contentDescription = null,
                                            tint = if (isDriver) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Mitra Driver",
                                            fontSize = 13.sp,
                                            fontWeight = if (isDriver) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isDriver) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Full Name
                            OutlinedTextField(
                                value = regFullName,
                                onValueChange = { regFullName = it },
                                label = { Text("Nama Lengkap") },
                                placeholder = { Text("Contoh: Ahmad Hidayat") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = BluePrimary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_name_field")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Username
                            OutlinedTextField(
                                value = regUsername,
                                onValueChange = { regUsername = it },
                                label = { Text("Username") },
                                placeholder = { Text("Contoh: ahmad_hidayat") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = BluePrimary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_username_field")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Phone
                            OutlinedTextField(
                                value = regPhone,
                                onValueChange = { regPhone = it },
                                label = { Text("Nomor WhatsApp / HP") },
                                placeholder = { Text("Contoh: 08123456789") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = BluePrimary)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_phone_field")
                            )

                            // Extra Driver Fields
                            if (regRole == "DRIVER") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isMotor = regVehicleType == "MOTORCYCLE"
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isMotor) BluePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isMotor) androidx.compose.foundation.BorderStroke(1.dp, BluePrimary) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { regVehicleType = "MOTORCYCLE" }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Motor", fontSize = 12.sp)
                                        }
                                    }

                                    val isCar = regVehicleType == "CAR"
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCar) BluePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isCar) androidx.compose.foundation.BorderStroke(1.dp, BluePrimary) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { regVehicleType = "CAR" }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Mobil", fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = regPlateNumber,
                                    onValueChange = { regPlateNumber = it.uppercase() },
                                    label = { Text("Nomor Plat Kendaraan") },
                                    placeholder = { Text("Contoh: B 1234 ABC") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_plate_field")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Password Field
                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it },
                                label = { Text("Password (min. 6 karakter)") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = BluePrimary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isRegPasswordVisible = !isRegPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isRegPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isRegPasswordVisible) "Sembunyikan password" else "Tampilkan password"
                                        )
                                    }
                                },
                                visualTransformation = if (isRegPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_password_field")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Confirm Password Field
                            OutlinedTextField(
                                value = regConfirmPassword,
                                onValueChange = { regConfirmPassword = it },
                                label = { Text("Konfirmasi Password") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = BluePrimary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isRegPasswordVisible = !isRegPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isRegPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isRegPasswordVisible) "Sembunyikan password" else "Tampilkan password"
                                        )
                                    }
                                },
                                visualTransformation = if (isRegPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_confirm_password_field")
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Submit Register Button
                            Button(
                                onClick = {
                                    if (isLoading) return@Button

                                    val trimmedName = regFullName.trim()
                                    if (trimmedName.length < 2) {
                                        errorMessage = "Nama lengkap minimal 2 karakter"
                                        return@Button
                                    }
                                    val cleanUser = regUsername.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "")
                                    if (cleanUser.length < 3) {
                                        errorMessage = "Username minimal 3 karakter (huruf, angka, titik, atau garis bawah)"
                                        return@Button
                                    }
                                    val cleanPhone = regPhone.trim().filter { it.isDigit() }
                                    if (cleanPhone.length < 9) {
                                        errorMessage = "Nomor WhatsApp/HP minimal 9 digit angka"
                                        return@Button
                                    }
                                    if (regPassword.length < 6) {
                                        errorMessage = "Password minimal 6 karakter sesuai kebijakan Firebase Authentication"
                                        return@Button
                                    }
                                    if (regPassword != regConfirmPassword) {
                                        errorMessage = "Konfirmasi password tidak cocok dengan password yang dimasukkan"
                                        return@Button
                                    }
                                    if (regRole == "DRIVER" && regPlateNumber.trim().isBlank()) {
                                        errorMessage = "Nomor plat kendaraan wajib diisi untuk mitra driver"
                                        return@Button
                                    }

                                    isLoading = true
                                    errorMessage = null
                                    scope.launch {
                                        val regRes = repo.signUpWithUsername(
                                            username = regUsername,
                                            password = regPassword,
                                            name = regFullName,
                                            phone = regPhone,
                                            role = regRole,
                                            vehicleType = regVehicleType,
                                            plateNumber = regPlateNumber
                                        )
                                        isLoading = false
                                        regRes.onSuccess {
                                            onAuthSuccess(regRole)
                                        }.onFailure { err ->
                                            errorMessage = err.localizedMessage ?: "Pendaftaran gagal"
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("register_submit_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Daftar Akun Baru",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = { activeTab = "LOGIN" },
                                modifier = Modifier.testTag("switch_to_login_button")
                            ) {
                                Text(
                                    text = "Sudah punya akun? Masuk di sini",
                                    fontSize = 13.sp,
                                    color = BluePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Dengan masuk atau mendaftar, Anda menyetujui Ketentuan Layanan & Privasi ISTAFA GO.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Clean Footer
            Text(
                text = "ISTAFA GO v1.0 • Transportasi Lokal Handal",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
