package com.example.ui.customer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppNotification
import com.example.data.model.Order
import com.example.data.model.PricingSetting
import com.example.data.model.User
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
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    user: User,
    orderRepository: OrderRepository,
    onSignOut: () -> Unit,
    onSwitchToRole: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Pesan Layanan, 1 = Riwayat, 2 = Kuliner GoFood

    // Booking form state
    var selectedService by remember { mutableStateOf("GO_RIDE") } // GO_RIDE, GO_CAR, GO_SEND, GO_FOOD
    var pickupAddress by remember { mutableStateOf("Jl. Merdeka No. 45, Gambir, Jakarta Pusat") }
    var pickupLat by remember { mutableDoubleStateOf(-6.175392) }
    var pickupLng by remember { mutableDoubleStateOf(106.827153) }

    var dropoffAddress by remember { mutableStateOf("Mall Grand Indonesia, Thamrin, Jakarta Pusat") }
    var dropoffLat by remember { mutableDoubleStateOf(-6.195046) }
    var dropoffLng by remember { mutableDoubleStateOf(106.820297) }

    var estimatedDistanceKm by remember { mutableDoubleStateOf(3.8) }
    var paymentMethod by remember { mutableStateOf("CASH") }

    // GoSend (Barang) specific fields
    var sendItemType by remember { mutableStateOf("Dokumen / Paket") }
    var sendItemDescription by remember { mutableStateOf("") }
    var sendRecipientName by remember { mutableStateOf("") }
    var sendRecipientPhone by remember { mutableStateOf("") }

    // GoFood specific fields
    var foodRestaurantName by remember { mutableStateOf("") }
    var foodOrderItems by remember { mutableStateOf("") }
    var foodTripNotes by remember { mutableStateOf("") }

    var pricing by remember { mutableStateOf(PricingSetting()) }
    var isBooking by remember { mutableStateOf(false) }
    var bookingError by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Dialogs & Modals
    var showCancelOrderDialog by remember { mutableStateOf<Order?>(null) }
    var cancelReasonInput by remember { mutableStateOf("Ingin mengubah titik penjemputan") }
    var showNotificationsModal by remember { mutableStateOf(false) }
    var showSupportTicketDialog by remember { mutableStateOf(false) }
    var showMerchantRegisterDialog by remember { mutableStateOf(false) }
    var ratingTargetOrder by remember { mutableStateOf<Order?>(null) }

    // Realtime Customer Active & History Orders
    val customerOrders by orderRepository.observeCustomerActiveOrders(user.userId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val activeOrder = customerOrders.firstOrNull {
        it.status in listOf("SEARCHING", "ACCEPTED", "PICKING_UP", "ARRIVED", "ON_THE_WAY")
    }

    // Realtime Notifications
    val notifications by orderRepository.observeUserNotifications(user.userId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val unreadNotifCount = notifications.count { !it.isRead }

    // Realtime GoFood Merchants
    val merchants by orderRepository.observeMerchants()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchDeviceLocation(context) { lat, lng ->
                pickupLat = lat
                pickupLng = lng
                pickupAddress = "Lokasi GPS Anda Saat Ini (Lat: %.4f, Lng: %.4f)".format(lat, lng)
                estimatedDistanceKm = OrderRepository.calculateHaversineDistanceKm(pickupLat, pickupLng, dropoffLat, dropoffLng)
                Toast.makeText(context, "Lokasi GPS berhasil diperbarui", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin lokasi diperlukan untuk mendeteksi penjemputan otomatis", Toast.LENGTH_LONG).show()
        }
    }

    // Update pricing when service changes
    LaunchedEffect(selectedService) {
        pricing = orderRepository.getPricingSettings(selectedService)
    }

    // Recalculate distance whenever pickup or dropoff coordinates change
    LaunchedEffect(pickupLat, pickupLng, dropoffLat, dropoffLng) {
        val calculated = OrderRepository.calculateHaversineDistanceKm(pickupLat, pickupLng, dropoffLat, dropoffLng)
        estimatedDistanceKm = if (calculated < 0.5) 1.2 else calculated
    }

    // Dynamic fare calculation with backend pricing formula
    val calculatedFare = remember(selectedService, estimatedDistanceKm, pricing) {
        val raw = pricing.baseFare + (estimatedDistanceKm * pricing.perKmRate)
        maxOf(raw, pricing.minimumFare)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ISTAFA GO",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Halo, ${user.name.ifBlank { "Pelanggan" }}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                actions = {
                    // Notifications Bell with Unread Badge
                    IconButton(
                        onClick = { showNotificationsModal = true },
                        modifier = Modifier.testTag("notifications_bell_button")
                    ) {
                        BadgedBox(badge = {
                            if (unreadNotifCount > 0) {
                                Badge(containerColor = AccentRed) {
                                    Text("$unreadNotifCount")
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Pusat Notifikasi",
                                tint = Color.White
                            )
                        }
                    }

                    // Support / Help Button
                    IconButton(onClick = { showSupportTicketDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Bantuan & Pengaduan",
                            tint = Color.White
                        )
                    }

                    if (onSwitchToRole != null) {
                        TextButton(
                            onClick = { onSwitchToRole("DRIVER") },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text("Mode Driver", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BluePrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Pesan", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.TwoWheeler, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Riwayat (${customerOrders.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.History, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Kuliner GoFood", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Fastfood, contentDescription = null) }
                )
            }

            when (selectedTab) {
                // Tab 0: Pesan Layanan (GoRide, GoCar, GoSend)
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Active Trip Tracker Card (Realtime update & WhatsApp button)
                        if (activeOrder != null) {
                            item {
                                ActiveTripCard(
                                    order = activeOrder,
                                    onCancelClick = { showCancelOrderDialog = activeOrder },
                                    onWhatsAppClick = { phone ->
                                        openWhatsApp(context, phone, "Halo driver ISTAFA GO, saya pemesan perjalanan Anda.")
                                    }
                                )
                            }
                        }

                        // Service Category Selector
                        item {
                            Text(
                                text = "Pilih Layanan ISTAFA GO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ServiceSelectionCard(
                                    title = "GoRide",
                                    desc = "Ojek Motor",
                                    icon = Icons.Default.TwoWheeler,
                                    isSelected = selectedService == "GO_RIDE",
                                    color = AccentGreen,
                                    onClick = { selectedService = "GO_RIDE" },
                                    modifier = Modifier.weight(1f)
                                )
                                ServiceSelectionCard(
                                    title = "GoCar",
                                    desc = "Mobil Nyaman",
                                    icon = Icons.Default.DirectionsCar,
                                    isSelected = selectedService == "GO_CAR",
                                    color = BluePrimary,
                                    onClick = { selectedService = "GO_CAR" },
                                    modifier = Modifier.weight(1f)
                                )
                                ServiceSelectionCard(
                                    title = "GoSend",
                                    desc = "Kirim Barang",
                                    icon = Icons.Default.LocalShipping,
                                    isSelected = selectedService == "GO_SEND",
                                    color = AccentCyan,
                                    onClick = { selectedService = "GO_SEND" },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Location, Route & Interactive GPS Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Titik Jemput & Tujuan",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        TextButton(onClick = {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }) {
                                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Deteksi GPS", fontSize = 12.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Pickup Input
                                    OutlinedTextField(
                                        value = pickupAddress,
                                        onValueChange = { pickupAddress = it },
                                        label = { Text("Lokasi Penjemputan") },
                                        leadingIcon = {
                                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = AccentGreen)
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("pickup_address_input")
                                    )

                                    // Quick Pickup Chips
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        item {
                                            FilterChip(
                                                selected = false,
                                                onClick = {
                                                    pickupAddress = "Stasiun Gambir, Gambir, Jakarta Pusat"
                                                    pickupLat = -6.176655
                                                    pickupLng = 106.830653
                                                },
                                                label = { Text("Stasiun Gambir", fontSize = 11.sp) }
                                            )
                                        }
                                        item {
                                            FilterChip(
                                                selected = false,
                                                onClick = {
                                                    pickupAddress = "Monumen Nasional (Monas), Gambir"
                                                    pickupLat = -6.175392
                                                    pickupLng = 106.827153
                                                },
                                                label = { Text("Monas", fontSize = 11.sp) }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Dropoff Input
                                    OutlinedTextField(
                                        value = dropoffAddress,
                                        onValueChange = { dropoffAddress = it },
                                        label = { Text(if (selectedService == "GO_SEND") "Lokasi Penerima Barang" else "Lokasi Tujuan") },
                                        leadingIcon = {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = AccentRed)
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("dropoff_address_input")
                                    )

                                    // Quick Dropoff Chips
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        item {
                                            FilterChip(
                                                selected = false,
                                                onClick = {
                                                    dropoffAddress = "Mall Grand Indonesia, Thamrin, Jakarta Pusat"
                                                    dropoffLat = -6.195046
                                                    dropoffLng = 106.820297
                                                },
                                                label = { Text("Grand Indonesia", fontSize = 11.sp) }
                                            )
                                        }
                                        item {
                                            FilterChip(
                                                selected = false,
                                                onClick = {
                                                    dropoffAddress = "Bandara Soekarno-Hatta (Terminal 3)"
                                                    dropoffLat = -6.125600
                                                    dropoffLng = 106.655900
                                                },
                                                label = { Text("Bandara Soetta (25 km)", fontSize = 11.sp) }
                                            )
                                        }
                                        item {
                                            FilterChip(
                                                selected = false,
                                                onClick = {
                                                    dropoffAddress = "Kampus Universitas Indonesia, Depok"
                                                    dropoffLat = -6.365000
                                                    dropoffLng = 106.831000
                                                },
                                                label = { Text("Kampus UI", fontSize = 11.sp) }
                                            )
                                        }
                                    }

                                    // Route Map Preview Canvas with interactive GPS
                                    Spacer(modifier = Modifier.height(12.dp))
                                    RouteMapPreview(
                                        pickupLat = pickupLat,
                                        pickupLng = pickupLng,
                                        dropoffLat = dropoffLat,
                                        dropoffLng = dropoffLng,
                                        distanceKm = estimatedDistanceKm,
                                        statusText = "Siap Dipesan"
                                    )
                                }
                            }
                        }

                        // GoSend (Barang) Detail Form if selected
                        if (selectedService == "GO_SEND") {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = AccentCyan.copy(alpha = 0.08f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Detail Barang yang Dikirim (GoSend)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AccentCyan
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedTextField(
                                            value = sendItemType,
                                            onValueChange = { sendItemType = it },
                                            label = { Text("Jenis Barang (Dokumen, Baju, Makanan, Elektronik)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = sendItemDescription,
                                            onValueChange = { sendItemDescription = it },
                                            label = { Text("Deskripsi / Catatan Barang") },
                                            placeholder = { Text("Contoh: Dus sedang, hati-hati barang pecah belah") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = sendRecipientName,
                                            onValueChange = { sendRecipientName = it },
                                            label = { Text("Nama Penerima Barang") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = sendRecipientPhone,
                                            onValueChange = { sendRecipientPhone = it },
                                            label = { Text("Nomor HP Penerima (WhatsApp)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // Fare & Checkout Action Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
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
                                        Column {
                                            Text(
                                                text = "Jarak Tempuh (Haversine)",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "%.1f KM".format(estimatedDistanceKm),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Tarif Final Terverifikasi",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = formatRupiah(calculatedFare),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 20.sp,
                                                color = BluePrimary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Payment Method Selector
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Payment, contentDescription = null, tint = BluePrimary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (paymentMethod == "CASH") "Metode: Tunai ke Driver" else "Metode: Dompet Digital ISTAFA",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        TextButton(onClick = {
                                            paymentMethod = if (paymentMethod == "CASH") "WALLET" else "CASH"
                                        }) {
                                            Text("Ganti", fontSize = 12.sp)
                                        }
                                    }

                                    if (bookingError != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ErrorBanner(message = bookingError!!, onRetry = { bookingError = null })
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Book Now Button
                                    Button(
                                        onClick = {
                                            if (pickupAddress.isBlank() || dropoffAddress.isBlank()) {
                                                bookingError = "Harap tentukan alamat jemput dan alamat tujuan."
                                            } else {
                                                showConfirmDialog = true
                                            }
                                        },
                                        enabled = !isBooking && (activeOrder == null),
                                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("book_order_button")
                                    ) {
                                        if (isBooking) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                        } else {
                                            val label = when (selectedService) {
                                                "GO_CAR" -> "Pesan Mobil GoCar"
                                                "GO_SEND" -> "Kirim Barang Sekarang"
                                                else -> "Pesan Ojek GoRide"
                                            }
                                            Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 1: Riwayat Perjalanan & Riwayat Pengantaran
                1 -> {
                    if (customerOrders.isEmpty()) {
                        EmptyStateCard(
                            title = "Belum Ada Riwayat Pesanan",
                            message = "Semua perjalanan dan pengiriman paket yang Anda pesan akan tercatat di sini."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(customerOrders, key = { it.id.ifBlank { it.customerId } }) { order ->
                                OrderHistoryDetailCard(
                                    order = order,
                                    onRateClick = { ratingTargetOrder = order }
                                )
                            }
                        }
                    }
                }

                // Tab 2: Kuliner GoFood
                2 -> {
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
                                colors = CardDefaults.cardColors(containerColor = AccentAmber.copy(alpha = 0.12f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = AccentAmber)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ISTAFA GoFood (Kuliner Lokal)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Pesan makanan lezat dari merchant dan UMKM kuliner lokal langsung diantar ke depan rumah Anda.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF856404)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { showMerchantRegisterDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = BlueDark)
                                    ) {
                                        Text("Daftarkan Merchant / Usaha Kuliner", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        if (merchants.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "Katalog Merchant Sedang Disiapkan",
                                    message = "Belum ada merchant kuliner yang terverifikasi saat ini. Anda dapat mendaftarkan usaha kuliner Anda melalui tombol di atas untuk proses verifikasi oleh Super Admin pada tahap selanjutnya.",
                                    icon = Icons.Default.Fastfood
                                )
                            }
                        } else {
                            items(merchants, key = { it.id }) { merchant ->
                                MerchantCard(
                                    merchant = merchant,
                                    onOrder = {
                                        selectedService = "GO_FOOD"
                                        pickupAddress = merchant.address
                                        foodRestaurantName = merchant.name
                                        selectedTab = 0
                                        Toast.makeText(context, "Merchant ${merchant.name} dipilih!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Booking Confirmation Dialog
        if (showConfirmDialog) {
            AppConfirmationDialog(
                title = "Konfirmasi Pemesanan",
                message = "Pesan $selectedService dari $pickupAddress menuju $dropoffAddress dengan ongkos terverifikasi ${formatRupiah(calculatedFare)}?",
                confirmText = "Ya, Pesan Sekarang",
                onConfirm = {
                    showConfirmDialog = false
                    isBooking = true
                    bookingError = null
                    scope.launch {
                        val result = orderRepository.createOrder(
                            customerName = user.name.ifBlank { "Pelanggan" },
                            customerPhone = user.phone,
                            serviceType = selectedService,
                            pickupAddress = pickupAddress,
                            pickupLat = pickupLat,
                            pickupLng = pickupLng,
                            dropoffAddress = dropoffAddress,
                            dropoffLat = dropoffLat,
                            dropoffLng = dropoffLng,
                            fare = calculatedFare,
                            distanceKm = estimatedDistanceKm,
                            paymentMethod = paymentMethod,
                            itemType = sendItemType,
                            itemDescription = sendItemDescription,
                            recipientName = sendRecipientName,
                            recipientPhone = sendRecipientPhone,
                            merchantName = foodRestaurantName,
                            foodItemsSummary = foodOrderItems,
                            tripNotes = foodTripNotes
                        )
                        isBooking = false
                        if (result.isFailure) {
                            bookingError = result.exceptionOrNull()?.localizedMessage ?: "Gagal membuat pesanan"
                        }
                    }
                },
                onDismiss = { showConfirmDialog = false }
            )
        }

        // Cancel with Reason Dialog
        if (showCancelOrderDialog != null) {
            val orderToCancel = showCancelOrderDialog!!
            AlertDialog(
                onDismissRequest = { showCancelOrderDialog = null },
                title = { Text("Batalkan Pesanan?", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Pilih atau tuliskan alasan pembatalan pesanan Anda:")
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = cancelReasonInput,
                            onValueChange = { cancelReasonInput = it },
                            label = { Text("Alasan Pembatalan") },
                            modifier = Modifier.fillMaxWidth().testTag("cancel_reason_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val orderId = orderToCancel.id
                            showCancelOrderDialog = null
                            scope.launch {
                                orderRepository.cancelOrder(
                                    orderId = orderId,
                                    cancelledBy = "CUSTOMER",
                                    reason = cancelReasonInput.ifBlank { "Dibatalkan oleh pelanggan" }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) {
                        Text("Konfirmasi Batalkan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelOrderDialog = null }) {
                        Text("Kembali")
                    }
                }
            )
        }

        // Notifications Modal Dialog
        if (showNotificationsModal) {
            AlertDialog(
                onDismissRequest = { showNotificationsModal = false },
                title = { Text("Pusat Notifikasi", fontWeight = FontWeight.Bold) },
                text = {
                    if (notifications.isEmpty()) {
                        Text("Belum ada notifikasi baru.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(notifications, key = { it.id }) { notif ->
                                Surface(
                                    color = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else BluePrimary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        scope.launch { orderRepository.markNotificationAsRead(notif.id) }
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = notif.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = notif.body, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showNotificationsModal = false }) {
                        Text("Tutup")
                    }
                }
            )
        }

        // Support Ticket Submission Dialog
        if (showSupportTicketDialog) {
            var ticketCategory by remember { mutableStateOf("Bantuan Perjalanan") }
            var ticketMessage by remember { mutableStateOf("") }
            var isSendingTicket by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showSupportTicketDialog = false },
                title = { Text("Bantuan & Pengaduan", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sampaikan kendala, keluhan, atau pertanyaan Anda:", fontSize = 13.sp)
                        OutlinedTextField(
                            value = ticketCategory,
                            onValueChange = { ticketCategory = it },
                            label = { Text("Kategori (Tarif, Driver, Aplikasi, Barang)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ticketMessage,
                            onValueChange = { ticketMessage = it },
                            label = { Text("Deskripsi Kendala") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (ticketMessage.isNotBlank()) {
                                isSendingTicket = true
                                scope.launch {
                                    orderRepository.createSupportTicket(ticketCategory, ticketMessage)
                                    isSendingTicket = false
                                    showSupportTicketDialog = false
                                    Toast.makeText(context, "Tiket bantuan berhasil dikirim", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isSendingTicket,
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text("Kirim Laporan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSupportTicketDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Merchant Registration Dialog (GoFood)
        if (showMerchantRegisterDialog) {
            var mName by remember { mutableStateOf("") }
            var mAddress by remember { mutableStateOf("") }
            var mPhone by remember { mutableStateOf(user.phone) }
            var mCategory by remember { mutableStateOf("Makanan Nusantara") }

            AlertDialog(
                onDismissRequest = { showMerchantRegisterDialog = false },
                title = { Text("Daftar Merchant GoFood", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Daftarkan restoran atau kedai kuliner Anda:", fontSize = 13.sp)
                        OutlinedTextField(value = mName, onValueChange = { mName = it }, label = { Text("Nama Resto / Usaha Kuliner") })
                        OutlinedTextField(value = mAddress, onValueChange = { mAddress = it }, label = { Text("Alamat Restoran") })
                        OutlinedTextField(value = mPhone, onValueChange = { mPhone = it }, label = { Text("No. HP / WhatsApp Resto") })
                        OutlinedTextField(value = mCategory, onValueChange = { mCategory = it }, label = { Text("Kategori Makanan") })
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (mName.isNotBlank() && mAddress.isNotBlank()) {
                                scope.launch {
                                    orderRepository.registerMerchant(mName, mAddress, mPhone, mCategory)
                                    showMerchantRegisterDialog = false
                                    Toast.makeText(context, "Pendaftaran merchant berhasil diajukan untuk verifikasi!", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text("Ajukan Pendaftaran")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMerchantRegisterDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Rating & Review Dialog
        if (ratingTargetOrder != null) {
            val orderToRate = ratingTargetOrder!!
            RatingReviewDialog(
                driverName = orderToRate.driverName.ifBlank { "Mitra Driver" },
                onDismiss = { ratingTargetOrder = null },
                onSubmit = { stars, review ->
                    val oId = orderToRate.id
                    val dId = orderToRate.driverId
                    ratingTargetOrder = null
                    scope.launch {
                        orderRepository.submitOrderRating(oId, dId, stars, review)
                        Toast.makeText(context, "Terima kasih atas ulasan dan penilaian Anda!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
fun ActiveTripCard(
    order: Order,
    onCancelClick: () -> Unit,
    onWhatsAppClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("active_trip_card"),
        shape = RoundedCornerShape(18.dp),
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

            // Dynamic Status Description
            when (order.status) {
                "SEARCHING" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Menghubungkan ke driver terdekat...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                "ACCEPTED" -> {
                    Text(
                        text = "Driver ${order.driverName} telah menerima pesanan!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Driver sedang meluncur menuju titik jemput Anda.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                "PICKING_UP" -> {
                    Text(
                        text = "Driver sedang dalam perjalanan menuju lokasi jemput",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                "ARRIVED" -> {
                    Text(
                        text = "Driver telah tiba di titik penjemputan!",
                        color = AccentAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Silakan temui driver di lokasi.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                "ON_THE_WAY" -> {
                    Text(
                        text = "Perjalanan / Pengantaran sedang berlangsung...",
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // Route Map Preview Canvas
            Spacer(modifier = Modifier.height(10.dp))
            RouteMapPreview(
                pickupLat = order.pickupLat,
                pickupLng = order.pickupLng,
                dropoffLat = order.dropoffLat,
                dropoffLng = order.dropoffLng,
                driverLat = if (order.driverId.isNotBlank()) order.pickupLat + 0.002 else null,
                driverLng = if (order.driverId.isNotBlank()) order.pickupLng + 0.002 else null,
                distanceKm = order.distanceKm,
                statusText = when (order.status) {
                    "SEARCHING" -> "Mencari Driver"
                    "ACCEPTED" -> "Driver Menuju Jemputan"
                    "PICKING_UP" -> "Driver Menuju Jemputan"
                    "ARRIVED" -> "Driver Tiba"
                    "ON_THE_WAY" -> "Dalam Perjalanan"
                    else -> order.status
                }
            )

            // Driver Card & WhatsApp Privacy Button (Shown ONLY if assigned)
            if (order.driverId.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Driver: ${order.driverName}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (order.driverPlateNumber.isNotBlank()) {
                                Text(text = "Plat: ${order.driverPlateNumber}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }

                        // WhatsApp Privacy Action
                        if (order.driverPhone.isNotBlank()) {
                            Button(
                                onClick = { onWhatsAppClick(order.driverPhone) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Route Details
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "Jemput: ${order.pickupAddress}", color = Color.White, fontSize = 12.sp, maxLines = 1)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Tujuan: ${order.dropoffAddress}", color = Color.White, fontSize = 12.sp, maxLines = 1)
                    if (order.itemType.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Barang: ${order.itemType} (${order.itemDescription})", color = AccentCyan, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ongkos: ${formatRupiah(order.fare)} (${order.paymentMethod})",
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Cancellation Action (Allowed while SEARCHING, ACCEPTED, or PICKING_UP)
            if (order.status in listOf("SEARCHING", "ACCEPTED", "PICKING_UP")) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancelClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("cancel_order_button")
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Batalkan Pesanan", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun OrderHistoryDetailCard(
    order: Order,
    onRateClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
            Text(text = order.dropoffAddress, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = "Dari: ${order.pickupAddress}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (order.status == "CANCELLED" && order.cancelReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Alasan batal (${order.cancelledBy}): ${order.cancelReason}",
                    color = AccentRed,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "%.1f km • Driver: ${order.driverName.ifBlank { "Tidak ada" }}".format(order.distanceKm),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatRupiah(order.fare),
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    fontSize = 14.sp
                )
            }

            if (order.status == "COMPLETED") {
                Spacer(modifier = Modifier.height(8.dp))
                if (order.rating > 0.0) {
                    Surface(
                        color = AccentAmber.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Penilaian Anda: %.1f ★ ${if (order.review.isNotBlank()) "(\"${order.review}\")" else ""}".format(order.rating),
                                fontSize = 12.sp,
                                color = Color(0xFF856404),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (onRateClick != null) {
                    OutlinedButton(
                        onClick = onRateClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentAmber),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("rate_driver_button")
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Beri Nilai Driver (1-5 Bintang)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MerchantCard(
    merchant: com.example.data.model.Merchant,
    onOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = merchant.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = merchant.category, color = BluePrimary, fontSize = 12.sp)
                Text(text = merchant.address, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onOrder,
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Pesan", fontSize = 12.sp)
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchDeviceLocation(context: Context, onLocation: (Double, Double) -> Unit) {
    try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                onLocation(loc.latitude, loc.longitude)
            }
        }
    } catch (_: Exception) {}
}

private fun openWhatsApp(context: Context, phone: String, message: String) {
    val cleanPhone = phone.replace("+", "").replace("-", "").replace(" ", "").replace("^0".toRegex(), "62")
    val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Aplikasi WhatsApp tidak ditemukan di perangkat ini.", Toast.LENGTH_LONG).show()
    }
}
