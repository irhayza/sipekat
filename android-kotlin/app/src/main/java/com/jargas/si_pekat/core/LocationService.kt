package com.jargas.si_pekat.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

enum class GpsResult { OK, SERVICE_DISABLED, PERMISSION_DENIED, PERMISSION_DENIED_FOREVER, TIMEOUT, UNKNOWN }

data class LocationOutcome(val location: Location?, val result: GpsResult) {
    val isOk: Boolean get() = result == GpsResult.OK && location != null
}

/**
 * Port dari location_service.dart. Beda dengan geolocator di Flutter: izin lokasi TIDAK diminta di sini
 * (Android meminta izin lewat Activity). Layar memanggil [hasPermission] / launcher izin lebih dulu,
 * lalu memanggil [getCurrentLocation].
 */
object LocationService {
    /** Buka layar Pengaturan Aplikasi (untuk izin lokasi yang ditolak permanen). */
    fun openAppSettings(context: Context) {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.parse("package:${context.packageName}"),
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (_: Exception) {}
    }

    /** Lokasi terakhir yang diketahui perangkat (instan, bisa kosong/usang). */
    suspend fun lastKnown(context: Context): Location? {
        if (!hasPermission(context)) return null
        return try {
            LocationServices.getFusedLocationProviderClient(context).lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun getCurrentLocation(context: Context, timeLimitMs: Long = 15_000): LocationOutcome {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(lm)) return LocationOutcome(null, GpsResult.SERVICE_DISABLED)
        if (!hasPermission(context)) return LocationOutcome(null, GpsResult.PERMISSION_DENIED)

        val client = LocationServices.getFusedLocationProviderClient(context)
        return try {
            val cts = CancellationTokenSource()
            val fresh = withTimeoutOrNull(timeLimitMs) {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
            }
            if (fresh != null) {
                LocationOutcome(fresh, GpsResult.OK)
            } else {
                cts.cancel()
                val last = client.lastLocation.await()
                if (last != null) LocationOutcome(last, GpsResult.OK) else LocationOutcome(null, GpsResult.TIMEOUT)
            }
        } catch (e: SecurityException) {
            LocationOutcome(null, GpsResult.PERMISSION_DENIED)
        } catch (e: Exception) {
            LocationOutcome(null, GpsResult.UNKNOWN)
        }
    }

    fun messageFor(r: GpsResult): String = when (r) {
        GpsResult.SERVICE_DISABLED -> "GPS perangkat tidak aktif. Laporan tetap bisa dikirim tanpa koordinat."
        GpsResult.PERMISSION_DENIED -> "Izin lokasi ditolak. Laporan tetap bisa dikirim tanpa koordinat."
        GpsResult.PERMISSION_DENIED_FOREVER -> "Izin lokasi diblokir permanen. Aktifkan lewat Pengaturan aplikasi jika ingin menyertakan koordinat."
        GpsResult.TIMEOUT -> "Sinyal GPS lemah. Laporan tetap bisa dikirim tanpa koordinat."
        GpsResult.UNKNOWN -> "Lokasi tidak tersedia. Laporan tetap bisa dikirim tanpa koordinat."
        GpsResult.OK -> "Lokasi berhasil dikunci."
    }
}
