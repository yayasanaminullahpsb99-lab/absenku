package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class GpsCoordinates(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val provider: String,
    val timestampMillis: Long
)

sealed interface GpsFetchResult {
    data class Success(val coordinates: GpsCoordinates) : GpsFetchResult
    data object PermissionMissing : GpsFetchResult
    data object LocationDisabled : GpsFetchResult
    data class Failure(val message: String) : GpsFetchResult
}

class GpsLocationTracker(private val context: Context) {

    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(context) }
    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isLocationEnabled(): Boolean {
        return runCatching {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): GpsFetchResult {
        if (!hasLocationPermission()) {
            return GpsFetchResult.PermissionMissing
        }
        if (!isLocationEnabled()) {
            return GpsFetchResult.LocationDisabled
        }

        // 1. Try FusedLocationProviderClient active high-accuracy fetch
        val fusedCurrent = withTimeoutOrNull(7000L) {
            fetchFusedCurrentLocation()
        }
        if (fusedCurrent != null) {
            return GpsFetchResult.Success(fusedCurrent.toGpsCoordinates("Fused GPS"))
        }

        // 2. Try native LocationManager single update (works offline without Play Services)
        val nativeCurrent = withTimeoutOrNull(6000L) {
            fetchNativeLocationUpdate()
        }
        if (nativeCurrent != null) {
            return GpsFetchResult.Success(nativeCurrent.toGpsCoordinates("GPS Perangkat"))
        }

        // 3. Fallback to most recent cached location (Fused or LocationManager)
        val fusedLast = withTimeoutOrNull(2000L) { fetchFusedLastLocation() }
        val nativeLast = getBestLastKnownNativeLocation()
        val bestLast = listOfNotNull(fusedLast, nativeLast).maxByOrNull { it.time }

        return if (bestLast != null) {
            GpsFetchResult.Success(bestLast.toGpsCoordinates("GPS Terakhir"))
        } else {
            GpsFetchResult.Failure(
                "Sinyal GPS belum ditemukan. Pastikan berada di area terbuka atau aktifkan GPS perangkat."
            )
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchFusedCurrentLocation(): Location? =
        suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }
            runCatching {
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }.onFailure {
                if (cont.isActive) cont.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun fetchFusedLastLocation(): Location? =
        suspendCancellableCoroutine { cont ->
            runCatching {
                fusedClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }.onFailure {
                if (cont.isActive) cont.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun fetchNativeLocationUpdate(): Location? =
        suspendCancellableCoroutine { cont ->
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                    LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                    LocationManager.NETWORK_PROVIDER
                else -> {
                    if (cont.isActive) cont.resume(null)
                    return@suspendCancellableCoroutine
                }
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    runCatching { locationManager.removeUpdates(this) }
                    if (cont.isActive) cont.resume(location)
                }

                override fun onProviderDisabled(provider: String) {}
                override fun onProviderEnabled(provider: String) {}
            }

            cont.invokeOnCancellation {
                runCatching { locationManager.removeUpdates(listener) }
            }

            runCatching {
                locationManager.requestLocationUpdates(
                    provider,
                    0L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
            }.onFailure {
                runCatching { locationManager.removeUpdates(listener) }
                if (cont.isActive) cont.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private fun getBestLastKnownNativeLocation(): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        return providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    }

    private fun Location.toGpsCoordinates(defaultProvider: String): GpsCoordinates {
        return GpsCoordinates(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = if (hasAccuracy()) accuracy else 0f,
            provider = provider ?: defaultProvider,
            timestampMillis = if (time > 0L) time else System.currentTimeMillis()
        )
    }

    companion object {
        fun distanceInMeters(
            lat1: Double,
            lng1: Double,
            lat2: Double,
            lng2: Double
        ): Float {
            val result = FloatArray(1)
            Location.distanceBetween(lat1, lng1, lat2, lng2, result)
            return result[0]
        }
    }
}
