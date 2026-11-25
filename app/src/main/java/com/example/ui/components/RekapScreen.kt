package com.example.ui.components

import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AdminConfigEntity
import com.example.data.local.AttendanceEntity
import com.example.data.local.AttendanceStatus
import com.example.data.local.EmployeeEntity
import com.example.data.local.GpsLocationEntity
import com.example.data.repository.AttendanceRepository
import com.example.ui.state.RecapDateFilter
import com.example.ui.state.RecapSummary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusAbsentFg
import com.example.ui.theme.StatusLateFg
import com.example.ui.theme.StatusPermitFg
import com.example.ui.theme.StatusPresentFg
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RekapScreen(
    adminConfig: AdminConfigEntity,
    isAdminUnlocked: Boolean,
    records: List<AttendanceEntity>,
    summary: RecapSummary,
    employees: List<EmployeeEntity>,
    locations: List<GpsLocationEntity>,
    dateFilter: RecapDateFilter,
    statusFilter: AttendanceStatus?,
    searchQuery: String,
    onUnlockAdmin: (String) -> Boolean,
    onLockAdmin: () -> Unit,
    onDateFilterChange: (RecapDateFilter) -> Unit,
    onStatusFilterChange: (AttendanceStatus?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCreateManualAttendance: (
        EmployeeEntity,
        String,
        Long?,
        Long?,
        String,
        AttendanceStatus,
        String
    ) -> Unit,
    onUpdateAttendance: (AttendanceEntity) -> Unit,
    onDeleteAttendance: (AttendanceEntity) -> Unit,
    onBuildExportCsv: () -> String,
    modifier: Modifier = Modifier
) {
    if (adminConfig.requirePinForAdmin && !isAdminUnlocked) {
        AdminPinGateCard(
            sectionTitle = "Rekap Absensi",
            onUnlock = onUnlockAdmin,
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<AttendanceEntity?>(null) }
    var deletingRecord by remember { mutableStateOf<AttendanceEntity?>(null) }

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
                Column {
                    Text(
                        text = "Rekap Absensi Admin",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Kelola CRUD & ekspor laporan offline",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (adminConfig.requirePinForAdmin) {
                        IconButton(
                            onClick = onLockAdmin,
                            modifier = Modifier.testTag("lock_admin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LockOpen,
                                contentDescription = "Kunci Mode Admin"
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val csv = onBuildExportCsv()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Rekap Absensi Offline AbsenKu")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Rekap Absensi"))
                        },
                        enabled = records.isNotEmpty(),
                        modifier = Modifier.testTag("export_recap_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Bagikan Rekap CSV"
                        )
                    }
                }
            }
        }

        // Summary Metric Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricTile(
                        label = "Hadir",
                        value = summary.presentCount.toString(),
                        accentColor = StatusPresentFg,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Terlambat",
                        value = summary.lateCount.toString(),
                        accentColor = StatusLateFg,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricTile(
                        label = "Izin / Sakit",
                        value = summary.permitOrSickCount.toString(),
                        accentColor = StatusPermitFg,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Alpha",
                        value = summary.absentCount.toString(),
                        accentColor = StatusAbsentFg,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Search & Add Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Cari nama, NIP, lokasi...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("recap_search_input")
                )
                Button(
                    onClick = { showAddDialog = true },
                    enabled = employees.isNotEmpty(),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("add_manual_attendance_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Tambah")
                }
            }
        }

        // Filters
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RecapDateFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = dateFilter == filter,
                        onClick = { onDateFilterChange(filter) },
                        label = { Text(filter.label) }
                    )
                }
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { onStatusFilterChange(null) },
                    label = { Text("Semua Status") }
                )
                AttendanceStatus.entries.forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = {
                            onStatusFilterChange(if (statusFilter == st) null else st)
                        },
                        label = { Text(st.label) }
                    )
                }
            }
        }

        if (records.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Assessment,
                    title = "Data Rekap Kosong",
                    subtitle = if (employees.isEmpty()) {
                        "Tambahkan karyawan terlebih dahulu sebelum mencatat atau menambah rekap absensi."
                    } else {
                        "Belum ada catatan absensi untuk filter yang dipilih. Anda dapat menambahkan data manual melalui tombol Tambah."
                    },
                    actionLabel = if (employees.isNotEmpty()) "Tambah Absen Manual" else null,
                    onAction = if (employees.isNotEmpty()) {
                        { showAddDialog = true }
                    } else null,
                    actionTestTag = "empty_add_attendance_button"
                )
            }
        } else {
            items(records, key = { it.id }) { record ->
                RecapRecordCard(
                    record = record,
                    onEdit = { editingRecord = record },
                    onDelete = { deletingRecord = record }
                )
            }
        }
    }

    if (showAddDialog) {
        AttendanceFormDialog(
            title = "Tambah Absensi Manual",
            employees = employees,
            locations = locations,
            existingRecord = null,
            onDismiss = { showAddDialog = false },
            onConfirmCreate = { emp, dateKey, inMillis, outMillis, locName, status, note ->
                onCreateManualAttendance(emp, dateKey, inMillis, outMillis, locName, status, note)
                showAddDialog = false
            },
            onConfirmUpdate = {}
        )
    }

    editingRecord?.let { target ->
        AttendanceFormDialog(
            title = "Ubah Data Absensi",
            employees = employees,
            locations = locations,
            existingRecord = target,
            onDismiss = { editingRecord = null },
            onConfirmCreate = { _, _, _, _, _, _, _ -> },
            onConfirmUpdate = { updated ->
                onUpdateAttendance(updated)
                editingRecord = null
            }
        )
    }

    deletingRecord?.let { target ->
        ConfirmDeleteDialog(
            title = "Hapus Catatan Absensi?",
            message = "Data absensi ${target.employeeName} tanggal ${AttendanceRepository.formatDisplayDate(target.dateKey)} akan dihapus permanen dari SQLite.",
            onConfirm = {
                onDeleteAttendance(target)
                deletingRecord = null
            },
            onDismiss = { deletingRecord = null }
        )
    }
}

@Composable
private fun RecapRecordCard(
    record: AttendanceEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    CleanCard(modifier = Modifier.testTag("recap_card_${record.id}")) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.employeeName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${record.employeeNip} • ${record.employeeDivision}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AttendanceStatusBadge(status = AttendanceStatus.fromCode(record.status))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = AttendanceRepository.formatDisplayDate(record.dateKey),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Masuk ${AttendanceRepository.formatTime(record.checkInTimeMillis)}  •  Pulang ${AttendanceRepository.formatTime(record.checkOutTimeMillis)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = JetBrainsMonoFamily
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    val distText = record.checkInDistanceMeters?.let { " (${it.toInt()} m)" } ?: ""
                    Text(
                        text = "Lokasi: ${record.locationName}$distText",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (record.note.isNotBlank()) {
                        Text(
                            text = "Catatan: ${record.note}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_attendance_${record.id}")
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Ubah")
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_attendance_${record.id}")
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AttendanceFormDialog(
    title: String,
    employees: List<EmployeeEntity>,
    locations: List<GpsLocationEntity>,
    existingRecord: AttendanceEntity?,
    onDismiss: () -> Unit,
    onConfirmCreate: (
        EmployeeEntity,
        String,
        Long?,
        Long?,
        String,
        AttendanceStatus,
        String
    ) -> Unit,
    onConfirmUpdate: (AttendanceEntity) -> Unit
) {
    var selectedEmployee by remember {
        mutableStateOf(
            employees.firstOrNull { it.id == existingRecord?.employeeId } ?: employees.firstOrNull()
        )
    }
    var empExpanded by remember { mutableStateOf(false) }

    var dateKey by remember {
        mutableStateOf(
            existingRecord?.dateKey ?: AttendanceRepository.formatDateKey(System.currentTimeMillis())
        )
    }
    var checkInText by remember {
        mutableStateOf(
            existingRecord?.checkInTimeMillis?.let { AttendanceRepository.formatTime(it) } ?: "08:00"
        )
    }
    var checkOutText by remember {
        mutableStateOf(
            existingRecord?.checkOutTimeMillis?.let { AttendanceRepository.formatTime(it) } ?: ""
        )
    }
    var selectedStatus by remember {
        mutableStateOf(
            existingRecord?.let { AttendanceStatus.fromCode(it.status) } ?: AttendanceStatus.HADIR
        )
    }
    var locationName by remember {
        mutableStateOf(
            existingRecord?.locationName
                ?: locations.firstOrNull { it.isActive }?.name
                ?: "Kantor Pusat"
        )
    }
    var note by remember { mutableStateOf(existingRecord?.note ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (existingRecord == null) {
                    ExposedDropdownMenuBox(
                        expanded = empExpanded,
                        onExpandedChange = { empExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedEmployee?.let { "${it.name} (${it.nip})" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Karyawan") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = empExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = empExpanded,
                            onDismissRequest = { empExpanded = false }
                        ) {
                            employees.forEach { emp ->
                                DropdownMenuItem(
                                    text = { Text("${emp.name} (${emp.nip})") },
                                    onClick = {
                                        selectedEmployee = emp
                                        empExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "${existingRecord.employeeName} (${existingRecord.employeeNip})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                OutlinedTextField(
                    value = dateKey,
                    onValueChange = { dateKey = it },
                    label = { Text("Tanggal (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = checkInText,
                        onValueChange = { checkInText = it },
                        label = { Text("Masuk (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = checkOutText,
                        onValueChange = { checkOutText = it },
                        label = { Text("Pulang (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Status Kehadiran",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AttendanceStatus.entries.forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(st.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Nama Lokasi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan") },
                    modifier = Modifier.fillMaxWidth()
                )

                validationError?.let {
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
                    val parsedDate = runCatching {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            isLenient = false
                        }.parse(dateKey.trim())
                    }.getOrNull()

                    if (parsedDate == null) {
                        validationError = "Format tanggal harus yyyy-MM-dd (contoh: 2026-11-24)."
                        return@Button
                    }

                    val inMillis = if (checkInText.isBlank()) null else parseDateAndTime(dateKey.trim(), checkInText.trim())
                    if (checkInText.isNotBlank() && inMillis == null) {
                        validationError = "Format jam masuk harus HH:mm (contoh: 08:00)."
                        return@Button
                    }

                    val outMillis = if (checkOutText.isBlank()) null else parseDateAndTime(dateKey.trim(), checkOutText.trim())
                    if (checkOutText.isNotBlank() && outMillis == null) {
                        validationError = "Format jam pulang harus HH:mm (contoh: 17:00)."
                        return@Button
                    }

                    if (existingRecord == null) {
                        val emp = selectedEmployee ?: return@Button
                        onConfirmCreate(
                            emp,
                            dateKey.trim(),
                            inMillis,
                            outMillis,
                            locationName,
                            selectedStatus,
                            note
                        )
                    } else {
                        onConfirmUpdate(
                            existingRecord.copy(
                                dateKey = dateKey.trim(),
                                checkInTimeMillis = inMillis,
                                checkOutTimeMillis = outMillis,
                                locationName = locationName.ifBlank { existingRecord.locationName },
                                status = selectedStatus.code,
                                note = note
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_attendance_dialog_button")
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

private fun parseDateAndTime(dateKey: String, timeStr: String): Long? {
    return runCatching {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey) ?: return null
        val parts = timeStr.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        if (hour !in 0..23 || minute !in 0..59) return null
        Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }.getOrNull()
}
