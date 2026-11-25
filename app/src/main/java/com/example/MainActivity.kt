package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AbsenDatabase
import com.example.data.location.GpsLocationTracker
import com.example.data.repository.AttendanceRepository
import com.example.ui.components.AbsenKuApp
import com.example.ui.state.AbsenViewModel
import com.example.ui.theme.AbsenKuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AbsenDatabase.getInstance(applicationContext)
        val repository = AttendanceRepository(
            employeeDao = db.employeeDao(),
            gpsLocationDao = db.gpsLocationDao(),
            attendanceDao = db.attendanceDao(),
            adminConfigDao = db.adminConfigDao()
        )
        val locationTracker = GpsLocationTracker(applicationContext)

        setContent {
            AbsenKuTheme {
                val viewModel: AbsenViewModel = viewModel(
                    factory = AbsenViewModel.Factory(repository, locationTracker)
                )
                AbsenKuApp(viewModel = viewModel)
            }
        }
    }
}
