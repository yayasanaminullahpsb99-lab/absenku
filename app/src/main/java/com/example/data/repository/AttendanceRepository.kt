package com.example.data.repository

import com.example.data.local.AdminConfigDao
import com.example.data.local.AdminConfigEntity
import com.example.data.local.AttendanceDao
import com.example.data.local.AttendanceEntity
import com.example.data.local.AttendanceStatus
import com.example.data.local.EmployeeDao
import com.example.data.local.EmployeeEntity
import com.example.data.local.GpsLocationDao
import com.example.data.local.GpsLocationEntity
import com.example.data.location.GpsCoordinates
import com.example.data.location.GpsLocationTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface CheckActionResult {
    data class Success(val message: String, val record: AttendanceEntity) : CheckActionResult
    data class Error(val message: String) : CheckActionResult
}

class AttendanceRepository(
    private val employeeDao: EmployeeDao,
    private val gpsLocationDao: GpsLocationDao,
    private val attendanceDao: AttendanceDao,
    private val adminConfigDao: AdminConfigDao
) {
    val allEmployees: Flow<List<EmployeeEntity>> = employeeDao.observeAll()
    val activeEmployees: Flow<List<EmployeeEntity>> = employeeDao.observeActive()
    val allLocations: Flow<List<GpsLocationEntity>> = gpsLocationDao.observeAll()
    val activeLocation: Flow<GpsLocationEntity?> = gpsLocationDao.observeActiveLocation()
    val allAttendance: Flow<List<AttendanceEntity>> = attendanceDao.observeAll()
    val adminConfig: Flow<AdminConfigEntity> = adminConfigDao.observeConfig().map {
        it ?: AdminConfigEntity()
    }

    suspend fun ensureDefaultConfig() {
        if (adminConfigDao.getConfig() == null) {
            adminConfigDao.upsert(AdminConfigEntity())
        }
    }

    // --- Employee CRUD ---
    suspend fun createEmployee(nip: String, name: String, division: String, isActive: Boolean = true): Long {
        return employeeDao.insert(
            EmployeeEntity(
                nip = nip.trim(),
                name = name.trim(),
                division = division.trim(),
                isActive = isActive
            )
        )
    }

    suspend fun updateEmployee(employee: EmployeeEntity) {
        employeeDao.update(
            employee.copy(
                nip = employee.nip.trim(),
                name = employee.name.trim(),
                division = employee.division.trim()
            )
        )
    }

    suspend fun deleteEmployee(employee: EmployeeEntity) {
        employeeDao.delete(employee)
    }

    // --- GPS Location CRUD ---
    suspend fun createLocation(
        name: String,
        addressNote: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        setAsActive: Boolean
    ): Long {
        val currentActive = gpsLocationDao.getActiveLocation()
        val shouldActivate = setAsActive || currentActive == null
        if (shouldActivate) {
            gpsLocationDao.deactivateAll()
        }
        return gpsLocationDao.insert(
            GpsLocationEntity(
                name = name.trim(),
                addressNote = addressNote.trim(),
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters.coerceIn(10, 10000),
                isActive = shouldActivate,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateLocation(location: GpsLocationEntity, setAsActive: Boolean) {
        if (setAsActive) {
            gpsLocationDao.deactivateAll()
        }
        gpsLocationDao.update(
            location.copy(
                name = location.name.trim(),
                addressNote = location.addressNote.trim(),
                radiusMeters = location.radiusMeters.coerceIn(10, 10000),
                isActive = setAsActive || location.isActive,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun activateLocation(locationId: Long) {
        gpsLocationDao.setActiveLocation(locationId)
    }

    suspend fun deleteLocation(location: GpsLocationEntity) {
        gpsLocationDao.delete(location)
    }

    // --- Attendance CRUD ---
    suspend fun createManualAttendance(
        employee: EmployeeEntity,
        dateKey: String,
        checkInTimeMillis: Long?,
        checkOutTimeMillis: Long?,
        locationName: String,
        status: AttendanceStatus,
        note: String
    ): Long {
        val now = System.currentTimeMillis()
        return attendanceDao.insert(
            AttendanceEntity(
                employeeId = employee.id,
                employeeName = employee.name,
                employeeNip = employee.nip,
                employeeDivision = employee.division,
                dateKey = dateKey,
                checkInTimeMillis = checkInTimeMillis,
                checkOutTimeMillis = checkOutTimeMillis,
                locationName = locationName.ifBlank { "Input Admin" },
                status = status.code,
                note = note.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateAttendance(record: AttendanceEntity) {
        attendanceDao.update(
            record.copy(
                note = record.note.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteAttendance(record: AttendanceEntity) {
        attendanceDao.delete(record)
    }

    // --- Check-In / Check-Out Business Logic ---
    suspend fun checkIn(
        employee: EmployeeEntity,
        gps: GpsCoordinates?,
        note: String = ""
    ): CheckActionResult {
        val activeLoc = gpsLocationDao.getActiveLocation()
            ?: return CheckActionResult.Error("Lokasi GPS belum diatur oleh Admin.")

        if (gps == null) {
            return CheckActionResult.Error("Koordinat GPS saat ini belum tersedia. Ketuk segarkan GPS.")
        }

        val config = adminConfigDao.getConfig() ?: AdminConfigEntity()
        val distance = GpsLocationTracker.distanceInMeters(
            gps.latitude,
            gps.longitude,
            activeLoc.latitude,
            activeLoc.longitude
        )

        val withinRadius = distance <= activeLoc.radiusMeters.toFloat()
        if (config.strictRadiusMode && !withinRadius) {
            return CheckActionResult.Error(
                "Anda berada di luar radius lokasi (${distance.toInt()} m dari ${activeLoc.name}, maks ${activeLoc.radiusMeters} m)."
            )
        }

        val now = System.currentTimeMillis()
        val dateKey = formatDateKey(now)
        val existing = attendanceDao.getByEmployeeAndDate(employee.id, dateKey)
        if (existing?.checkInTimeMillis != null) {
            return CheckActionResult.Error("${employee.name} sudah melakukan absen masuk hari ini.")
        }

        val isLate = isLateCheckIn(now, config)
        val computedStatus = if (isLate) AttendanceStatus.TERLAMBAT else AttendanceStatus.HADIR
        val autoNote = buildString {
            if (!withinRadius) {
                append("Di luar radius (${distance.toInt()} m)")
            }
            if (note.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(note.trim())
            }
        }

        val entity = if (existing != null) {
            existing.copy(
                checkInTimeMillis = now,
                checkInLat = gps.latitude,
                checkInLng = gps.longitude,
                checkInDistanceMeters = distance,
                locationName = activeLoc.name,
                status = computedStatus.code,
                note = autoNote.ifBlank { existing.note },
                updatedAt = now
            )
        } else {
            AttendanceEntity(
                employeeId = employee.id,
                employeeName = employee.name,
                employeeNip = employee.nip,
                employeeDivision = employee.division,
                dateKey = dateKey,
                checkInTimeMillis = now,
                checkInLat = gps.latitude,
                checkInLng = gps.longitude,
                checkInDistanceMeters = distance,
                locationName = activeLoc.name,
                status = computedStatus.code,
                note = autoNote,
                createdAt = now,
                updatedAt = now
            )
        }

        val insertedId = attendanceDao.insert(entity)
        val saved = entity.copy(id = if (entity.id == 0L) insertedId else entity.id)
        val statusMsg = if (isLate) "Absen masuk tercatat (Terlambat)" else "Absen masuk berhasil dicatat"
        return CheckActionResult.Success(statusMsg, saved)
    }

    suspend fun checkOut(
        employee: EmployeeEntity,
        gps: GpsCoordinates?
    ): CheckActionResult {
        val activeLoc = gpsLocationDao.getActiveLocation()
            ?: return CheckActionResult.Error("Lokasi GPS belum diatur oleh Admin.")

        if (gps == null) {
            return CheckActionResult.Error("Koordinat GPS saat ini belum tersedia. Ketuk segarkan GPS.")
        }

        val config = adminConfigDao.getConfig() ?: AdminConfigEntity()
        val distance = GpsLocationTracker.distanceInMeters(
            gps.latitude,
            gps.longitude,
            activeLoc.latitude,
            activeLoc.longitude
        )

        val withinRadius = distance <= activeLoc.radiusMeters.toFloat()
        if (config.strictRadiusMode && !withinRadius) {
            return CheckActionResult.Error(
                "Anda berada di luar radius lokasi (${distance.toInt()} m dari ${activeLoc.name}, maks ${activeLoc.radiusMeters} m)."
            )
        }

        val now = System.currentTimeMillis()
        val dateKey = formatDateKey(now)
        val existing = attendanceDao.getByEmployeeAndDate(employee.id, dateKey)
            ?: return CheckActionResult.Error("${employee.name} belum melakukan absen masuk hari ini.")

        if (existing.checkOutTimeMillis != null) {
            return CheckActionResult.Error("${employee.name} sudah melakukan absen pulang hari ini.")
        }

        val updatedNote = if (!withinRadius) {
            if (existing.note.isBlank()) "Pulang di luar radius (${distance.toInt()} m)"
            else "${existing.note} • Pulang di luar radius (${distance.toInt()} m)"
        } else {
            existing.note
        }

        val updated = existing.copy(
            checkOutTimeMillis = now,
            checkOutLat = gps.latitude,
            checkOutLng = gps.longitude,
            checkOutDistanceMeters = distance,
            note = updatedNote,
            updatedAt = now
        )
        attendanceDao.update(updated)
        return CheckActionResult.Success("Absen pulang berhasil dicatat", updated)
    }

    // --- Admin Config ---
    suspend fun updateAdminConfig(config: AdminConfigEntity) {
        adminConfigDao.upsert(config.copy(id = 1))
    }

    private fun isLateCheckIn(timestampMillis: Long, config: AdminConfigEntity): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val limitCal = Calendar.getInstance().apply {
            timeInMillis = timestampMillis
            set(Calendar.HOUR_OF_DAY, config.workStartHour)
            set(Calendar.MINUTE, config.workStartMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, config.lateToleranceMinutes)
        }
        return cal.after(limitCal)
    }

    companion object {
        fun formatDateKey(millis: Long): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
        }

        fun formatTime(millis: Long?): String {
            if (millis == null) return "--:--"
            return SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(millis))
        }

        fun formatDisplayDate(dateKey: String): String {
            return runCatching {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey)
                if (parsed != null) {
                    SimpleDateFormat("EEE, dd MMM yyyy", Locale("id", "ID")).format(parsed)
                } else dateKey
            }.getOrDefault(dateKey)
        }

        fun buildCsvRecap(records: List<AttendanceEntity>): String {
            val sb = StringBuilder()
            sb.appendLine("Tanggal,NIP,Nama,Divisi,Status,Masuk,Pulang,Lokasi,JarakMasuk(m),Catatan")
            records.forEach { r ->
                val dist = r.checkInDistanceMeters?.toInt()?.toString() ?: "-"
                val cleanNote = r.note.replace("\"", "'").replace("\n", " ")
                sb.appendLine(
                    "\"${r.dateKey}\",\"${r.employeeNip}\",\"${r.employeeName}\",\"${r.employeeDivision}\",\"${AttendanceStatus.fromCode(r.status).label}\",\"${formatTime(r.checkInTimeMillis)}\",\"${formatTime(r.checkOutTimeMillis)}\",\"${r.locationName}\",\"$dist\",\"$cleanNote\""
                )
            }
            return sb.toString()
        }
    }
}
