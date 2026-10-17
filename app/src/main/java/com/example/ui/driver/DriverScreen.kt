package com.example.ui.driver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import com.example.ui.common.RouteMapPreview
import kotlinx.coroutines.delay
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Order
import com.example.data.model.User
import com.example.data.repository.DriverRepository
import com.example.data.repository.OrderRepository
import com.example.ui.common.AppConfirmationDialog
import com.example.ui.common.EmptyStateCard
import com.example.ui.common.ErrorBanner
import com.example.ui.common.ServiceBadge
import com.example.ui.common.StatusBadge
import com.example.ui.common.formatRupiah
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BlueDark
import com.example.ui.theme.BluePrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScreen(
    user: User,
    driverRepository: DriverRepository,
    orderRepository: OrderRepository,
    onSignOut: () -> Unit,
    onSwitchToRole: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Realtime driver profile & submission
    val driverProfile by driverRepository.observeDriverProfile(user.userId)
        .collectAsStateWithLifecycle(initialValue = null)

    val driverSubmission by driverRepository.observeDriverSubmission(user.userId)
        .collectAsStateWithLifecycle(initialValue = null)

    // Searching orders radar (only active when online)
    val availableOrders by orderRepository.observeSearchingOrders()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Active order currently taken by this driver
    val activeDriverOrders by orderRepository.observeDriverActiveOrders(user.userId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val ongoingOrder = activeDriverOrders.firstOrNull()

    val pastOrders by orderRepository.observeDriverPastOrders(user.userId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val context = LocalContext.current
    var selectedDriverTab by remember { mutableIntStateOf(0) }
    var rejectedOrderIds by remember { mutableStateOf(setOf<String>()) }

    // Registration Form State
    var regFullName by remember { mutableStateOf(user.name) }
    var regPhone by remember { mutableStateOf(user.phone) }
    var regVehicleType by remember { mutableStateOf("MOTORCYCLE") }
    var regPlateNumber by remember { mutableStateOf("") }
    var regVehicleModel by remember { mutableStateOf("") }
    var regKtpNumber by remember { mutableStateOf("") }
    var regSimNumber by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var showAcceptDialog by remember { mutableStateOf<Order?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ISTAFA GO DRIVER",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Mitra: ${user.name.ifBlank { "Driver" }}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                actions = {
                    if (onSwitchToRole != null) {
                        TextButton(
                            onClick = { onSwitchToRole("CUSTOMER") },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text("Mode Pelanggan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Keluar Akun",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isApproved = driverProfile?.status == "APPROVED"
            val isPending = driverProfile?.status == "PENDING_VERIFICATION" || (driverSubmission != null && driverSubmission?.status == "PENDING")
            val isRejected = driverProfile?.status == "REJECTED" || driverSubmission?.status == "REJECTED"

            if (!isApproved) {
                // Verification or Application Flow
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isPending) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("pending_verification_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = AccentAmber.copy(alpha = 0.12f))
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = AccentAmber,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Pendaftaran Sedang Ditinjau",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF856404)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Dokumen pendaftaran Anda (KTP, SIM, Kendaraan) telah diterima dan sedang diperiksa oleh Super Admin ISTAFA GO. Anda baru dapat online dan menerima order setelah verifikasi disetujui.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF856404)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "Nama: ${driverSubmission?.driverName ?: regFullName}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Kendaraan: ${driverSubmission?.vehicleType ?: regVehicleType} • ${driverSubmission?.plateNumber ?: regPlateNumber}",
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "Status: Menunggu Persetujuan Admin",
                                                fontSize = 12.sp,
                                                color = AccentAmber,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (isRejected) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.12f))
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = AccentRed,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Pendaftaran Driver Ditolak",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = AccentRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Alasan penolakan: ${driverSubmission?.rejectionReason?.ifBlank { "Dokumen tidak memenuhi persyaratan verifikasi." } ?: "Dokumen tidak memenuhi persyaratan verifikasi."}",
                                        fontSize = 13.sp,
                                        color = AccentRed
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Silakan perbaiki data di bawah dan ajukan kembali.",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Registration Form
                    if (!isPending) {
                        item {
                            DriverRegistrationCard(
                                fullName = regFullName,
                                onFullNameChange = { regFullName = it },
                                phone = regPhone,
                                onPhoneChange = { regPhone = it },
                                vehicleType = regVehicleType,
                                onVehicleTypeChange = { regVehicleType = it },
                                plateNumber = regPlateNumber,
                                onPlateNumberChange = { regPlateNumber = it },
                                vehicleModel = regVehicleModel,
                                onVehicleModelChange = { regVehicleModel = it },
                                ktpNumber = regKtpNumber,
                                onKtpNumberChange = { regKtpNumber = it },
                                simNumber = regSimNumber,
                                onSimNumberChange = { regSimNumber = it },
                                isSubmitting = isSubmitting,
                                onSubmit = {
                                    if (regFullName.isBlank() || regPhone.isBlank() || regPlateNumber.isBlank() || regKtpNumber.isBlank() || regSimNumber.isBlank()) {
                                        actionError = "Semua kolom pendaftaran dan dokumen wajib diisi."
                                    } else {
                                        isSubmitting = true
                                        actionError = null
                                        scope.launch {
                                            val result = driverRepository.submitDriverRegistration(
                                                fullName = regFullName,
                                                phone = regPhone,
                                                vehicleType = regVehicleType,
                                                plateNumber = regPlateNumber,
                                                vehicleModel = regVehicleModel,
                                                ktpNumber = regKtpNumber,
                                                simNumber = regSimNumber
                                            )
                                            isSubmitting = false
                                            if (result.isFailure) {
                                                actionError = result.exceptionOrNull()?.localizedMessage ?: "Gagal mengajukan pendaftaran"
                                            }
                                        }
                                    }
                                },
                                errorMessage = actionError
                            )
                        }
                    }
                }
            } else {
                // APPROVED DRIVER DASHBOARD
                val profile = driverProfile!!

                TabRow(
                    selectedTabIndex = selectedDriverTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = BlueDark
                ) {
                    Tab(
                        selected = selectedDriverTab == 0,
                        onClick = { selectedDriverTab = 0 },
                        text = { Text("Radar & Order", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedDriverTab == 1,
                        onClick = { selectedDriverTab = 1 },
                        text = { Text("Riwayat (${pastOrders.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedDriverTab == 2,
                        onClick = { selectedDriverTab = 2 },
                        text = { Text("Profil Mitra", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                when (selectedDriverTab) {
                    0 -> {
                        // TAB 0: Operasional, Radar & Order Aktif
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Online / Offline Duty Switch Header Card
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("driver_status_card"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (profile.isOnline) BluePrimary else MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (profile.isOnline) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PowerSettingsNew,
                                                    contentDescription = null,
                                                    tint = if (profile.isOnline) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = if (profile.isOnline) "Driver Online (Aktif)" else "Driver Offline",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = if (profile.isOnline) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (profile.isOnline) "Siap menerima pesanan masuk" else "Nyalakan untuk terima order",
                                                    fontSize = 12.sp,
                                                    color = if (profile.isOnline) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = profile.isOnline,
                                            onCheckedChange = { newStatus ->
                                                scope.launch {
                                                    driverRepository.toggleOnlineStatus(user.userId, newStatus)
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = AccentGreen
                                            ),
                                            modifier = Modifier.testTag("driver_online_toggle")
                                        )
                                    }
                                }
                            }

                            // GPS Broadcast Card
                            if (profile.isOnline) {
                                item {
                                    Surface(
                                        color = AccentGreen.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("GPS Driver Aktif & Terverifikasi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text("Koordinat: (%.4f, %.4f)".format(profile.currentLat, profile.currentLng), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            TextButton(onClick = {
                                                scope.launch {
                                                    driverRepository.updateLocation(user.userId, profile.currentLat + 0.0003, profile.currentLng + 0.0003)
                                                    Toast.makeText(context, "Posisi GPS terkirim ke server!", Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Text("Update GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Earnings & Performance Summary Card
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        PerformanceItem(
                                            icon = Icons.Default.AccountBalanceWallet,
                                            label = "Saldo Dompet",
                                            value = formatRupiah(profile.walletBalance),
                                            color = BluePrimary
                                        )
                                        PerformanceItem(
                                            icon = Icons.Default.TwoWheeler,
                                            label = "Total Tarikan",
                                            value = "${profile.totalTrips} Trip",
                                            color = AccentCyan
                                        )
                                        PerformanceItem(
                                            icon = Icons.Default.Star,
                                            label = "Rating",
                                            value = "%.1f ★".format(profile.rating),
                                            color = AccentAmber
                                        )
                                    }
                                }
                            }

                            // Active Ongoing Order Execution Card
                            if (ongoingOrder != null) {
                                item {
                                    DriverActiveOrderCard(
                                        order = ongoingOrder,
                                        onUpdateStatus = { nextStatus ->
                                            scope.launch {
                                                orderRepository.updateOrderStatus(ongoingOrder.id, nextStatus)
                                            }
                                        },
                                        onWhatsAppClick = { phone ->
                                            openDriverWhatsApp(context, phone, "Halo pemesan ISTAFA GO, saya mitra driver perjalanan Anda.")
                                        }
                                    )
                                }
                            }

                            // Incoming Orders Radar
                            val validIncomingOrders = availableOrders.filter { it.id !in rejectedOrderIds }
                            item {
                                Text(
                                    text = "Radar Pesanan Masuk (${validIncomingOrders.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!profile.isOnline) {
                                item {
                                    EmptyStateCard(
                                        title = "Status Sedang Offline",
                                        message = "Aktifkan sakelar Online di atas untuk mendeteksi pesanan yang butuh driver di sekitar Anda."
                                    )
                                }
                            } else if (ongoingOrder != null) {
                                item {
                                    EmptyStateCard(
                                        title = "Sedang Melayani Pesanan",
                                        message = "Selesaikan pesanan yang sedang berjalan terlebih dahulu sebelum menerima pesanan baru."
                                    )
                                }
                            } else if (validIncomingOrders.isEmpty()) {
                                item {
                                    EmptyStateCard(
                                        title = "Menunggu Pesanan...",
                                        message = "Belum ada pesanan baru di sekitar lokasi Anda. Tetap aktif, pesanan akan muncul secara otomatis."
                                    )
                                }
                            } else {
                                items(validIncomingOrders, key = { it.id }) { order ->
                                    IncomingOrderCard(
                                        order = order,
                                        onAccept = { showAcceptDialog = order },
                                        onReject = {
                                            rejectedOrderIds = rejectedOrderIds + order.id
                                            Toast.makeText(context, "Pesanan dilewati.", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: Riwayat & Dompet
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = BlueDark),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Text(text = "Saldo Dompet Mitra Driver", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatRupiah(profile.walletBalance),
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 24.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            color = Color.White.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Bagi Hasil Mitra", color = Color.White, fontSize = 12.sp)
                                                Text("85% Bersih Mitra (15% Komisi)", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "Riwayat Tarikan Selesai",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (pastOrders.isEmpty()) {
                                item {
                                    EmptyStateCard(
                                        title = "Belum Ada Riwayat Tarikan",
                                        message = "Pesanan yang telah Anda selesaikan atau batalkan akan tercatat di sini."
                                    )
                                }
                            } else {
                                items(pastOrders, key = { it.id }) { order ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                ServiceBadge(serviceType = order.serviceType)
                                                StatusBadge(status = order.status)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = "Tujuan: ${order.dropoffAddress}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(text = "Jemput: ${order.pickupAddress}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Tarif: ${formatRupiah(order.fare)} • Bersih (85%): ${formatRupiah(order.fare * 0.85)}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AccentGreen
                                                )
                                                if (order.rating > 0.0) {
                                                    Text(
                                                        text = "%.1f ★".format(order.rating),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AccentAmber
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 2: Profil & Kendaraan
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .background(BlueDark),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Text(text = profile.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Text(text = profile.phone, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                StatusBadge(status = profile.status)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Text(text = "Informasi Kendaraan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(text = "Tipe: ${if (profile.vehicleType == "CAR") "Mobil (GoCar)" else "Sepeda Motor (GoRide/GoSend)"}", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Merk & Model: ${profile.vehicleModel.ifBlank { "Honda Vario" }}", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Nomor Plat: ${profile.plateNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BluePrimary)
                                    }
                                }
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Text(text = "Status Dokumen & Verifikasi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("SIM Pengemudi: Terverifikasi Aktif", fontSize = 13.sp)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("NIK / KTP: Terverifikasi Super Admin", fontSize = 13.sp)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("STNK Kendaraan: Terverifikasi", fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Accept Order Dialog
        if (showAcceptDialog != null) {
            val orderToAccept = showAcceptDialog!!
            AppConfirmationDialog(
                title = "Terima Pesanan Ini?",
                message = "Ambil order ${orderToAccept.serviceType} dari ${orderToAccept.pickupAddress} menuju ${orderToAccept.dropoffAddress} dengan ongkos ${formatRupiah(orderToAccept.fare)}?",
                confirmText = "Terima Sekarang",
                onConfirm = {
                    val orderId = orderToAccept.id
                    showAcceptDialog = null
                    scope.launch {
                        val result = orderRepository.acceptOrder(
                            orderId = orderId,
                            driverId = user.userId,
                            driverName = user.name.ifBlank { "Mitra Driver" }
                        )
                        if (result.isSuccess) {
                            Toast.makeText(context, "Pesanan berhasil diterima! Segera jemput pelanggan.", Toast.LENGTH_LONG).show()
                        } else {
                            val err = result.exceptionOrNull()?.localizedMessage ?: "Pesanan sudah diambil driver lain."
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onDismiss = { showAcceptDialog = null }
            )
        }
    }
}

@Composable
fun PerformanceItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DriverActiveOrderCard(
    order: Order,
    onUpdateStatus: (String) -> Unit,
    onWhatsAppClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("driver_active_order_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BlueDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ServiceBadge(serviceType = order.serviceType)
                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Penumpang: ${order.customerName}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (order.customerPhone.isNotBlank()) {
                        Text(
                            text = "Telepon: ${order.customerPhone}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }

                if (order.customerPhone.isNotBlank()) {
                    Button(
                        onClick = { onWhatsAppClick(order.customerPhone) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Route Map Preview Canvas
            Spacer(modifier = Modifier.height(10.dp))
            RouteMapPreview(
                pickupLat = order.pickupLat,
                pickupLng = order.pickupLng,
                dropoffLat = order.dropoffLat,
                dropoffLng = order.dropoffLng,
                driverLat = order.pickupLat + 0.001,
                driverLng = order.pickupLng + 0.001,
                distanceKm = order.distanceKm,
                statusText = order.status
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "Titik Jemput: ${order.pickupAddress}", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Tujuan: ${order.dropoffAddress}", color = Color.White, fontSize = 12.sp)
                    if (order.itemType.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Paket Barang: ${order.itemType} (${order.itemDescription})", color = AccentCyan, fontSize = 12.sp)
                    }
                    if (order.merchantName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Restoran: ${order.merchantName} (${order.foodItemsSummary})", color = AccentAmber, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ongkos: ${formatRupiah(order.fare)} (${order.paymentMethod}) • Bersih: ${formatRupiah(order.fare * 0.85)}",
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action button depending on status
            when (order.status) {
                "ACCEPTED" -> {
                    Button(
                        onClick = { onUpdateStatus("PICKING_UP") },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("arrived_pickup_button")
                    ) {
                        Text("Saya Sudah Tiba di Titik Jemput", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                "PICKING_UP" -> {
                    Button(
                        onClick = { onUpdateStatus("ON_THE_WAY") },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("start_trip_button")
                    ) {
                        Text("Mulai Antar ke Tujuan", fontWeight = FontWeight.Bold)
                    }
                }
                "ON_THE_WAY" -> {
                    Button(
                        onClick = { onUpdateStatus("COMPLETED") },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("complete_trip_button")
                    ) {
                        Text("Selesaikan Perjalanan & Terima Ongkos", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun IncomingOrderCard(
    order: Order,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    var timeLeftSeconds by remember(order.id) { mutableIntStateOf(30) }

    LaunchedEffect(order.id) {
        while (timeLeftSeconds > 0) {
            delay(1000L)
            timeLeftSeconds--
        }
        onReject()
    }

    val progress = timeLeftSeconds / 30f

    Card(
        modifier = modifier.fillMaxWidth().testTag("incoming_order_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(2.dp, AccentAmber),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (timeLeftSeconds > 10) AccentGreen else AccentRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Order Masuk: $timeLeftSeconds dtk",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (timeLeftSeconds > 10) BluePrimary else AccentRed
                    )
                }
                ServiceBadge(serviceType = order.serviceType)
            }

            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (timeLeftSeconds > 10) AccentAmber else AccentRed,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pemesan: ${order.customerName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = formatRupiah(order.fare),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = BluePrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MyLocation, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = order.pickupAddress, fontSize = 12.sp, maxLines = 1)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = order.dropoffAddress, fontSize = 12.sp, maxLines = 1)
            }

            if (order.serviceType == "GO_SEND" && order.itemType.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Paket: ${order.itemType} (${order.itemDescription})", color = AccentCyan, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("reject_order_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tolak", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(2f).testTag("accept_order_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Terima Order", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun openDriverWhatsApp(context: Context, phone: String, message: String) {
    val cleanPhone = phone.replace("+", "").replace("-", "").replace(" ", "").replace("^0".toRegex(), "62")
    val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Aplikasi WhatsApp tidak ditemukan.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun DriverRegistrationCard(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    vehicleType: String,
    onVehicleTypeChange: (String) -> Unit,
    plateNumber: String,
    onPlateNumberChange: (String) -> Unit,
    vehicleModel: String,
    onVehicleModelChange: (String) -> Unit,
    ktpNumber: String,
    onKtpNumberChange: (String) -> Unit,
    simNumber: String,
    onSimNumberChange: (String) -> Unit,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Formulir Pendaftaran Mitra Driver",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Lengkapi data identitas dan kendaraan Anda untuk diverifikasi oleh Super Admin ISTAFA GO.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                label = { Text("Nama Lengkap Sesuai KTP") },
                modifier = Modifier.fillMaxWidth().testTag("reg_driver_name")
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Nomor Telepon Aktif (WhatsApp)") },
                modifier = Modifier.fillMaxWidth().testTag("reg_driver_phone")
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ktpNumber,
                onValueChange = onKtpNumberChange,
                label = { Text("Nomor Induk Kependudukan (NIK/KTP)") },
                modifier = Modifier.fillMaxWidth().testTag("reg_driver_ktp")
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = simNumber,
                onValueChange = onSimNumberChange,
                label = { Text("Nomor Surat Izin Mengemudi (SIM C / A)") },
                modifier = Modifier.fillMaxWidth().testTag("reg_driver_sim")
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = plateNumber,
                onValueChange = onPlateNumberChange,
                label = { Text("Plat Nomor Kendaraan (Contoh: B 1234 XYZ)") },
                modifier = Modifier.fillMaxWidth().testTag("reg_driver_plate")
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = vehicleModel,
                onValueChange = onVehicleModelChange,
                label = { Text("Merk & Model Kendaraan (Contoh: Honda Vario 160)") },
                modifier = Modifier.fillMaxWidth().testTag("reg_driver_model")
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                ErrorBanner(message = errorMessage)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_driver_reg_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Kirim Dokumen Pendaftaran", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
