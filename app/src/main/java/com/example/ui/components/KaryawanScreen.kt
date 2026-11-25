package com.example.ui.components

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AdminConfigEntity
import com.example.data.local.EmployeeEntity
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusPresentBg
import com.example.ui.theme.StatusPresentFg

@Composable
fun KaryawanScreen(
    adminConfig: AdminConfigEntity,
    isAdminUnlocked: Boolean,
    employees: List<EmployeeEntity>,
    searchQuery: String,
    onUnlockAdmin: (String) -> Boolean,
    onLockAdmin: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSaveEmployee: (EmployeeEntity?, String, String, String, Boolean) -> Unit,
    onDeleteEmployee: (EmployeeEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (adminConfig.requirePinForAdmin && !isAdminUnlocked) {
        AdminPinGateCard(
            sectionTitle = "Data Karyawan",
            onUnlock = onUnlockAdmin,
            modifier = modifier
        )
        return
    }

    var showFormDialog by remember { mutableStateOf(false) }
    var editingEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var deletingEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }

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
                        text = "Data Karyawan",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "CRUD data karyawan tersimpan offline di SQLite",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (adminConfig.requirePinForAdmin) {
                    IconButton(onClick = onLockAdmin) {
                        Icon(Icons.Rounded.LockOpen, contentDescription = "Kunci Mode Admin")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Cari NIP, nama, divisi...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("employee_search_input")
                )
                Button(
                    onClick = {
                        editingEmployee = null
                        showFormDialog = true
                    },
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("add_employee_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Tambah")
                }
            }
        }

        if (employees.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Groups,
                    title = "Belum Ada Karyawan",
                    subtitle = "Tambahkan daftar karyawan atau staf yang akan melakukan absensi pada aplikasi ini.",
                    actionLabel = "Tambah Karyawan Pertama",
                    onAction = {
                        editingEmployee = null
                        showFormDialog = true
                    },
                    actionTestTag = "empty_add_employee_button"
                )
            }
        } else {
            items(employees, key = { it.id }) { employee ->
                EmployeeCard(
                    employee = employee,
                    onEdit = {
                        editingEmployee = employee
                        showFormDialog = true
                    },
                    onDelete = { deletingEmployee = employee }
                )
            }
        }
    }

    if (showFormDialog) {
        EmployeeFormDialog(
            editing = editingEmployee,
            onDismiss = { showFormDialog = false },
            onSave = { nip, name, division, isActive ->
                onSaveEmployee(editingEmployee, nip, name, division, isActive)
                showFormDialog = false
            }
        )
    }

    deletingEmployee?.let { target ->
        ConfirmDeleteDialog(
            title = "Hapus Karyawan?",
            message = "Karyawan ${target.name} (${target.nip}) akan dihapus dari daftar aktif. Riwayat absensi sebelumnya tetap tersimpan di rekap.",
            onConfirm = {
                onDeleteEmployee(target)
                deletingEmployee = null
            },
            onDismiss = { deletingEmployee = null }
        )
    }
}

@Composable
private fun EmployeeCard(
    employee: EmployeeEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    CleanCard(modifier = Modifier.testTag("employee_card_${employee.id}")) {
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val initials = employee.name
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercase() }
                        .ifBlank { "?" }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column {
                        Text(
                            text = employee.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${employee.nip} • ${employee.division}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (employee.isActive) StatusPresentBg else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (employee.isActive) "Aktif" else "Nonaktif",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (employee.isActive) StatusPresentFg else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_employee_${employee.id}")
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Ubah")
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_employee_${employee.id}")
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

@Composable
private fun EmployeeFormDialog(
    editing: EmployeeEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var nip by remember { mutableStateOf(editing?.nip ?: "") }
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var division by remember { mutableStateOf(editing?.division ?: "") }
    var isActive by remember { mutableStateOf(editing?.isActive ?: true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editing == null) "Tambah Karyawan" else "Ubah Data Karyawan")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nip,
                    onValueChange = {
                        nip = it
                        errorMsg = null
                    },
                    label = { Text("NIP / ID Karyawan") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_nip_input")
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMsg = null
                    },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_name_input")
                )
                OutlinedTextField(
                    value = division,
                    onValueChange = { division = it },
                    label = { Text("Jabatan / Divisi") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_division_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Status Karyawan Aktif", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        modifier = Modifier.testTag("employee_active_switch")
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
                    if (nip.isBlank() || name.isBlank()) {
                        errorMsg = "NIP dan Nama Lengkap wajib diisi."
                        return@Button
                    }
                    onSave(nip, name, division, isActive)
                },
                modifier = Modifier.testTag("save_employee_dialog_button")
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
