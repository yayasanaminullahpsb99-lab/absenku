package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditLocationAlt
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AdminConfigEntity
import com.example.data.local.AttendanceEntity
import com.example.data.local.AttendanceStatus
import com.example.data.local.EmployeeEntity
import com.example.data.local.GpsLocationEntity
import com.example.data.location.GpsLocationTracker
import com.example.data.repository.AttendanceRepository
import com.example.ui.state.GpsUiState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SlateNavy
import com.example.ui.theme.SlateNavyDark
import com.example.ui.theme.StatusAbsentFg
import com.example.ui.theme.StatusPresentBg
import com.example.ui.theme.StatusPresentFg
import com.example.ui.theme.TealAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsensiScreen(
    currentTimeMillis: Long,
    gpsState: GpsUiState,
    activeLocation: GpsLocationEntity?,
    activeEmployees: List<EmployeeEntity>,
    selectedEmployee: EmployeeEntity?,
    todayAttendance: List<AttendanceEntity>,
    adminConfig: AdminConfigEntity,
    onSelectEmployee: (Long) -> Unit,
    onRefreshGps: () -> Unit,
    onCheckIn: (String) -> Unit,
    onCheckOut: () -> Unit,
    onNavigateToLocations: () -> Unit,
    onNavigateToEmployees: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var employeeDropdownExpanded by remember { mutableStateOf(false) }
    var noteInput by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onRefreshGps()
        }
    }

    val timeFormatted = remember(currentTimeMillis) {
        SimpleDateFormat("HH:mm:ss", Locale("id", "ID")).format(Date(currentTimeMillis))
    }
    val dateFormatted = remember(currentTimeMillis) {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date(currentTimeMillis))
    }

    val currentCoords = (gpsState as? GpsUiState.Ready)?.coordinates
    val distanceMeters = remember(currentCoords, activeLocation) {
        if (currentCoords != null && activeLocation != null) {
            GpsLocationTracker.distanceInMeters(
                currentCoords.latitude,
                currentCoords.longitude,
                activeLocation.latitude,
                activeLocation.longitude
            )
        } else null
    }
    val withinRadius = distanceMeters != null && activeLocation != null &&
        distanceMeters <= activeLocation.radiusMeters.toFloat()

    val selectedTodayRecord = remember(todayAttendance, selectedEmployee) {
        selectedEmployee?.let { emp -> todayAttendance.firstOrNull { it.employeeId == emp.id } }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live Clock Hero Header
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(SlateNavy, SlateNavyDark)
                            ),
                            shape = MaterialTheme.shapes.large
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(TealAccent)
                                    )
                                    Text(
                                        text = "Mode Offline • SQLite",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Masuk %02d:%02d",
                                    adminConfig.workStartHour,
                                    adminConfig.workStartMinute
                                ),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFamily
                                ),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            color = Color.White
                        )
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 2. GPS Verification Card
        item {
            CleanCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            activeLocation == null -> MaterialTheme.colorScheme.surfaceVariant
                                            withinRadius -> StatusPresentBg
                                            else -> MaterialTheme.colorScheme.errorContainer
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (gpsState) {
                                        is GpsUiState.Ready -> Icons.Rounded.GpsFixed
                                        is GpsUiState.LocationDisabled,
                                        is GpsUiState.PermissionRequired,
                                        is GpsUiState.Error -> Icons.Rounded.GpsOff
                                        else -> Icons.Rounded.MyLocation
                                    },
                                    contentDescription = "Status GPS",
                                    tint = when {
                                        activeLocation == null -> MaterialTheme.colorScheme.primary
                                        withinRadius -> StatusPresentFg
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                            Column {
                                Text(
                                    text = activeLocation?.name ?: "Titik Lokasi Belum Diatur",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = activeLocation?.let {
                                        "Radius maks ${it.radiusMeters} m • ${if (adminConfig.strictRadiusMode) "Mode Ketat" else "Mode Fleksibel"}"
                                    } ?: "Atur koordinat kantor di tab Lokasi GPS",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefreshGps,
                            modifier = Modifier.testTag("refresh_gps_button")
                        ) {
                            if (gpsState is GpsUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Segarkan Lokasi GPS"
                                )
                            }
                        }
                    }

                    when (gpsState) {
                        is GpsUiState.PermissionRequired -> {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Izin lokasi GPS belum diberikan.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Button(
                                        onClick = {
                                            permissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        },
                                        modifier = Modifier.testTag("request_location_permission_button")
                                    ) {
                                        Text("Izinkan Akses GPS")
                                    }
                                }
                            }
                        }

                        is GpsUiState.LocationDisabled -> {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "GPS perangkat sedang nonaktif.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                        }
                                    ) {
                                        Text("Buka Pengaturan GPS")
                                    }
                                }
                            }
                        }

                        is GpsUiState.Error -> {
                            Text(
                                text = gpsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        is GpsUiState.Loading, GpsUiState.Idle -> {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = "Mencari koordinat GPS perangkat...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        is GpsUiState.Ready -> {
                            val coords = gpsState.coordinates
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = String.format(
                                                Locale.US,
                                                "%.6f, %.6f",
                                                coords.latitude,
                                                coords.longitude
                                            ),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = JetBrainsMonoFamily
                                            )
                                        )
                                        Text(
                                            text = "Akurasi ±${coords.accuracyMeters.toInt()} m",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (distanceMeters != null && activeLocation != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (withinRadius) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                                contentDescription = null,
                                                tint = if (withinRadius) StatusPresentFg else StatusAbsentFg,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = if (withinRadius) {
                                                    "Dalam jangkauan (${distanceMeters.toInt()} m dari titik)"
                                                } else {
                                                    "Di luar jangkauan (${distanceMeters.toInt()} m > ${activeLocation.radiusMeters} m)"
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (withinRadius) StatusPresentFg else StatusAbsentFg
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (activeLocation == null) {
                        FilledTonalButton(
                            onClick = onNavigateToLocations,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("goto_setup_location_button")
                        ) {
                            Icon(Icons.Rounded.EditLocationAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Atur Lokasi GPS Sekarang")
                        }
                    }
                }
            }
        }

        // 3. Employee Selector & Check-In / Check-Out Action
        item {
            if (activeEmployees.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Rounded.PersonAdd,
                    title = "Belum Ada Karyawan Aktif",
                    subtitle = "Tambahkan data karyawan terlebih dahulu di menu Karyawan agar dapat melakukan absensi.",
                    actionLabel = "Kelola Karyawan",
                    onAction = onNavigateToEmployees,
                    actionTestTag = "goto_add_employee_button"
                )
            } else {
                CleanCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Form Absensi Harian",
                            style = MaterialTheme.typography.titleMedium
                        )

                        ExposedDropdownMenuBox(
                            expanded = employeeDropdownExpanded,
                            onExpandedChange = { employeeDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedEmployee?.let { "${it.name} (${it.nip})" } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Pilih Karyawan") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Person, contentDescription = null)
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeDropdownExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("employee_selector_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = employeeDropdownExpanded,
                                onDismissRequest = { employeeDropdownExpanded = false }
                            ) {
                                activeEmployees.forEach { emp ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(emp.name, fontWeight = FontWeight.Medium)
                                                Text(
                                                    "${emp.nip} • ${emp.division}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            onSelectEmployee(emp.id)
                                            employeeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedTodayRecord?.checkInTimeMillis == null) {
                            OutlinedTextField(
                                value = noteInput,
                                onValueChange = { noteInput = it },
                                label = { Text("Catatan Kehadiran (Opsional)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("attendance_note_input")
                            )
                        }

                        val hasCheckedIn = selectedTodayRecord?.checkInTimeMillis != null
                        val hasCheckedOut = selectedTodayRecord?.checkOutTimeMillis != null
                        val gpsReady = currentCoords != null && activeLocation != null
                        val radiusPermitted = !adminConfig.strictRadiusMode || withinRadius

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    onCheckIn(noteInput)
                                    noteInput = ""
                                },
                                enabled = !hasCheckedIn && gpsReady && radiusPermitted,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("check_in_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TealAccent,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (hasCheckedIn) "Sudah Masuk" else "Absen Masuk")
                            }

                            Button(
                                onClick = onCheckOut,
                                enabled = hasCheckedIn && !hasCheckedOut && gpsReady && radiusPermitted,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("check_out_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (hasCheckedOut) "Sudah Pulang" else "Absen Pulang")
                            }
                        }

                        if (selectedTodayRecord != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Status Hari Ini",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Masuk ${AttendanceRepository.formatTime(selectedTodayRecord.checkInTimeMillis)} • Pulang ${AttendanceRepository.formatTime(selectedTodayRecord.checkOutTimeMillis)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = JetBrainsMonoFamily
                                            ),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    AttendanceStatusBadge(
                                        status = AttendanceStatus.fromCode(selectedTodayRecord.status)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Today's Attendance Feed
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Absensi Hari Ini (${todayAttendance.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (todayAttendance.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Schedule,
                    title = "Belum Ada Catatan Hari Ini",
                    subtitle = "Aktivitas absen masuk dan pulang hari ini akan muncul secara otomatis di sini."
                )
            }
        } else {
            items(todayAttendance, key = { it.id }) { record ->
                TodayAttendanceItem(record = record)
            }
        }
    }
}

@Composable
private fun TodayAttendanceItem(record: AttendanceEntity) {
    CleanCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = record.employeeName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${record.employeeNip} • ${record.locationName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Masuk ${AttendanceRepository.formatTime(record.checkInTimeMillis)} | Pulang ${AttendanceRepository.formatTime(record.checkOutTimeMillis)}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            AttendanceStatusBadge(status = AttendanceStatus.fromCode(record.status))
        }
    }
}
