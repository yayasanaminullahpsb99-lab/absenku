package com.example.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AdminConfigEntity
import com.example.data.local.AttendanceEntity
import com.example.data.local.AttendanceStatus
import com.example.data.local.EmployeeEntity
import com.example.data.local.GpsLocationEntity
import com.example.data.location.GpsCoordinates
import com.example.data.location.GpsFetchResult
import com.example.data.location.GpsLocationTracker
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.CheckActionResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AbsenTab(val label: String) {
    ABSENSI("Absensi"),
    REKAP("Rekap"),
    KARYAWAN("Karyawan"),
    LOKASI("Lokasi GPS")
}

enum class RecapDateFilter(val label: String) {
    TODAY("Hari Ini"),
    ALL("Semua Tanggal")
}

sealed interface GpsUiState {
    data object Idle : GpsUiState
    data object Loading : GpsUiState
    data class Ready(val coordinates: GpsCoordinates) : GpsUiState
    data object PermissionRequired : GpsUiState
    data object LocationDisabled : GpsUiState
    data class Error(val message: String) : GpsUiState
}

data class RecapSummary(
    val totalRecords: Int = 0,
    val presentCount: Int = 0,
    val lateCount: Int = 0,
    val permitOrSickCount: Int = 0,
    val absentCount: Int = 0
)

class AbsenViewModel(
    private val repository: AttendanceRepository,
    private val locationTracker: GpsLocationTracker
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(AbsenTab.ABSENSI)
    val selectedTab: StateFlow<AbsenTab> = _selectedTab.asStateFlow()

    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMillis: StateFlow<Long> = _currentTimeMillis.asStateFlow()

    private val _gpsState = MutableStateFlow<GpsUiState>(GpsUiState.Idle)
    val gpsState: StateFlow<GpsUiState> = _gpsState.asStateFlow()

    private val _selectedEmployeeId = MutableStateFlow<Long?>(null)
    val selectedEmployeeId: StateFlow<Long?> = _selectedEmployeeId.asStateFlow()

    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    private val _recapDateFilter = MutableStateFlow(RecapDateFilter.TODAY)
    val recapDateFilter: StateFlow<RecapDateFilter> = _recapDateFilter.asStateFlow()

    private val _recapStatusFilter = MutableStateFlow<AttendanceStatus?>(null)
    val recapStatusFilter: StateFlow<AttendanceStatus?> = _recapStatusFilter.asStateFlow()

    private val _recapSearchQuery = MutableStateFlow("")
    val recapSearchQuery: StateFlow<String> = _recapSearchQuery.asStateFlow()

    private val _employeeSearchQuery = MutableStateFlow("")
    val employeeSearchQuery: StateFlow<String> = _employeeSearchQuery.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    val allEmployees: StateFlow<List<EmployeeEntity>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeEmployees: StateFlow<List<EmployeeEntity>> = repository.activeEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLocations: StateFlow<List<GpsLocationEntity>> = repository.allLocations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeLocation: StateFlow<GpsLocationEntity?> = repository.activeLocation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allAttendance: StateFlow<List<AttendanceEntity>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminConfig: StateFlow<AdminConfigEntity> = repository.adminConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminConfigEntity())

    val filteredEmployees: StateFlow<List<EmployeeEntity>> = combine(
        allEmployees,
        _employeeSearchQuery
    ) { employees, query ->
        val q = query.trim().lowercase()
        if (q.isEmpty()) employees
        else employees.filter {
            it.name.lowercase().contains(q) ||
                it.nip.lowercase().contains(q) ||
                it.division.lowercase().contains(q)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredRecapRecords: StateFlow<List<AttendanceEntity>> = combine(
        allAttendance,
        _recapDateFilter,
        _recapStatusFilter,
        _recapSearchQuery,
        _currentTimeMillis
    ) { records, dateFilter, statusFilter, query, now ->
        val todayKey = AttendanceRepository.formatDateKey(now)
        val q = query.trim().lowercase()
        records.filter { record ->
            val dateMatches = when (dateFilter) {
                RecapDateFilter.TODAY -> record.dateKey == todayKey
                RecapDateFilter.ALL -> true
            }
            val statusMatches = statusFilter == null || record.status == statusFilter.code
            val queryMatches = q.isEmpty() ||
                record.employeeName.lowercase().contains(q) ||
                record.employeeNip.lowercase().contains(q) ||
                record.employeeDivision.lowercase().contains(q) ||
                record.locationName.lowercase().contains(q)
            dateMatches && statusMatches && queryMatches
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recapSummary: StateFlow<RecapSummary> = filteredRecapRecords.combine(_recapDateFilter) { list, _ ->
        RecapSummary(
            totalRecords = list.size,
            presentCount = list.count { it.status == AttendanceStatus.HADIR.code },
            lateCount = list.count { it.status == AttendanceStatus.TERLAMBAT.code },
            permitOrSickCount = list.count {
                it.status == AttendanceStatus.IZIN.code || it.status == AttendanceStatus.SAKIT.code
            },
            absentCount = list.count { it.status == AttendanceStatus.ALPHA.code }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecapSummary())

    val todayAttendanceList: StateFlow<List<AttendanceEntity>> = combine(
        allAttendance,
        _currentTimeMillis
    ) { records, now ->
        val todayKey = AttendanceRepository.formatDateKey(now)
        records.filter { it.dateKey == todayKey }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedEmployee: StateFlow<EmployeeEntity?> = combine(
        activeEmployees,
        _selectedEmployeeId
    ) { employees, selectedId ->
        employees.firstOrNull { it.id == selectedId } ?: employees.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            repository.ensureDefaultConfig()
        }
        viewModelScope.launch {
            while (true) {
                _currentTimeMillis.value = System.currentTimeMillis()
                delay(1000L)
            }
        }
        refreshGpsLocation()
    }

    fun selectTab(tab: AbsenTab) {
        _selectedTab.value = tab
    }

    fun selectEmployee(employeeId: Long) {
        _selectedEmployeeId.value = employeeId
    }

    fun setRecapDateFilter(filter: RecapDateFilter) {
        _recapDateFilter.value = filter
    }

    fun setRecapStatusFilter(status: AttendanceStatus?) {
        _recapStatusFilter.value = status
    }

    fun setRecapSearchQuery(query: String) {
        _recapSearchQuery.value = query
    }

    fun setEmployeeSearchQuery(query: String) {
        _employeeSearchQuery.value = query
    }

    fun refreshGpsLocation() {
        viewModelScope.launch {
            _gpsState.value = GpsUiState.Loading
            when (val result = locationTracker.getCurrentLocation()) {
                is GpsFetchResult.Success -> _gpsState.value = GpsUiState.Ready(result.coordinates)
                GpsFetchResult.PermissionMissing -> _gpsState.value = GpsUiState.PermissionRequired
                GpsFetchResult.LocationDisabled -> _gpsState.value = GpsUiState.LocationDisabled
                is GpsFetchResult.Failure -> _gpsState.value = GpsUiState.Error(result.message)
            }
        }
    }

    fun performCheckIn(note: String = "") {
        val employee = selectedEmployee.value
        if (employee == null) {
            emitMessage("Pilih atau tambahkan data karyawan terlebih dahulu.")
            return
        }
        val coords = (_gpsState.value as? GpsUiState.Ready)?.coordinates
        viewModelScope.launch {
            when (val res = repository.checkIn(employee, coords, note)) {
                is CheckActionResult.Success -> emitMessage(res.message)
                is CheckActionResult.Error -> emitMessage(res.message)
            }
        }
    }

    fun performCheckOut() {
        val employee = selectedEmployee.value
        if (employee == null) {
            emitMessage("Pilih atau tambahkan data karyawan terlebih dahulu.")
            return
        }
        val coords = (_gpsState.value as? GpsUiState.Ready)?.coordinates
        viewModelScope.launch {
            when (val res = repository.checkOut(employee, coords)) {
                is CheckActionResult.Success -> emitMessage(res.message)
                is CheckActionResult.Error -> emitMessage(res.message)
            }
        }
    }

    // --- Admin Auth ---
    fun unlockAdmin(pinInput: String): Boolean {
        val valid = pinInput == adminConfig.value.adminPin
        if (valid) {
            _isAdminUnlocked.value = true
            emitMessage("Mode Admin aktif")
        }
        return valid
    }

    fun lockAdmin() {
        _isAdminUnlocked.value = false
    }

    // --- Employee CRUD ---
    fun saveEmployee(editing: EmployeeEntity?, nip: String, name: String, division: String, isActive: Boolean) {
        if (nip.isBlank() || name.isBlank()) {
            emitMessage("NIP dan Nama karyawan wajib diisi.")
            return
        }
        viewModelScope.launch {
            if (editing == null) {
                val id = repository.createEmployee(nip, name, division.ifBlank { "Umum" }, isActive)
                if (_selectedEmployeeId.value == null) _selectedEmployeeId.value = id
                emitMessage("Karyawan \"$name\" ditambahkan.")
            } else {
                repository.updateEmployee(
                    editing.copy(
                        nip = nip,
                        name = name,
                        division = division.ifBlank { "Umum" },
                        isActive = isActive
                    )
                )
                emitMessage("Data \"$name\" diperbarui.")
            }
        }
    }

    fun deleteEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            repository.deleteEmployee(employee)
            emitMessage("Karyawan \"${employee.name}\" dihapus.")
        }
    }

    // --- GPS Location CRUD ---
    fun saveLocation(
        editing: GpsLocationEntity?,
        name: String,
        addressNote: String,
        latitude: Double?,
        longitude: Double?,
        radiusMeters: Int,
        setAsActive: Boolean
    ) {
        if (name.isBlank() || latitude == null || longitude == null) {
            emitMessage("Nama lokasi dan koordinat Latitude/Longitude wajib valid.")
            return
        }
        viewModelScope.launch {
            if (editing == null) {
                repository.createLocation(name, addressNote, latitude, longitude, radiusMeters, setAsActive)
                emitMessage("Lokasi GPS \"$name\" disimpan.")
            } else {
                repository.updateLocation(
                    editing.copy(
                        name = name,
                        addressNote = addressNote,
                        latitude = latitude,
                        longitude = longitude,
                        radiusMeters = radiusMeters
                    ),
                    setAsActive = setAsActive
                )
                emitMessage("Lokasi GPS \"$name\" diperbarui.")
            }
        }
    }

    fun activateLocation(location: GpsLocationEntity) {
        viewModelScope.launch {
            repository.activateLocation(location.id)
            emitMessage("Lokasi aktif diubah ke \"${location.name}\".")
        }
    }

    fun deleteLocation(location: GpsLocationEntity) {
        viewModelScope.launch {
            repository.deleteLocation(location)
            emitMessage("Lokasi \"${location.name}\" dihapus.")
        }
    }

    // --- Attendance CRUD (Admin) ---
    fun createManualAttendance(
        employee: EmployeeEntity,
        dateKey: String,
        checkInMillis: Long?,
        checkOutMillis: Long?,
        locationName: String,
        status: AttendanceStatus,
        note: String
    ) {
        viewModelScope.launch {
            repository.createManualAttendance(
                employee = employee,
                dateKey = dateKey,
                checkInTimeMillis = checkInMillis,
                checkOutTimeMillis = checkOutMillis,
                locationName = locationName,
                status = status,
                note = note
            )
            emitMessage("Data absensi ${employee.name} ditambahkan.")
        }
    }

    fun updateAttendanceRecord(record: AttendanceEntity) {
        viewModelScope.launch {
            repository.updateAttendance(record)
            emitMessage("Data absensi ${record.employeeName} diperbarui.")
        }
    }

    fun deleteAttendanceRecord(record: AttendanceEntity) {
        viewModelScope.launch {
            repository.deleteAttendance(record)
            emitMessage("Data absensi ${record.employeeName} dihapus.")
        }
    }

    // --- Admin Settings ---
    fun updateAdminSettings(
        newPin: String,
        requirePin: Boolean,
        workStartHour: Int,
        workStartMinute: Int,
        lateToleranceMinutes: Int,
        strictRadiusMode: Boolean
    ) {
        val cleanPin = newPin.trim()
        if (cleanPin.length < 4) {
            emitMessage("PIN Admin minimal 4 digit.")
            return
        }
        viewModelScope.launch {
            repository.updateAdminConfig(
                AdminConfigEntity(
                    id = 1,
                    adminPin = cleanPin,
                    requirePinForAdmin = requirePin,
                    workStartHour = workStartHour.coerceIn(0, 23),
                    workStartMinute = workStartMinute.coerceIn(0, 59),
                    lateToleranceMinutes = lateToleranceMinutes.coerceIn(0, 120),
                    strictRadiusMode = strictRadiusMode
                )
            )
            emitMessage("Pengaturan Admin disimpan.")
        }
    }

    fun buildExportCsv(): String = AttendanceRepository.buildCsvRecap(filteredRecapRecords.value)

    private fun emitMessage(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    class Factory(
        private val repository: AttendanceRepository,
        private val locationTracker: GpsLocationTracker
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AbsenViewModel(repository, locationTracker) as T
        }
    }
}
