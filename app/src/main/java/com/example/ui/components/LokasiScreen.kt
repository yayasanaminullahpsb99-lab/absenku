package com.example.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AdminConfigEntity
import com.example.data.local.GpsLocationEntity
import com.example.ui.state.GpsUiState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusPresentBg
import com.example.ui.theme.StatusPresentFg
import java.util.Locale

@Composable
fun LokasiScreen(
    adminConfig: AdminConfigEntity,
    isAdminUnlocked: Boolean,
    locations: List<GpsLocationEntity>,
    gpsState: GpsUiState,
    onUnlockAdmin: (String) -> Boolean,
    onLockAdmin: () -> Unit,
    onRefreshGps: () -> Unit,
    onSaveLocation: (GpsLocationEntity?, String, String, Double?, Double?, Int, Boolean) -> Unit,
    onActivateLocation: (GpsLocationEntity) -> Unit,
    onDeleteLocation: (GpsLocationEntity) -> Unit,
    onUpdateAdminSettings: (String, Boolean, Int, Int, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (adminConfig.requirePinForAdmin && !isAdminUnlocked) {
        AdminPinGateCard(
            sectionTitle = "Lokasi GPS & Pengaturan",
            onUnlock = onUnlockAdmin,
            modifier = modifier
        )
        return
    }

    var showLocationDialog by remember { mutableStateOf(false) }
    var editingLocation by remember { mutableStateOf<GpsLocationEntity?>(null) }
    var deletingLocation by remember { mutableStateOf<GpsLocationEntity?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lokasi GPS & Aturan",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Atur titik koordinat GPS & radius absensi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    if (adminConfig.requirePinForAdmin) {
                        IconButton(onClick = onLockAdmin) {
                            Icon(Icons.Rounded.LockOpen, contentDescription = "Kunci Mode Admin")
                        }
                    }
                    Button(
                        onClick = {
                            editingLocation = null
                            showLocationDialog = true
                        },
                        modifier = Modifier.testTag("add_location_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Lokasi")
                    }
                }
            }
        }

        // Work Hours & Security Summary Card
        item {
            CleanCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Pengaturan Jam Kerja & Admin",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        FilledTonalButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("edit_admin_settings_button")
                        ) {
                            Text("Ubah")
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Jam Masuk & Toleransi", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(
                                Locale.US,
                                "%02d:%02d (+%d mnt)",
                                adminConfig.workStartHour,
                                adminConfig.workStartMinute,
                                adminConfig.lateToleranceMinutes
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Validasi Radius GPS", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (adminConfig.strictRadiusMode) "Ketat (Wajib di radius)" else "Fleksibel (Catat jarak)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kunci PIN Admin", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (adminConfig.requirePinForAdmin) "Aktif" else "Nonaktif",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Daftar Titik Lokasi GPS (${locations.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (locations.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.LocationOn,
                    title = "Belum Ada Lokasi GPS",
                    subtitle = "Tambahkan titik koordinat kantor atau lokasi kerja agar karyawan dapat melakukan absensi sesuai radius GPS.",
                    actionLabel = "Tambah Lokasi GPS",
                    onAction = {
                        editingLocation = null
                        showLocationDialog = true
                    },
                    actionTestTag = "empty_add_location_button"
                )
            }
        } else {
            items(locations, key = { it.id }) { loc ->
                LocationCard(
                    location = loc,
                    onActivate = { onActivateLocation(loc) },
                    onEdit = {
                        editingLocation = loc
                        showLocationDialog = true
                    },
                    onDelete = { deletingLocation = loc }
                )
            }
        }
    }

    if (showLocationDialog) {
        LocationFormDialog(
            editing = editingLocation,
            gpsState = gpsState,
            onRefreshGps = onRefreshGps,
            onDismiss = { showLocationDialog = false },
            onSave = { name, note, lat, lng, radius, active ->
                onSaveLocation(editingLocation, name, note, lat, lng, radius, active)
                showLocationDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        AdminSettingsDialog(
            currentConfig = adminConfig,
            onDismiss = { showSettingsDialog = false },
            onSave = { pin, requirePin, hour, minute, tolerance, strict ->
                onUpdateAdminSettings(pin, requirePin, hour, minute, tolerance, strict)
                showSettingsDialog = false
            }
        )
    }

    deletingLocation?.let { target ->
        ConfirmDeleteDialog(
            title = "Hapus Lokasi GPS?",
            message = "Titik lokasi \"${target.name}\" akan dihapus dari SQLite.",
            onConfirm = {
                onDeleteLocation(target)
                deletingLocation = null
            },
            onDismiss = { deletingLocation = null }
        )
    }
}

@Composable
private fun LocationCard(
    location: GpsLocationEntity,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    CleanCard(modifier = Modifier.testTag("location_card_${location.id}")) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = location.isActive,
                        onClick = onActivate,
                        modifier = Modifier.testTag("activate_location_${location.id}")
                    )
                    Column {
                        Text(
                            text = location.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (location.addressNote.isNotBlank()) {
                            Text(
                                text = location.addressNote,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (location.isActive) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = StatusPresentBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = StatusPresentFg,
                                modifier = Modifier.height(14.dp)
                            )
                            Text(
                                text = "Aktif",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusPresentFg
                            )
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "%.6f, %.6f", location.latitude, location.longitude),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFamily
                        )
                    )
                    Text(
                        text = "Radius ${location.radiusMeters} m",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!location.isActive) {
                    TextButton(onClick = onActivate) {
                        Text("Jadikan Aktif")
                    }
                }
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_location_${location.id}")
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Ubah")
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_location_${location.id}")
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocationFormDialog(
    editing: GpsLocationEntity?,
    gpsState: GpsUiState,
    onRefreshGps: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, Double?, Double?, Int, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var addressNote by remember { mutableStateOf(editing?.addressNote ?: "") }
    var latText by remember { mutableStateOf(editing?.latitude?.toString() ?: "") }
    var lngText by remember { mutableStateOf(editing?.longitude?.toString() ?: "") }
    var radiusText by remember { mutableStateOf((editing?.radiusMeters ?: 100).toString()) }
    var setAsActive by remember { mutableStateOf(editing?.isActive ?: true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) onRefreshGps()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editing == null) "Tambah Lokasi GPS" else "Ubah Lokasi GPS")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMsg = null
                    },
                    label = { Text("Nama Lokasi (cth: Kantor Pusat)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_name_input")
                )

                OutlinedTextField(
                    value = addressNote,
                    onValueChange = { addressNote = it },
                    label = { Text("Keterangan Alamat (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                FilledTonalButton(
                    onClick = {
                        when (gpsState) {
                            is GpsUiState.Ready -> {
                                latText = String.format(Locale.US, "%.6f", gpsState.coordinates.latitude)
                                lngText = String.format(Locale.US, "%.6f", gpsState.coordinates.longitude)
                                errorMsg = null
                            }
                            is GpsUiState.PermissionRequired -> {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            else -> onRefreshGps()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("capture_current_gps_button")
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (gpsState) {
                            is GpsUiState.Ready -> "Gunakan Koordinat GPS Saat Ini"
                            is GpsUiState.Loading -> "Mencari Sinyal GPS..."
                            is GpsUiState.PermissionRequired -> "Izinkan & Ambil GPS Saat Ini"
                            else -> "Segarkan Sinyal GPS Perangkat"
                        }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = {
                            latText = it
                            errorMsg = null
                        },
                        label = { Text("Latitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("location_lat_input")
                    )
                    OutlinedTextField(
                        value = lngText,
                        onValueChange = {
                            lngText = it
                            errorMsg = null
                        },
                        label = { Text("Longitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("location_lng_input")
                    )
                }

                OutlinedTextField(
                    value = radiusText,
                    onValueChange = { radiusText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Radius Toleransi (Meter)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_radius_input")
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(50, 100, 200, 500).forEach { preset ->
                        FilterChip(
                            selected = radiusText.toIntOrNull() == preset,
                            onClick = { radiusText = preset.toString() },
                            label = { Text("${preset} m") }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aktifkan Lokasi Ini", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = setAsActive,
                        onCheckedChange = { setAsActive = it }
                    )
                }

                errorMsg?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.trim().replace(',', '.').toDoubleOrNull()
                    val lng = lngText.trim().replace(',', '.').toDoubleOrNull()
                    val rad = radiusText.toIntOrNull() ?: 100
                    if (name.isBlank() || lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
                        errorMsg = "Nama lokasi dan koordinat Latitude (-90..90) / Longitude (-180..180) harus valid."
                        return@Button
                    }
                    onSave(name, addressNote, lat, lng, rad, setAsActive)
                },
                modifier = Modifier.testTag("save_location_dialog_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun AdminSettingsDialog(
    currentConfig: AdminConfigEntity,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, Int, Int, Int, Boolean) -> Unit
) {
    var pinText by remember { mutableStateOf(currentConfig.adminPin) }
    var requirePin by remember { mutableStateOf(currentConfig.requirePinForAdmin) }
    var hourText by remember { mutableStateOf(currentConfig.workStartHour.toString().padStart(2, '0')) }
    var minuteText by remember { mutableStateOf(currentConfig.workStartMinute.toString().padStart(2, '0')) }
    var toleranceText by remember { mutableStateOf(currentConfig.lateToleranceMinutes.toString()) }
    var strictRadius by remember { mutableStateOf(currentConfig.strictRadiusMode) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan Jam Kerja & Admin") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Jam Masuk (0-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { minuteText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Menit (0-59)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = toleranceText,
                    onValueChange = { toleranceText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Toleransi Terlambat (Menit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mode Radius Ketat", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Tolak absen jika berada di luar radius GPS",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = strictRadius,
                        onCheckedChange = { strictRadius = it }
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kunci Menu Admin dgn PIN", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Lindungi Rekap, Karyawan & Lokasi GPS",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = requirePin,
                        onCheckedChange = { requirePin = it }
                    )
                }

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        pinText = it.filter { ch -> ch.isDigit() }.take(8)
                        errorMsg = null
                    },
                    label = { Text("PIN Admin (min. 4 digit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinText.length < 4) {
                        errorMsg = "PIN Admin minimal 4 digit."
                        return@Button
                    }
                    val h = (hourText.toIntOrNull() ?: 8).coerceIn(0, 23)
                    val m = (minuteText.toIntOrNull() ?: 0).coerceIn(0, 59)
                    val tol = (toleranceText.toIntOrNull() ?: 15).coerceIn(0, 120)
                    onSave(pinText, requirePin, h, m, tol, strictRadius)
                },
                modifier = Modifier.testTag("save_admin_settings_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
