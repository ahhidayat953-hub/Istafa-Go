package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.User
import com.example.data.repository.AdminRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.DriverRepository
import com.example.data.repository.OrderRepository
import com.example.ui.admin.AdminScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.customer.CustomerScreen
import com.example.ui.driver.DriverScreen
import com.example.ui.theme.IstafaGoTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IstafaGoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IstafaGoAppRoot()
                }
            }
        }
    }
}

// Convert FirebaseAuth listener into reactive Flow
fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun IstafaGoAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { Firebase.auth }
    val credentialManager = remember { CredentialManager.create(context) }

    // Dynamic Database ID resolution
    val databaseId = remember {
        val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
        if (resId != 0) context.getString(resId) else "demo-database"
    }

    val firestore = remember {
        FirebaseFirestore.getInstance(databaseId)
    }

    // Repositories
    val authRepository = remember { AuthRepository(firestore, auth) }
    val orderRepository = remember { OrderRepository(firestore) }
    val driverRepository = remember { DriverRepository(firestore) }
    val adminRepository = remember { AdminRepository(firestore) }

    // Observe active auth state
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var currentRoleOverride by remember { mutableStateOf<String?>(null) }
    var isSuperAdminVerified by remember { mutableStateOf(false) }

    val user = currentUser
    if (user == null) {
        // Unauthenticated Gate
        AuthScreen(
            onAuthSuccess = { preferredRole ->
                currentRoleOverride = preferredRole
            }
        )
    } else {
        // Authenticated Session: Load or sync Firestore user profile
        var userProfile by remember { mutableStateOf<User?>(null) }
        var isProfileLoading by remember { mutableStateOf(true) }

        LaunchedEffect(user.uid) {
            isProfileLoading = true
            // Check super admin status
            isSuperAdminVerified = authRepository.isSuperAdmin(user.uid, user.email)

            val syncResult = authRepository.syncUserAccount(
                user = user,
                preferredRole = currentRoleOverride ?: "CUSTOMER"
            )
            userProfile = syncResult.getOrNull()
            isProfileLoading = false
        }

        if (isProfileLoading || userProfile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
            }
        } else {
            val activeProfile = userProfile!!
            val activeRole = currentRoleOverride ?: activeProfile.role

            val handleSignOut: () -> Unit = {
                scope.launch {
                    try {
                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    } catch (_: Exception) {}
                    authRepository.signOut()
                    currentRoleOverride = null
                }
            }

            when {
                activeRole == "ADMIN" && isSuperAdminVerified -> {
                    AdminScreen(
                        user = activeProfile,
                        adminRepository = adminRepository,
                        onSignOut = handleSignOut,
                        onExitAdmin = { currentRoleOverride = "CUSTOMER" }
                    )
                }
                activeRole == "DRIVER" -> {
                    DriverScreen(
                        user = activeProfile,
                        driverRepository = driverRepository,
                        orderRepository = orderRepository,
                        onSignOut = handleSignOut,
                        onSwitchToRole = { targetRole -> currentRoleOverride = targetRole }
                    )
                }
                else -> {
                    CustomerScreen(
                        user = activeProfile,
                        orderRepository = orderRepository,
                        onSignOut = handleSignOut,
                        onSwitchToRole = { targetRole -> currentRoleOverride = targetRole }
                    )
                }
            }
        }
    }
}
