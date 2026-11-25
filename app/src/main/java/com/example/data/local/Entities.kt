package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttendanceStatus(val code: String, val label: String) {
    HADIR("HADIR", "Hadir"),
    TERLAMBAT("TERLAMBAT", "Terlambat"),
    IZIN("IZIN", "Izin"),
    SAKIT("SAKIT", "Sakit"),
    ALPHA("ALPHA", "Alpha");

    companion object {
        fun fromCode(code: String): AttendanceStatus =
            entries.firstOrNull { it.code == code } ?: HADIR
    }
}

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nip: String,
    val name: String,
    val division: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "gps_locations")
data class GpsLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val addressNote: String = "",
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 100,
    val isActive: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = EmployeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["employeeId"]),
        Index(value = ["dateKey"])
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val employeeId: Long?,
    val employeeName: String,
    val employeeNip: String,
    val employeeDivision: String,
    val dateKey: String,
    val checkInTimeMillis: Long? = null,
    val checkOutTimeMillis: Long? = null,
    val checkInLat: Double? = null,
    val checkInLng: Double? = null,
    val checkInDistanceMeters: Float? = null,
    val checkOutLat: Double? = null,
    val checkOutLng: Double? = null,
    val checkOutDistanceMeters: Float? = null,
    val locationName: String = "",
    val status: String = AttendanceStatus.HADIR.code,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_config")
data class AdminConfigEntity(
    @PrimaryKey val id: Int = 1,
    val adminPin: String = "1234",
    val requirePinForAdmin: Boolean = true,
    val workStartHour: Int = 8,
    val workStartMinute: Int = 0,
    val lateToleranceMinutes: Int = 15,
    val strictRadiusMode: Boolean = true
)
