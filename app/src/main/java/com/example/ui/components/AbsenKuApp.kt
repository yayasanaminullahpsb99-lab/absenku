package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.state.AbsenTab
import com.example.ui.state.AbsenViewModel

private data class TabNavItem(
    val tab: AbsenTab,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val navItems = listOf(
    TabNavItem(AbsenTab.ABSENSI, Icons.Rounded.Fingerprint, Icons.Outlined.Fingerprint),
    TabNavItem(AbsenTab.REKAP, Icons.Rounded.Assessment, Icons.Outlined.Assessment),
    TabNavItem(AbsenTab.KARYAWAN, Icons.Rounded.Badge, Icons.Outlined.Badge),
    TabNavItem(AbsenTab.LOKASI, Icons.Rounded.LocationOn, Icons.Outlined.LocationOn)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsenKuApp(viewModel: AbsenViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val currentTimeMillis by viewModel.currentTimeMillis.collectAsStateWithLifecycle()
    val gpsState by viewModel.gpsState.collectAsStateWithLifecycle()
    val activeLocation by viewModel.activeLocation.collectAsStateWithLifecycle()
    val allLocations by viewModel.allLocations.collectAsStateWithLifecycle()
    val activeEmployees by viewModel.activeEmployees.collectAsStateWithLifecycle()
    val filteredEmployees by viewModel.filteredEmployees.collectAsStateWithLifecycle()
    val allEmployees by viewModel.allEmployees.collectAsStateWithLifecycle()
    val selectedEmployee by viewModel.selectedEmployee.collectAsStateWithLifecycle()
    val todayAttendance by viewModel.todayAttendanceList.collectAsStateWithLifecycle()
    val filteredRecap by viewModel.filteredRecapRecords.collectAsStateWithLifecycle()
    val recapSummary by viewModel.recapSummary.collectAsStateWithLifecycle()
    val adminConfig by viewModel.adminConfig.collectAsStateWithLifecycle()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsStateWithLifecycle()
    val recapDateFilter by viewModel.recapDateFilter.collectAsStateWithLifecycle()
    val recapStatusFilter by viewModel.recapStatusFilter.collectAsStateWithLifecycle()
    val recapSearchQuery by viewModel.recapSearchQuery.collectAsStateWithLifecycle()
    val employeeSearchQuery by viewModel.employeeSearchQuery.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (selectedTab != AbsenTab.ABSENSI) {
        BackHandler {
            viewModel.selectTab(AbsenTab.ABSENSI)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (selectedTab) {
                                AbsenTab.ABSENSI -> "AbsenKu"
                                AbsenTab.REKAP -> "Rekap Absensi"
                                AbsenTab.KARYAWAN -> "Karyawan"
                                AbsenTab.LOKASI -> "Lokasi GPS"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val isSelected = selectedTab == item.tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.tab.label
                                    )
                                },
                                label = { Text(item.tab.label) },
                                modifier = Modifier.testTag("nav_tab_${item.tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val isSelected = selectedTab == item.tab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(item.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.tab.label
                                    )
                                },
                                label = { Text(item.tab.label) },
                                modifier = Modifier.testTag("rail_tab_${item.tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val contentModifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)

                    when (selectedTab) {
                        AbsenTab.ABSENSI -> {
                            AbsensiScreen(
                                currentTimeMillis = currentTimeMillis,
                                gpsState = gpsState,
                                activeLocation = activeLocation,
                                activeEmployees = activeEmployees,
                                selectedEmployee = selectedEmployee,
                                todayAttendance = todayAttendance,
                                adminConfig = adminConfig,
                                onSelectEmployee = viewModel::selectEmployee,
                                onRefreshGps = viewModel::refreshGpsLocation,
                                onCheckIn = viewModel::performCheckIn,
                                onCheckOut = viewModel::performCheckOut,
                                onNavigateToLocations = { viewModel.selectTab(AbsenTab.LOKASI) },
                                onNavigateToEmployees = { viewModel.selectTab(AbsenTab.KARYAWAN) },
                                modifier = contentModifier
                            )
                        }

                        AbsenTab.REKAP -> {
                            RekapScreen(
                                adminConfig = adminConfig,
                                isAdminUnlocked = isAdminUnlocked,
                                records = filteredRecap,
                                summary = recapSummary,
                                employees = allEmployees,
                                locations = allLocations,
                                dateFilter = recapDateFilter,
                                statusFilter = recapStatusFilter,
                                searchQuery = recapSearchQuery,
                                onUnlockAdmin = viewModel::unlockAdmin,
                                onLockAdmin = viewModel::lockAdmin,
                                onDateFilterChange = viewModel::setRecapDateFilter,
                                onStatusFilterChange = viewModel::setRecapStatusFilter,
                                onSearchQueryChange = viewModel::setRecapSearchQuery,
                                onCreateManualAttendance = viewModel::createManualAttendance,
                                onUpdateAttendance = viewModel::updateAttendanceRecord,
                                onDeleteAttendance = viewModel::deleteAttendanceRecord,
                                onBuildExportCsv = viewModel::buildExportCsv,
                                modifier = contentModifier
                            )
                        }

                        AbsenTab.KARYAWAN -> {
                            KaryawanScreen(
                                adminConfig = adminConfig,
                                isAdminUnlocked = isAdminUnlocked,
                                employees = filteredEmployees,
                                searchQuery = employeeSearchQuery,
                                onUnlockAdmin = viewModel::unlockAdmin,
                                onLockAdmin = viewModel::lockAdmin,
                                onSearchQueryChange = viewModel::setEmployeeSearchQuery,
                                onSaveEmployee = viewModel::saveEmployee,
                                onDeleteEmployee = viewModel::deleteEmployee,
                                modifier = contentModifier
                            )
                        }

                        AbsenTab.LOKASI -> {
                            LokasiScreen(
                                adminConfig = adminConfig,
                                isAdminUnlocked = isAdminUnlocked,
                                locations = allLocations,
                                gpsState = gpsState,
                                onUnlockAdmin = viewModel::unlockAdmin,
                                onLockAdmin = viewModel::lockAdmin,
                                onRefreshGps = viewModel::refreshGpsLocation,
                                onSaveLocation = viewModel::saveLocation,
                                onActivateLocation = viewModel::activateLocation,
                                onDeleteLocation = viewModel::deleteLocation,
                                onUpdateAdminSettings = viewModel::updateAdminSettings,
                                modifier = contentModifier
                            )
                        }
                    }
                }
            }
        }
    }
}
