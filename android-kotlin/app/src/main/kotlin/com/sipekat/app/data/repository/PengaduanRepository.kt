package com.sipekat.app.data.repository

import com.sipekat.app.config.AppConfig
import com.sipekat.app.data.remote.HttpClient

class PengaduanRepository {

    suspend fun checkCustomerId(id: String): Map<String, String>? {
        return try {
            val data = HttpClient.postJson(
                url  = AppConfig.PENGADUAN_BRIDGE_URL,
                body = mapOf("action" to "get_customer_by_id", "idpel" to id)
            )
            if (data["status"] == "success") {
                mapOf("nama" to (data["nama"]?.toString() ?: ""), "alamat" to (data["alamat"]?.toString() ?: ""))
            } else null
        } catch (_: Exception) { null }
    }

    suspend fun submitComplaint(data: Map<String, Any?>): Map<String, Any?> {
        return try {
            HttpClient.postJson(
                url  = AppConfig.PENGADUAN_BRIDGE_URL,
                body = mapOf("action" to "submit_pengaduan", "data" to data)
            )
        } catch (e: Exception) {
            mapOf("ok" to false, "message" to "Tidak bisa terhubung ke server. Periksa koneksi internet lalu coba kirim lagi.")
        }
    }
}
