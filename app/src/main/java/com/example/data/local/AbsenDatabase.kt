package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        EmployeeEntity::class,
        GpsLocationEntity::class,
        AttendanceEntity::class,
        AdminConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AbsenDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun gpsLocationDao(): GpsLocationDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun adminConfigDao(): AdminConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AbsenDatabase? = null

        fun getInstance(context: Context): AbsenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AbsenDatabase::class.java,
                    "absenku_offline.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
