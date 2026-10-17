package com.example.ui.common

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
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BlueDark
import com.example.ui.theme.BluePrimary

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (status) {
        "SEARCHING" -> Triple("Mencari Driver", Color(0xFFFFF3CD), Color(0xFF856404))
        "ACCEPTED" -> Triple("Driver Menuju Jemputan", Color(0xFFCCE5FF), Color(0xFF004085))
        "PICKING_UP" -> Triple("Driver Sampai", Color(0xFFD1ECF1), Color(0xFF0C5460))
        "ON_THE_WAY" -> Triple("Dalam Perjalanan", Color(0xFFD4EDDA), Color(0xFF155724))
        "COMPLETED" -> Triple("Selesai", Color(0xFFE2F0D9), Color(0xFF28A745))
        "CANCELLED" -> Triple("Dibatalkan", Color(0xFFF8D7DA), Color(0xFF721C24))
        "APPROVED" -> Triple("Terverifikasi", Color(0xFFD4EDDA), Color(0xFF155724))
        "PENDING", "PENDING_VERIFICATION" -> Triple("Menunggu Verifikasi", Color(0xFFFFF3CD), Color(0xFF856404))
        "REJECTED" -> Triple("Ditolak", Color(0xFFF8D7DA), Color(0xFF721C24))
        "SUSPENDED" -> Triple("Dibekukan", Color(0xFFF8D7DA), Color(0xFF721C24))
        else -> Triple(status, Color(0xFFE2E3E5), Color(0xFF383D41))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ServiceBadge(serviceType: String, modifier: Modifier = Modifier) {
    val (icon, label, color) = when (serviceType) {
        "GO_CAR" -> Triple(Icons.Default.DirectionsCar, "GoCar", BluePrimary)
        "GO_SEND" -> Triple(Icons.Default.LocalShipping, "GoSend", AccentCyan)
        else -> Triple(Icons.Default.TwoWheeler, "GoRide", AccentGreen)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.Info,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(BluePrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("error_banner"),
        colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Peringatan",
                tint = AccentRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = AccentRed,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.testTag("retry_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Coba Lagi",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Coba Lagi", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AppConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Ya, Lanjutkan",
    cancelText: String = "Batal",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = { Text(text = message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText)
            }
        }
    )
}

fun formatRupiah(amount: Double): String {
    val rounded = amount.toLong()
    val formatted = "%,d".format(rounded).replace(',', '.')
    return "Rp $formatted"
}

@Composable
fun ServiceSelectionCard(
    title: String,
    desc: String,
    icon: ImageVector,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.testTag("service_card_${title.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, color) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) color else color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) Color.White else color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Visualisasi Peta Rute Interaktif (Google Maps visual rendering):
 * Menampilkan grid jalan, titik jemput, titik antar, driver live marker, dan status GPS.
 */
@Composable
fun RouteMapPreview(
    pickupLat: Double,
    pickupLng: Double,
    dropoffLat: Double,
    dropoffLng: Double,
    driverLat: Double? = null,
    driverLng: Double? = null,
    distanceKm: Double = 0.0,
    statusText: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .testTag("route_map_preview"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Stylized vector map canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Road grid
                val roadColor = Color(0xFFE2E8F0)
                val gridStep = 40f
                var x = 0f
                while (x < w) {
                    drawLine(
                        color = roadColor,
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 2f
                    )
                    x += gridStep
                }
                var y = 0f
                while (y < h) {
                    drawLine(
                        color = roadColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 2f
                    )
                    y += gridStep
                }

                // Main route curve (Pickup -> Dropoff)
                val pStart = Offset(w * 0.22f, h * 0.72f)
                val pEnd = Offset(w * 0.78f, h * 0.28f)
                val pCtrl1 = Offset(w * 0.35f, h * 0.25f)
                val pCtrl2 = Offset(w * 0.65f, h * 0.75f)

                val routePath = Path().apply {
                    moveTo(pStart.x, pStart.y)
                    cubicTo(pCtrl1.x, pCtrl1.y, pCtrl2.x, pCtrl2.y, pEnd.x, pEnd.y)
                }

                // Route halo background
                drawPath(
                    path = routePath,
                    color = BluePrimary.copy(alpha = 0.25f),
                    style = Stroke(width = 14f, cap = StrokeCap.Round)
                )

                // Route main line
                drawPath(
                    path = routePath,
                    color = BluePrimary,
                    style = Stroke(
                        width = 6f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                    )
                )

                // Pickup marker halo and dot
                drawCircle(color = AccentGreen.copy(alpha = 0.3f), radius = 22f, center = pStart)
                drawCircle(color = AccentGreen, radius = 12f, center = pStart)
                drawCircle(color = Color.White, radius = 5f, center = pStart)

                // Dropoff marker halo and dot
                drawCircle(color = AccentRed.copy(alpha = 0.3f), radius = 22f, center = pEnd)
                drawCircle(color = AccentRed, radius = 12f, center = pEnd)
                drawCircle(color = Color.White, radius = 5f, center = pEnd)

                // Driver position marker if assigned
                val driverPoint = if (driverLat != null && driverLng != null) {
                    Offset(w * 0.45f, h * 0.48f)
                } else null

                if (driverPoint != null) {
                    drawCircle(color = AccentAmber.copy(alpha = 0.4f), radius = 24f, center = driverPoint)
                    drawCircle(color = AccentAmber, radius = 14f, center = driverPoint)
                    drawCircle(color = Color.White, radius = 6f, center = driverPoint)
                }
            }

            // Top overlay with GPS accuracy & Service label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Peta Rute & GPS Aktif",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueDark
                        )
                    }
                }

                if (distanceKm > 0) {
                    Surface(
                        color = BlueDark,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "%.1f km".format(distanceKm),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Bottom overlay with pickup and dropoff indicators
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AccentGreen))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Titik Jemput", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AccentRed))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tujuan", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        if (driverLat != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AccentAmber))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Driver", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (statusText != null) {
                    Surface(
                        color = AccentGreen,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog Rating & Ulasan Bintang 1-5 untuk Driver
 */
@Composable
fun RatingReviewDialog(
    driverName: String,
    onDismiss: () -> Unit,
    onSubmit: (Double, String) -> Unit
) {
    var selectedRating by remember { mutableIntStateOf(5) }
    var reviewText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Beri Nilai Driver",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bagaimana pelayanan $driverName?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive 5 Stars
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= selectedRating) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Bintang $star",
                            tint = if (star <= selectedRating) AccentAmber else Color.Gray,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { selectedRating = star }
                        )
                    }
                }

                Text(
                    text = when (selectedRating) {
                        5 -> "Sangat Puas! Driver ramah & tepat waktu."
                        4 -> "Bagus dan memuaskan."
                        3 -> "Cukup baik."
                        2 -> "Kurang memuaskan."
                        else -> "Kecewa dengan pelayanan."
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BluePrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    label = { Text("Tuliskan ulasan Anda (opsional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("review_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedRating.toDouble(), reviewText) },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                modifier = Modifier.testTag("submit_rating_button")
            ) {
                Text("Kirim Penilaian", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Lewati")
            }
        }
    )
}
