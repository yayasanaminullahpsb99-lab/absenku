package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY isActive DESC, name ASC")
    fun observeAll(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE isActive = 1 ORDER BY name ASC")
    fun observeActive(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(employee: EmployeeEntity): Long

    @Update
    suspend fun update(employee: EmployeeEntity)

    @Delete
    suspend fun delete(employee: EmployeeEntity)
}

@Dao
interface GpsLocationDao {
    @Query("SELECT * FROM gps_locations ORDER BY isActive DESC, updatedAt DESC")
    fun observeAll(): Flow<List<GpsLocationEntity>>

    @Query("SELECT * FROM gps_locations WHERE isActive = 1 LIMIT 1")
    fun observeActiveLocation(): Flow<GpsLocationEntity?>

    @Query("SELECT * FROM gps_locations WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveLocation(): GpsLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: GpsLocationEntity): Long

    @Update
    suspend fun update(location: GpsLocationEntity)

    @Delete
    suspend fun delete(location: GpsLocationEntity)

    @Query("UPDATE gps_locations SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE gps_locations SET isActive = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun activateById(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Transaction
    suspend fun setActiveLocation(id: Long) {
        deactivateAll()
        activateById(id)
    }
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY dateKey DESC, COALESCE(checkInTimeMillis, createdAt) DESC")
    fun observeAll(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE dateKey = :dateKey ORDER BY COALESCE(checkInTimeMillis, createdAt) DESC")
    fun observeByDate(dateKey: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId AND dateKey = :dateKey LIMIT 1")
    suspend fun getByEmployeeAndDate(employeeId: Long, dateKey: String): AttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AttendanceEntity): Long

    @Update
    suspend fun update(record: AttendanceEntity)

    @Delete
    suspend fun delete(record: AttendanceEntity)
}

@Dao
interface AdminConfigDao {
    @Query("SELECT * FROM admin_config WHERE id = 1 LIMIT 1")
    fun observeConfig(): Flow<AdminConfigEntity?>

    @Query("SELECT * FROM admin_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): AdminConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: AdminConfigEntity)
}
