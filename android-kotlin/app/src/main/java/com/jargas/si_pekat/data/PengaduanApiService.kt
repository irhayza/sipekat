package com.jargas.si_pekat.data

import com.google.gson.JsonParseException
import com.jargas.si_pekat.config.AppConfig
import com.jargas.si_pekat.core.GasHttp
import com.jargas.si_pekat.core.Json
import com.jargas.si_pekat.core.asJsonMap
import kotlinx.coroutines.CancellationException

data class SubmitResult(
    val ok: Boolean,
    val ticket: String? = null,
    val isOverdueUpdate: Boolean = false,
    val message: String? = null,
)

data class CustomerLookup(val nama: String, val alamat: String)

object PengaduanApiService {
    /** Cari pelanggan berdasarkan ID lewat route 'get_customer_by_id' di Code.gs. Null bila tidak ditemukan/gagal. */
    suspend fun checkCustomerId(id: String): CustomerLookup? {
        return try {
            val res = GasHttp.postJson(
                AppConfig.PENGADUAN_BRIDGE_URL,
                mapOf("action" to "get_customer_by_id", "idpel" to id),
                AppConfig.CONNECTION_TIMEOUT_SEC,
            )
            if (res.code != 200) return null
            val decoded = Json.decode(res.body).asJsonMap()
            if (decoded != null && decoded["status"] == "success") {
                CustomerLookup(
                    nama = decoded["nama"]?.toString() ?: "",
                    alamat = decoded["alamat"]?.toString() ?: "",
                )
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Kirim pengaduan. Body WAJIB memuat 'action' = 'submit_pengaduan'; tanpa itu doPost() di Code.gs
     * melempar request ke jalur webhook WAHA/Telegram dan pengaduan tidak pernah tersimpan.
     */
    suspend fun submitComplaint(data: Map<String, Any?>): SubmitResult {
        return try {
            val res = GasHttp.postJson(
                AppConfig.PENGADUAN_BRIDGE_URL,
                mapOf("action" to "submit_pengaduan", "data" to data),
                AppConfig.CONNECTION_TIMEOUT_SEC,
            )
            if (res.code != 200) {
                return SubmitResult(false, message = "Gagal terhubung ke server (Status ${res.code}).")
            }
            val decoded = try {
                Json.decode(res.body).asJsonMap()
            } catch (e: JsonParseException) {
                val trimmed = res.body.trim()
                return if (trimmed == "Unauthorized") {
                    SubmitResult(
                        false,
                        message = "Akses Ditolak (Unauthorized). Pastikan Web App dideploy sebagai 'Anyone' (Siapa saja) atau token script diatur dengan benar.",
                    )
                } else {
                    val preview = if (trimmed.length > 150) trimmed.substring(0, 150) + "..." else trimmed
                    SubmitResult(false, message = "Response server tidak valid (bukan JSON). Hubungi Admin.\nResponse: $preview")
                }
            }
                ?: return SubmitResult(false, message = "Response server tidak valid (bukan JSON). Hubungi Admin.")
            SubmitResult(
                ok = decoded["ok"] == true,
                ticket = decoded["ticket"]?.toString(),
                isOverdueUpdate = decoded["isOverdueUpdate"] == true,
                message = decoded["message"]?.toString(),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SubmitResult(false, message = "Tidak bisa terhubung ke server. Periksa koneksi internet lalu coba kirim lagi. (${e.message ?: e})")
        }
    }
}
