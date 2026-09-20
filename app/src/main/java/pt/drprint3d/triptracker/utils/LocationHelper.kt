package pt.drprint3d.triptracker.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class LocationHelper(context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        // Try FusedLocationProviderClient first
        try {
            val cancellationTokenSource = CancellationTokenSource()
            val location = withTimeoutOrNull(10000L) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).awaitTask()
            }
            if (location != null) {
                return location
            }
        } catch (e: Exception) {
            // Ignore & fallback
        }

        // Try last known location from fused client
        try {
            val lastLoc = fusedLocationClient.lastLocation.awaitTask()
            if (lastLoc != null && (System.currentTimeMillis() - lastLoc.time) < 120000) {
                return lastLoc
            }
        } catch (e: Exception) {
            // Ignore & fallback
        }

        // Fallback to Android Framework LocationManager (100% offline GPS)
        try {
            val gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (gpsLoc != null && netLoc != null) {
                return if (gpsLoc.time > netLoc.time) gpsLoc else netLoc
            }
            if (gpsLoc != null) return gpsLoc
            if (netLoc != null) return netLoc
        } catch (e: Exception) {
            // Ignore
        }

        return null
    }

    private suspend fun <T> Task<T>.awaitTask(): T? = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result, null)
        }
        addOnFailureListener {
            if (continuation.isActive) continuation.resume(null, null)
        }
        addOnCanceledListener {
            if (continuation.isActive) continuation.resume(null, null)
        }
    }
}
