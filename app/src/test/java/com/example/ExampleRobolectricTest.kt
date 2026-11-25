package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AbsenDatabase
import com.example.data.local.AttendanceStatus
import com.example.data.location.GpsCoordinates
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.CheckActionResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AbsenDatabase
    private lateinit var repository: AttendanceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AbsenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AttendanceRepository(
            employeeDao = db.employeeDao(),
            gpsLocationDao = db.gpsLocationDao(),
            attendanceDao = db.attendanceDao(),
            adminConfigDao = db.adminConfigDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app name matches AbsenKu`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AbsenKu", appName)
    }

    @Test
    fun `employee and location CRUD and gps check-in check-out flow`() = runBlocking {
        repository.ensureDefaultConfig()

        // Create Employee
        val empId = repository.createEmployee("EMP001", "Budi Santoso", "Operasional", true)
        val employees = repository.allEmployees.first()
        assertEquals(1, employees.size)
        val employee = employees.first()
        assertEquals("Budi Santoso", employee.name)

        // Update Employee
        repository.updateEmployee(employee.copy(division = "Logistik"))
        assertEquals("Logistik", repository.allEmployees.first().first().division)

        // Create GPS Location (Monas Jakarta)
        val locId = repository.createLocation(
            name = "Kantor Pusat",
            addressNote = "Jl. Medan Merdeka",
            latitude = -6.175392,
            longitude = 106.827153,
            radiusMeters = 100,
            setAsActive = true
        )
        assertTrue(locId > 0)
        val activeLoc = repository.activeLocation.first()
        assertNotNull(activeLoc)
        assertEquals("Kantor Pusat", activeLoc?.name)

        // Check-In outside radius -> Error
        val farGps = GpsCoordinates(
            latitude = -6.200000,
            longitude = 106.827153,
            accuracyMeters = 10f,
            provider = "gps",
            timestampMillis = System.currentTimeMillis()
        )
        val outsideResult = repository.checkIn(employee, farGps)
        assertTrue(outsideResult is CheckActionResult.Error)

        // Check-In inside radius -> Success
        val nearGps = GpsCoordinates(
            latitude = -6.175410,
            longitude = 106.827160,
            accuracyMeters = 8f,
            provider = "gps",
            timestampMillis = System.currentTimeMillis()
        )
        val insideResult = repository.checkIn(employee, nearGps, "Shift pagi")
        assertTrue(insideResult is CheckActionResult.Success)

        // Check-Out inside radius -> Success
        val checkOutResult = repository.checkOut(employee, nearGps)
        assertTrue(checkOutResult is CheckActionResult.Success)

        // Verify Attendance record in SQLite & Update/Delete CRUD
        val records = repository.allAttendance.first()
        assertEquals(1, records.size)
        val record = records.first()
        assertNotNull(record.checkInTimeMillis)
        assertNotNull(record.checkOutTimeMillis)

        repository.updateAttendance(record.copy(status = AttendanceStatus.IZIN.code, note = "Diubah admin"))
        assertEquals("IZIN", repository.allAttendance.first().first().status)

        repository.deleteAttendance(record)
        assertEquals(0, repository.allAttendance.first().size)

        repository.deleteEmployee(employee)
        assertEquals(0, repository.allEmployees.first().size)
    }
}
