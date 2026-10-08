package com.jargas.si_pekat.data

import com.jargas.si_pekat.core.Connectivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import java.io.File

/** Sinkronisasi antrean bacaan meter offline. Port dari ocr_sync_service.dart. */
object OcrSyncService {
    private val lock = Mutex()

    /** Kirim semua bacaan yang belum tersinkron. Mengembalikan jumlah yang berhasil. */
    suspend fun syncAll(): Int {
        if (!Connectivity.isOnline()) return 0
        if (!lock.tryLock()) return 0
        var synced = 0
        try {
            val unsynced = OcrLocalDb.getUnsynced()
            for (reading in unsynced) {
                try {
                    val updated = OcrApiService.syncPendingReading(reading, AppSessionCache.currentOfficerName())
                    OcrLocalDb.saveReading(updated)
                    reading.fotoPath?.trim()?.takeIf { it.isNotEmpty() }?.let { path ->
                        try { File(path).delete() } catch (_: Exception) {}
                    }
                    synced++
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Gagal untuk satu bacaan: lanjut ke berikutnya, coba lagi pada sinkronisasi berikutnya.
                }
            }
        } finally {
            lock.unlock()
        }
        return synced
    }

    /** Sinkron otomatis tiap kali jaringan kembali tersedia; memancarkan jumlah yang berhasil dikirim. */
    fun autoSync(): Flow<Int> = Connectivity.onAvailable().map { syncAll() }
}
