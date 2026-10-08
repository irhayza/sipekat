package com.jargas.si_pekat.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat
import com.jargas.si_pekat.core.GpsResult
import com.jargas.si_pekat.core.LocationService
import kotlinx.coroutines.CompletableDeferred

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/**
 * Setara KunjunganLocationService.ensurePermission(): cek layanan GPS, minta izin bila perlu, dan
 * kembalikan hasilnya (OK / SERVICE_DISABLED / PERMISSION_DENIED / PERMISSION_DENIED_FOREVER).
 */
@Composable
fun rememberEnsureLocation(): suspend () -> GpsResult {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<CompletableDeferred<Boolean>>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        pending[0]?.complete(r.values.any { it })
        pending[0] = null
    }
    return remember(context) {
        suspend {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            when {
                !LocationManagerCompat.isLocationEnabled(lm) -> GpsResult.SERVICE_DISABLED
                LocationService.hasPermission(context) -> GpsResult.OK
                else -> {
                    val d = CompletableDeferred<Boolean>()
                    pending[0] = d
                    launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    if (d.await()) GpsResult.OK else {
                        val act = context.findActivity()
                        if (act != null && !ActivityCompat.shouldShowRequestPermissionRationale(act, Manifest.permission.ACCESS_FINE_LOCATION))
                            GpsResult.PERMISSION_DENIED_FOREVER else GpsResult.PERMISSION_DENIED
                    }
                }
            }
        }
    }
}

/** Status GPS untuk form Kunjungan/Pembukaan: koordinat terakhir + peringatan izin/layanan. */
class VisitGpsState(private val context: Context, private val ensure: suspend () -> GpsResult) {
    var lat by mutableStateOf<Double?>(null)
    var lng by mutableStateOf<Double?>(null)
    var serviceEnabled by mutableStateOf(true)
    var warning by mutableStateOf<String?>(null)

    val hasFix: Boolean get() = lat != null && lng != null

    suspend fun ensurePermission(): GpsResult {
        val st = ensure()
        serviceEnabled = st != GpsResult.SERVICE_DISABLED
        return st
    }

    /** Prefetch diam-diam saat layar dibuka: izin → lokasi terakhir (instan) → lokasi baru. */
    suspend fun prefetch() {
        val st = ensurePermission()
        warning = when (st) {
            GpsResult.PERMISSION_DENIED -> "Izin lokasi belum diberikan. Ketuk untuk memberi izin."
            GpsResult.PERMISSION_DENIED_FOREVER -> "Izin lokasi ditolak permanen. Ketuk untuk membuka Pengaturan Aplikasi."
            else -> null
        }
        if (st != GpsResult.OK) return
        LocationService.lastKnown(context)?.let { lat = it.latitude; lng = it.longitude }
        val fresh = LocationService.getCurrentLocation(context, 15_000)
        if (fresh.isOk) { lat = fresh.location!!.latitude; lng = fresh.location.longitude }
    }

    /** Pastikan ada koordinat saat kirim; null bila berhasil, selain itu pesan galat. */
    suspend fun ensureFix(): String? {
        if (hasFix) return null
        val o = LocationService.getCurrentLocation(context, 10_000)
        if (o.isOk) { lat = o.location!!.latitude; lng = o.location.longitude; return null }
        return "Gagal mendapatkan koordinat GPS. Pastikan GPS aktif dan sinyal cukup, lalu coba lagi."
    }

    fun openSettings() = LocationService.openAppSettings(context)
}

@Composable
fun rememberVisitGps(): VisitGpsState {
    val context = LocalContext.current
    val ensure = rememberEnsureLocation()
    return remember(context, ensure) { VisitGpsState(context, ensure) }
}
