package com.sipekat.app.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sipekat.app.config.AppConfig
import com.sipekat.app.data.model.*
import com.sipekat.app.data.repository.KunjunganRepository
import com.sipekat.app.data.repository.OcrRepository
import com.sipekat.app.data.repository.PerbaikanRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class CachedCustomerMatch(
    val idPelanggan: String, val nama: String, val alamat: String,
    val telepon: String, val source: String
)

/**
 * Cache sesi aplikasi — port dari Dart AppSessionCache.
 * Menyimpan seluruh daftar modul ke SharedPreferences agar
 * layar bisa langsung membaca tanpa menunggu jaringan.
 */
object SessionCache {

    private lateinit var sm: SessionManager
    private val gson = Gson()

    var officerNama: String  = ""; var officerEmail: String = ""; var officerRole: String = "petugas"
    var perbaikanOptions: DropdownOptions?   = null
    var perbaikanTickets: List<Ticket>       = emptyList()
    var perbaikanError: String?              = null; var perbaikanLoading = false
    var kunjunganList: List<VisitCustomer>   = emptyList()
    var kunjunganError: String?              = null; var kunjunganLoading = false
    var pembukaanList: List<VisitCustomer>   = emptyList()
    var pembukaanError: String?              = null; var pembukaanLoading = false
    var ocrCustomers: List<Customer>         = emptyList()
    var ocrError: String?                   = null; var ocrLoading = false
    var dapellCustomers: List<Customer>      = emptyList()
    var dapellError: String?                = null; var dapellLoading = false

    private var everPreloaded = false; private var preloading = false
    var lastLoadedAt: Long? = null
    val isPreloaded: Boolean get() = everPreloaded
    val isPreloading: Boolean get() = preloading

    fun init(sessionManager: SessionManager) { sm = sessionManager }

    fun initFromLocal(nama: String, email: String, role: String = "petugas") {
        officerNama = nama; officerEmail = email; officerRole = role.ifEmpty { "petugas" }
        try {
            sm.getString(AppConfig.PREF_CACHE_PERBAIKAN_OPTIONS)?.let { json ->
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val m: Map<String, Any> = gson.fromJson(json, type)
                perbaikanOptions = DropdownOptions(
                    petugas    = (m["petugas"] as? List<*>)?.filterIsInstance<Map<String,Any>>()
                        ?.map { p -> Officer(p["nama"]?.toString() ?: "", p["area"]?.toString() ?: "") } ?: emptyList(),
                    jenis      = (m["jenis"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                    penanganan = (m["penanganan"] as? List<*>)?.map { it.toString() } ?: emptyList()
                )
            }
            val typeTicket = object : TypeToken<List<Map<String, Any?>>>() {}.type
            fun restoreListTicket(key: String): List<Ticket> {
                val json = sm.getString(key) ?: return emptyList()
                val list: List<Map<String, Any?>> = gson.fromJson(json, typeTicket)
                return list.map { Ticket.fromMap(it) }
            }
            fun restoreListVisit(key: String): List<VisitCustomer> {
                val json = sm.getString(key) ?: return emptyList()
                val list: List<Map<String, Any?>> = gson.fromJson(json, typeTicket)
                return list.map { VisitCustomer.fromMap(it) }
            }
            fun restoreListCustomer(key: String): List<Customer> {
                val json = sm.getString(key) ?: return emptyList()
                val list: List<Map<String, Any?>> = gson.fromJson(json, typeTicket)
                return list.map { Customer.fromMap(it) }
            }
            perbaikanTickets = restoreListTicket(AppConfig.PREF_CACHE_PERBAIKAN_TICKETS)
            kunjunganList    = restoreListVisit(AppConfig.PREF_CACHE_KUNJUNGAN_LIST)
            pembukaanList    = restoreListVisit(AppConfig.PREF_CACHE_PEMBUKAAN_LIST)
            ocrCustomers     = restoreListCustomer(AppConfig.PREF_CACHE_OCR_CUSTOMERS)
            dapellCustomers  = restoreListCustomer(AppConfig.PREF_CACHE_DAPELL_CUSTOMERS)
            everPreloaded = true
        } catch (_: Exception) {}
    }

    suspend fun preloadAll(
        nama: String, email: String, role: String = "petugas",
        onProgress: ((String) -> Unit)? = null
    ) {
        if (preloading) return
        preloading = true; officerNama = nama; officerEmail = email; officerRole = role.ifEmpty { "petugas" }
        try {
            coroutineScope {
                val j1 = async { onProgress?.invoke("Memuat data Perbaikan...");        loadPerbaikan() }
                val j2 = async { onProgress?.invoke("Memuat data Kunjungan...");        loadKunjungan() }
                val j3 = async { onProgress?.invoke("Memuat data Pembukaan...");        loadPembukaan() }
                val j4 = async { onProgress?.invoke("Memuat data Pencatatan Meter..."); loadOcr() }
                val j5 = async { onProgress?.invoke("Memuat data pelanggan lokal...");  loadDapell() }
                j1.await(); j2.await(); j3.await(); j4.await(); j5.await()
            }
        } finally {
            everPreloaded = true; preloading = false; lastLoadedAt = System.currentTimeMillis()
            saveToLocal()
        }
    }

    private suspend fun loadPerbaikan() {
        perbaikanLoading = true
        try {
            val r = PerbaikanRepository()
            perbaikanOptions = r.getDropdownOptions()
            perbaikanTickets = r.getActiveTicketsFull(officerEmail, officerNama)
            perbaikanError   = null
        } catch (e: Exception) { perbaikanError = e.message } finally { perbaikanLoading = false }
    }
    private suspend fun loadKunjungan() {
        kunjunganLoading = true
        try { kunjunganList  = KunjunganRepository().getList(officerNama, officerRole, "kunjungan", officerEmail); kunjunganError  = null }
        catch (e: Exception) { kunjunganError  = e.message } finally { kunjunganLoading = false }
    }
    private suspend fun loadPembukaan() {
        pembukaanLoading = true
        try { pembukaanList  = KunjunganRepository().getList(officerNama, officerRole, "pembukaan", officerEmail); pembukaanError  = null }
        catch (e: Exception) { pembukaanError  = e.message } finally { pembukaanLoading = false }
    }
    private suspend fun loadOcr() {
        ocrLoading = true
        try { ocrCustomers = OcrRepository().getAssignedCustomers(officerNama); ocrError = null }
        catch (e: Exception) { ocrError = e.message } finally { ocrLoading = false }
    }
    private suspend fun loadDapell() {
        dapellLoading = true
        try { dapellCustomers = OcrRepository().getAllDapellCustomers(); dapellError = null }
        catch (e: Exception) { dapellError = e.message } finally { dapellLoading = false }
    }

    fun saveToLocal() {
        try {
            perbaikanOptions?.let { opt ->
                sm.saveString(AppConfig.PREF_CACHE_PERBAIKAN_OPTIONS, gson.toJson(mapOf(
                    "petugas"    to opt.petugas.map { mapOf("nama" to it.nama, "area" to it.area) },
                    "jenis"      to opt.jenis,
                    "penanganan" to opt.penanganan
                )))
            }
            sm.saveString(AppConfig.PREF_CACHE_PERBAIKAN_TICKETS,  gson.toJson(perbaikanTickets))
            sm.saveString(AppConfig.PREF_CACHE_KUNJUNGAN_LIST,     gson.toJson(kunjunganList))
            sm.saveString(AppConfig.PREF_CACHE_PEMBUKAAN_LIST,     gson.toJson(pembukaanList))
            sm.saveString(AppConfig.PREF_CACHE_OCR_CUSTOMERS,      gson.toJson(ocrCustomers))
            sm.saveString(AppConfig.PREF_CACHE_DAPELL_CUSTOMERS,   gson.toJson(dapellCustomers))
        } catch (_: Exception) {}
    }

    fun findCustomerById(rawId: String): CachedCustomerMatch? {
        val id = rawId.trim().lowercase()
        if (id.isEmpty()) return null
        dapellCustomers.firstOrNull { it.noPelanggan.lowercase() == id || it.id.lowercase() == id }
            ?.let { return CachedCustomerMatch(it.noPelanggan, it.nama, it.alamat ?: "", "", "Dapell") }
        perbaikanTickets.firstOrNull { it.idPelanggan.lowercase() == id }
            ?.let { return CachedCustomerMatch(it.idPelanggan, it.nama, it.alamat, it.telepon, "Perbaikan") }
        kunjunganList.firstOrNull { it.idpel.lowercase() == id }
            ?.let { return CachedCustomerMatch(it.idpel, it.nama, it.alamat, it.telepon, "Kunjungan") }
        pembukaanList.firstOrNull { it.idpel.lowercase() == id }
            ?.let { return CachedCustomerMatch(it.idpel, it.nama, it.alamat, it.telepon, "Pembukaan") }
        ocrCustomers.firstOrNull { it.noPelanggan.lowercase() == id || it.id.lowercase() == id }
            ?.let { return CachedCustomerMatch(it.noPelanggan, it.nama, it.alamat ?: "", "", "Pencatatan Meter") }
        return null
    }

    fun clear() {
        officerNama = ""; officerEmail = ""; officerRole = "petugas"
        perbaikanOptions = null; perbaikanTickets = emptyList(); perbaikanError = null
        kunjunganList    = emptyList(); kunjunganError = null
        pembukaanList    = emptyList(); pembukaanError = null
        ocrCustomers     = emptyList(); ocrError = null
        dapellCustomers  = emptyList(); dapellError = null
        everPreloaded = false; preloading = false; lastLoadedAt = null
    }
}
