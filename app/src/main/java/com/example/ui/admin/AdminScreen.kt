package com.example.ui.admin

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AuditLog
import com.example.data.model.DriverSubmission
import com.example.data.model.Order
import com.example.data.model.PricingSetting
import com.example.data.model.User
import com.example.data.repository.AdminRepository
import com.example.ui.common.AppConfirmationDialog
import com.example.ui.common.EmptyStateCard
import com.example.ui.common.ErrorBanner
import com.example.ui.common.ServiceBadge
import com.example.ui.common.StatusBadge
import com.example.ui.common.formatRupiah
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BlueDark
import com.example.ui.theme.BluePrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    user: User,
    adminRepository: AdminRepository,
    onSignOut: () -> Unit,
    onExitAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    val pendingSubmissions by adminRepository.observePendingDriverSubmissions()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val liveOrders by adminRepository.observeAllLiveOrders()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val pricingSettings by adminRepository.observePricingSettings()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val auditLogs by adminRepository.observeAuditLogs()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var rejectDialogSubmission by remember { mutableStateOf<DriverSubmission?>(null) }
    var rejectionReasonText by remember { mutableStateOf("") }
    var approveDialogSubmission by remember { mutableStateOf<DriverSubmission?>(null) }

    // Pricing Editor Dialog
    var editingPricing by remember { mutableStateOf<PricingSetting?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SUPER ADMIN CONSOLE",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Admin: ${user.email.ifBlank { user.name }}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = onExitAdmin,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("Kembali", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Keluar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E293B))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BluePrimary,
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Verifikasi Driver (${pendingSubmissions.count { it.status == "PENDING" }})") },
                    icon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pantau Pesanan (${liveOrders.size})") },
                    icon = { Icon(Icons.Default.TwoWheeler, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Tarif & Komisi") },
                    icon = { Icon(Icons.Default.MonetizationOn, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Audit Logs") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            }

            when (selectedTab) {
                // Tab 0: Driver Verification Queue
                0 -> {
                    val pendingList = pendingSubmissions.filter { it.status == "PENDING" }
                    if (pendingList.isEmpty()) {
                        EmptyStateCard(
                            title = "Tidak Ada Pengajuan Menunggu",
                            message = "Semua pendaftaran mitra driver telah selesai diverifikasi."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(pendingList, key = { it.id }) { submission ->
                                DriverSubmissionItem(
                                    submission = submission,
                                    onApprove = { approveDialogSubmission = submission },
                                    onReject = {
                                        rejectDialogSubmission = submission
                                        rejectionReasonText = ""
                                    }
                                )
                            }
                        }
                    }
                }

                // Tab 1: Live Orders Monitor
                1 -> {
                    if (liveOrders.isEmpty()) {
                        EmptyStateCard(
                            title = "Belum Ada Pesanan di Sistem",
                            message = "Pesanan yang dibuat oleh pelanggan akan otomatis muncul di sini."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(liveOrders, key = { it.id }) { order ->
                                AdminOrderItem(order = order)
                            }
                        }
                    }
                }

                // Tab 2: Pricing Settings
                2 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = "Pengaturan Tarif & Komisi Platform",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Semua perubahan tarif disimpan langsung ke Cloud Firestore dan dilindungi audit trail.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val defaultPricings = listOf(
                            PricingSetting(serviceType = "GO_RIDE", baseFare = 8000.0, perKmRate = 2500.0, minimumFare = 10000.0, platformFeePercent = 15.0),
                            PricingSetting(serviceType = "GO_CAR", baseFare = 15000.0, perKmRate = 4500.0, minimumFare = 18000.0, platformFeePercent = 15.0),
                            PricingSetting(serviceType = "GO_SEND", baseFare = 10000.0, perKmRate = 2800.0, minimumFare = 12000.0, platformFeePercent = 12.0)
                        )

                        val displaySettings = defaultPricings.map { defaultSetting ->
                            pricingSettings.firstOrNull { it.serviceType == defaultSetting.serviceType } ?: defaultSetting
                        }

                        items(displaySettings, key = { it.serviceType }) { setting ->
                            PricingSettingCard(
                                setting = setting,
                                onEdit = { editingPricing = setting }
                            )
                        }
                    }
                }

                // Tab 3: Audit Logs
                3 -> {
                    if (auditLogs.isEmpty()) {
                        EmptyStateCard(
                            title = "Belum Ada Catatan Audit",
                            message = "Semua aktivitas admin seperti persetujuan driver dan pengubahan tarif akan dicatat permanen di sini."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(auditLogs, key = { it.id.ifBlank { it.timestamp?.nanoseconds?.toString() ?: "" } }) { log ->
                                AuditLogItem(log = log)
                            }
                        }
                    }
                }
            }
        }

        // Approve Driver Confirmation
        if (approveDialogSubmission != null) {
            val sub = approveDialogSubmission!!
            AppConfirmationDialog(
                title = "Setujui Pendaftaran Driver?",
                message = "Pengemudi ${sub.driverName} (${sub.plateNumber}) akan disetujui untuk mulai beroperasi dan menerima pesanan.",
                confirmText = "Setujui Driver",
                onConfirm = {
                    approveDialogSubmission = null
                    scope.launch {
                        adminRepository.reviewDriverSubmission(
                            submissionId = sub.id,
                            driverId = sub.driverId,
                            approve = true
                        )
                    }
                },
                onDismiss = { approveDialogSubmission = null }
            )
        }

        // Reject Driver Dialog
        if (rejectDialogSubmission != null) {
            val sub = rejectDialogSubmission!!
            AlertDialog(
                onDismissRequest = { rejectDialogSubmission = null },
                title = { Text("Tolak Pendaftaran Driver", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Masukkan alasan penolakan untuk driver ${sub.driverName}:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = rejectionReasonText,
                            onValueChange = { rejectionReasonText = it },
                            label = { Text("Alasan Penolakan") },
                            modifier = Modifier.fillMaxWidth().testTag("reject_reason_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            rejectDialogSubmission = null
                            scope.launch {
                                adminRepository.reviewDriverSubmission(
                                    submissionId = sub.id,
                                    driverId = sub.driverId,
                                    approve = false,
                                    reason = rejectionReasonText.ifBlank { "Dokumen tidak memenuhi persyaratan." }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Text("Tolak Pendaftaran")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { rejectDialogSubmission = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Edit Pricing Dialog
        if (editingPricing != null) {
            val setting = editingPricing!!
            var editBaseFare by remember { mutableStateOf(setting.baseFare.toString()) }
            var editPerKm by remember { mutableStateOf(setting.perKmRate.toString()) }
            var editMinFare by remember { mutableStateOf(setting.minimumFare.toString()) }
            var editFeePercent by remember { mutableStateOf(setting.platformFeePercent.toString()) }

            AlertDialog(
                onDismissRequest = { editingPricing = null },
                title = { Text("Edit Tarif ${setting.serviceType}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editBaseFare,
                            onValueChange = { editBaseFare = it },
                            label = { Text("Tarif Dasar (Rp)") }
                        )
                        OutlinedTextField(
                            value = editPerKm,
                            onValueChange = { editPerKm = it },
                            label = { Text("Tarif per Km (Rp)") }
                        )
                        OutlinedTextField(
                            value = editMinFare,
                            onValueChange = { editMinFare = it },
                            label = { Text("Tarif Minimal (Rp)") }
                        )
                        OutlinedTextField(
                            value = editFeePercent,
                            onValueChange = { editFeePercent = it },
                            label = { Text("Komisi Platform (%)") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val updated = setting.copy(
                                baseFare = editBaseFare.toDoubleOrNull() ?: setting.baseFare,
                                perKmRate = editPerKm.toDoubleOrNull() ?: setting.perKmRate,
                                minimumFare = editMinFare.toDoubleOrNull() ?: setting.minimumFare,
                                platformFeePercent = editFeePercent.toDoubleOrNull() ?: setting.platformFeePercent
                            )
                            editingPricing = null
                            scope.launch {
                                adminRepository.updatePricingSetting(updated)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text("Simpan Tarif")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingPricing = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun DriverSubmissionItem(
    submission: DriverSubmission,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = submission.driverName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                StatusBadge(status = submission.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Nomor HP: ${submission.phone}", fontSize = 13.sp)
            Text(text = "NIK / KTP: ${submission.ktpNumber}", fontSize = 13.sp)
            Text(text = "Nomor SIM: ${submission.simNumber}", fontSize = 13.sp)
            Text(text = "Kendaraan: ${submission.vehicleType} (${submission.plateNumber})", fontSize = 13.sp)

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                    modifier = Modifier.weight(1f).testTag("admin_reject_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tolak")
                }
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    modifier = Modifier.weight(1f).testTag("admin_approve_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Setujui")
                }
            }
        }
    }
}

@Composable
fun AdminOrderItem(order: Order, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
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

            Text(
                text = "Pemesan: ${order.customerName} • Driver: ${order.driverName.ifBlank { "Belum ada" }}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = "Rute: ${order.pickupAddress} -> ${order.dropoffAddress}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${order.distanceKm} km • ${order.paymentMethod} (${order.paymentStatus})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatRupiah(order.fare),
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun PricingSettingCard(
    setting: PricingSetting,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ServiceBadge(serviceType = setting.serviceType)
                TextButton(onClick = onEdit) {
                    Text("Ubah Tarif", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tarif Dasar:", fontSize = 12.sp)
                Text(formatRupiah(setting.baseFare), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tarif per Km:", fontSize = 12.sp)
                Text(formatRupiah(setting.perKmRate), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tarif Minimal:", fontSize = 12.sp)
                Text(formatRupiah(setting.minimumFare), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Komisi Platform:", fontSize = 12.sp)
                Text("%.1f%%".format(setting.platformFeePercent), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentGreen)
            }
        }
    }
}

@Composable
fun AuditLogItem(log: AuditLog, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.action,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = BlueDark
                )
                Text(
                    text = log.targetResource,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = log.details, fontSize = 11.sp)
        }
    }
}
