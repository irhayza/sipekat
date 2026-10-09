package com.jargas.si_pekat.data

import com.jargas.si_pekat.core.AppContext
import com.jargas.si_pekat.core.Json
import com.jargas.si_pekat.core.JsonMap
import com.jargas.si_pekat.core.SessionStore
import com.jargas.si_pekat.core.asJsonMap
import com.jargas.si_pekat.core.asList
import com.jargas.si_pekat.model.Customer
import com.jargas.si_pekat.model.DropdownOptions
import com.jargas.si_pekat.model.Ticket
import com.jargas.si_pekat.model.VisitCustomer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File

/** Hasil pencarian pelanggan lintas modul untuk autofill (mis. di Pengaduan saat mengetik ID). */
data class CachedCustomerMatch(
    val idPelanggan: String,
    val nama: String,
    val alamat: String,
    val telepon: String,
    val source: String,
)

/**
 * Cache sesi aplikasi. Semua daftar yang dibutuhkan tiap modul diambil SEKALI setelah login, disimpan lokal,
 * lalu dibaca instan oleh layar; panggilan jaringan baru hanya saat pengguna menekan "Segarkan".
 *
 * Beda dari versi Flutter: cache disimpan sebagai berkas JSON di filesDir (bukan SharedPreferences) —
 * daftar pelanggan dbase bisa puluhan ribu baris dan terlalu besar untuk SharedPreferences.
 */
object AppSessionCache {
    @Volatile var officerNama = ""
    @Volatile var officerEmail = ""
    @Volatile var officerRole = "petugas"

    // ── Modul Perbaikan ──
    @Volatile var perbaikanOptions: DropdownOptions? = null
    @Volatile var perbaikanTickets: List<Ticket> = emptyList()
    @Volatile var perbaikanError: String? = null
    @Volatile var perbaikanLoading = false

    // ── Modul Kunjungan (penutupan) ──
    @Volatile var kunjunganList: List<VisitCustomer> = emptyList()
    @Volatile var kunjunganError: String? = null
    @Volatile var kunjunganLoading = false

    // ── Modul Pembukaan (reopen) ──
    @Volatile var pembukaanList: List<VisitCustomer> = emptyList()
    @Volatile var pembukaanError: String? = null
    @Volatile var pembukaanLoading = false

    // ── Modul Pencatatan Meter (OCR) ──
    @Volatile var ocrCustomers: List<Customer> = emptyList()
    @Volatile var ocrError: String? = null
    @Volatile var ocrLoading = false

    // ── Cache global dbase (autofill seluruh form) ──
    @Volatile var dapellCustomers: List<Customer> = emptyList()
        set(value) {
            field = value
            dapellIndex = HashMap<String, Customer>(value.size * 2).also { m -> value.forEach { m[it.noPelanggan.trim()] = it } }
        }

    /** Indeks IDPEL → pelanggan untuk pencarian instan (autofill form Pengaduan), dibangun otomatis saat dbase dimuat. */
    @Volatile var dapellIndex: Map<String, Customer> = emptyMap()
        private set
    @Volatile var dapellError: String? = null
    @Volatile var dapellLoading = false

    @Volatile private var everPreloaded = false
    @Volatile private var preloading = false
    @Volatile var lastLoadedAt: Long? = null

    /** Nama petugas yang sedang login (cadangan: dari sesi tersimpan). */
    fun currentOfficerName(): String = officerNama.ifEmpty { SessionStore.load()?.nama.orEmpty() }

    val isPreloaded: Boolean get() = everPreloaded
    val isPreloading: Boolean get() = preloading

    // ── Penyimpanan berkas ──────────────────────────────────────────────────
    private const val F_OPTIONS = "cache_perbaikan_options.json"
    private const val F_TICKETS = "cache_perbaikan_tickets.json"
    private const val F_KUNJUNGAN = "cache_kunjungan_list.json"
    private const val F_PEMBUKAAN = "cache_pembukaan_list.json"
    private const val F_OCR = "cache_ocr_customers.json"
    private const val F_DAPELL = "cache_dapell_customers.json"
    private const val F_LAST = "cache_last_loaded_at.txt"
    private val ALL_FILES = listOf(F_OPTIONS, F_TICKETS, F_KUNJUNGAN, F_PEMBUKAAN, F_OCR, F_DAPELL, F_LAST)

    private fun file(name: String) = File(AppContext.app.filesDir, name)

    private fun readMap(name: String): JsonMap? =
        file(name).takeIf { it.exists() && it.length() > 0 }?.readText()?.let { Json.decode(it).asJsonMap() }

    private fun <T> readList(name: String, mapper: (JsonMap) -> T): List<T>? =
        file(name).takeIf { it.exists() && it.length() > 0 }?.readText()
            ?.let { Json.decode(it).asList() }
            ?.mapNotNull { it.asJsonMap()?.let(mapper) }

    /** Muat cache dari penyimpanan lokal saat aplikasi dibuka (jalankan di background). */
    suspend fun initFromLocal(nama: String, email: String, role: String = "petugas") = withContext(Dispatchers.IO) {
        officerNama = nama
        officerEmail = email
        officerRole = role.ifEmpty { "petugas" }
        try {
            readMap(F_OPTIONS)?.let { perbaikanOptions = DropdownOptions.fromMap(it) }
            readList(F_TICKETS, Ticket.Companion::fromMap)?.let { perbaikanTickets = it }
            readList(F_KUNJUNGAN, VisitCustomer.Companion::fromMap)?.let { kunjunganList = it }
            readList(F_PEMBUKAAN, VisitCustomer.Companion::fromMap)?.let { pembukaanList = it }
            readList(F_OCR, Customer.Companion::fromMap)?.let { ocrCustomers = it }
            readList(F_DAPELL, Customer.Companion::fromMap)?.let { dapellCustomers = it }
            file(F_LAST).takeIf { it.exists() }?.readText()?.trim()?.toLongOrNull()?.let { lastLoadedAt = it }
            everPreloaded = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Cache rusak/tidak terbaca → abaikan; preloadAll akan mengisi ulang.
        }
        Unit
    }

    /** Simpan seluruh cache aktif ke penyimpanan lokal. */
    suspend fun saveToLocal() = withContext(Dispatchers.IO) {
        try {
            perbaikanOptions?.let { file(F_OPTIONS).writeText(Json.encode(it.toMap())) }
            file(F_TICKETS).writeText(Json.encode(perbaikanTickets.map { it.toMap() }))
            file(F_KUNJUNGAN).writeText(Json.encode(kunjunganList.map { it.toMap() }))
            file(F_PEMBUKAAN).writeText(Json.encode(pembukaanList.map { it.toMap() }))
            file(F_OCR).writeText(Json.encode(ocrCustomers.map { it.toMap() }))
            file(F_DAPELL).writeText(Json.encode(dapellCustomers.map { it.toMap() }))
            file(F_LAST).writeText((lastLoadedAt ?: System.currentTimeMillis()).toString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Gagal menyimpan cache tidak boleh mengganggu alur utama.
        }
        Unit
    }

    /** Dipanggil sekali tepat setelah login: isi seluruh cache paralel lalu simpan ke lokal. */
    suspend fun preloadAll(
        nama: String,
        email: String,
        role: String = "petugas",
        onProgress: ((String) -> Unit)? = null,
    ) {
        if (preloading) return
        preloading = true
        officerNama = nama
        officerEmail = email
        officerRole = role.ifEmpty { "petugas" }
        try {
            coroutineScope {
                onProgress?.invoke("Memuat data Perbaikan...")
                val perbaikan = async(Dispatchers.Default) { loadPerbaikan() }
                onProgress?.invoke("Memuat data Kunjungan...")
                val kunjungan = async(Dispatchers.Default) { loadKunjungan() }
                onProgress?.invoke("Memuat data Pembukaan...")
                val pembukaan = async(Dispatchers.Default) { loadPembukaan() }
                onProgress?.invoke("Memuat data Pencatatan Meter...")
                val ocr = async(Dispatchers.Default) { loadOcrCustomers() }
                onProgress?.invoke("Memuat data pelanggan lokal...")
                val dapell = async(Dispatchers.Default) { loadDapellCustomers() }
                awaitAll(perbaikan, kunjungan, pembukaan, ocr, dapell)
            }
            everPreloaded = true
            lastLoadedAt = System.currentTimeMillis()
            saveToLocal()
        } finally {
            preloading = false
        }
    }

    private fun Throwable.brief(): String = message ?: toString()

    private suspend fun loadPerbaikan() {
        perbaikanLoading = true
        try {
            val options = PerbaikanApiService.getDropdownOptions()
            // Parser tiket yang sama dengan layar Perbaikan (termasuk koordinat berformat desimal koma).
            val tickets = PerbaikanTicketService.getActiveTickets()
            perbaikanOptions = options
            perbaikanTickets = tickets
            perbaikanError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            perbaikanError = e.brief()
        } finally {
            perbaikanLoading = false
        }
    }

    private suspend fun loadKunjungan() {
        kunjunganLoading = true
        try {
            kunjunganList = KunjunganApiService.getList(officerNama, officerRole, "kunjungan")
            kunjunganError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            kunjunganError = e.brief()
        } finally {
            kunjunganLoading = false
        }
    }

    private suspend fun loadPembukaan() {
        pembukaanLoading = true
        try {
            pembukaanList = KunjunganApiService.getList(officerNama, officerRole, "pembukaan")
            pembukaanError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            pembukaanError = e.brief()
        } finally {
            pembukaanLoading = false
        }
    }

    private suspend fun loadOcrCustomers() {
        ocrLoading = true
        try {
            ocrCustomers = OcrApiService.getAssignedCustomers(officerNama)
            ocrError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ocrError = e.brief()
        } finally {
            ocrLoading = false
        }
    }

    private suspend fun loadDapellCustomers() {
        dapellLoading = true
        try {
            dapellCustomers = OcrApiService.getAllDapellCustomers()
            dapellError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            dapellError = e.brief()
        } finally {
            dapellLoading = false
        }
    }

    // ── Refresh per modul (tombol / pull-to-refresh) ────────────────────────
    suspend fun refreshPerbaikan() { withContext(Dispatchers.Default) { loadPerbaikan() }; saveToLocal() }
    suspend fun refreshKunjungan() { withContext(Dispatchers.Default) { loadKunjungan() }; saveToLocal() }
    suspend fun refreshPembukaan() { withContext(Dispatchers.Default) { loadPembukaan() }; saveToLocal() }
    suspend fun refreshOcrCustomers() { withContext(Dispatchers.Default) { loadOcrCustomers() }; saveToLocal() }
    suspend fun refreshDapellCustomers() { withContext(Dispatchers.Default) { loadDapellCustomers() }; saveToLocal() }

    /** Cari pelanggan lintas seluruh cache berdasarkan ID (prioritas: dbase → Perbaikan → Kunjungan → Pembukaan → Meter). */
    fun findCustomerById(rawId: String): CachedCustomerMatch? {
        val id = rawId.trim().lowercase()
        if (id.isEmpty()) return null

        dapellIndex[rawId.trim()]?.let {
            return CachedCustomerMatch(it.noPelanggan, it.nama, it.alamat ?: "", "", "Dapell")
        }
        perbaikanTickets.firstOrNull { it.idPelanggan.lowercase() == id }?.let {
            return CachedCustomerMatch(it.idPelanggan, it.nama, it.alamat, it.telepon, "Perbaikan")
        }
        kunjunganList.firstOrNull { it.idpel.lowercase() == id }?.let {
            return CachedCustomerMatch(it.idpel, it.nama, it.alamat, it.telepon, "Kunjungan")
        }
        pembukaanList.firstOrNull { it.idpel.lowercase() == id }?.let {
            return CachedCustomerMatch(it.idpel, it.nama, it.alamat, it.telepon, "Pembukaan")
        }
        ocrCustomers.firstOrNull { it.noPelanggan.lowercase() == id || it.id.lowercase() == id }?.let {
            return CachedCustomerMatch(it.noPelanggan, it.nama, it.alamat ?: "", "", "Pencatatan Meter")
        }
        return null
    }

    /** Dipanggil saat logout agar petugas berikutnya di perangkat yang sama tidak melihat cache petugas sebelumnya. */
    fun clear() {
        officerNama = ""
        officerEmail = ""
        officerRole = "petugas"
        perbaikanOptions = null
        perbaikanTickets = emptyList()
        perbaikanError = null
        kunjunganList = emptyList()
        kunjunganError = null
        pembukaanList = emptyList()
        pembukaanError = null
        ocrCustomers = emptyList()
        ocrError = null
        dapellCustomers = emptyList()
        dapellError = null
        dapellLoading = false
        everPreloaded = false
        preloading = false
        lastLoadedAt = null
        ALL_FILES.forEach { runCatching { file(it).delete() } }
    }
}
