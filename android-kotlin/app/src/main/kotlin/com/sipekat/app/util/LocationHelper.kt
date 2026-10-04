package com.sipekat.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class GpsResult {
    OK, SERVICE_DISABLED, PERMISSION_DENIED, PERMISSION_DENIED_FOREVER, TIMEOUT, UNKNOWN
}

data class LocationOutcome(
    val lat: Double?,
    val lng: Double?,
    val result: GpsResult
) {
    val isOk: Boolean get() = result == GpsResult.OK && lat != null && lng != null
}

object LocationHelper {

    suspend fun getCurrentLocation(
        context: Context,
        timeLimitMs: Long = 15_000L
    ): LocationOutcome {
        val fineGranted  = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)  == PackageManager.PERMISSION_GRANTED
        val coarseGranted= ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return LocationOutcome(null, null, GpsResult.PERMISSION_DENIED)

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        return suspendCancellableCoroutine { cont ->
            val request = LocationRequest.create().apply {
                priority        = LocationRequest.PRIORITY_HIGH_ACCURACY
                numUpdates      = 1
                setExpirationDuration(timeLimitMs)
            }
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fusedClient.removeLocationUpdates(this)
                    val loc = result.lastLocation
                    if (loc != null) cont.resume(LocationOutcome(loc.latitude, loc.longitude, GpsResult.OK))
                    else cont.resume(LocationOutcome(null, null, GpsResult.UNKNOWN))
                }
                override fun onLocationAvailability(avail: LocationAvailability) {
                    if (!avail.isLocationAvailable) {
                        fusedClient.removeLocationUpdates(this)
                        try {
                            fusedClient.lastLocation.addOnCompleteListener { task ->
                                val loc = task.result
                                if (loc != null) cont.resume(LocationOutcome(loc.latitude, loc.longitude, GpsResult.OK))
                                else cont.resume(LocationOutcome(null, null, GpsResult.SERVICE_DISABLED))
                            }
                        } catch (e: SecurityException) {
                            cont.resume(LocationOutcome(null, null, GpsResult.PERMISSION_DENIED))
                        }
                    }
                }
            }
            try {
                fusedClient.requestLocationUpdates(request, callback, context.mainLooper)
            } catch (e: SecurityException) {
                cont.resume(LocationOutcome(null, null, GpsResult.PERMISSION_DENIED))
            }
            cont.invokeOnCancellation { fusedClient.removeLocationUpdates(callback) }
        }
    }

    fun messageFor(result: GpsResult): String = when (result) {
        GpsResult.SERVICE_DISABLED         -> "GPS perangkat tidak aktif. Laporan tetap bisa dikirim tanpa koordinat."
        GpsResult.PERMISSION_DENIED        -> "Izin lokasi ditolak. Laporan tetap bisa dikirim tanpa koordinat."
        GpsResult.PERMISSION_DENIED_FOREVER-> "Izin lokasi diblokir permanen. Aktifkan lewat Pengaturan aplikasi."
        GpsResult.TIMEOUT                  -> "Sinyal GPS lemah. Laporan tetap bisa dikirim tanpa koordinat."
        GpsResult.UNKNOWN                  -> "Lokasi tidak tersedia. Laporan tetap bisa dikirim tanpa koordinat."
        GpsResult.OK                       -> "Lokasi berhasil dikunci."
    }
}
