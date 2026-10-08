package com.jargas.si_pekat.core

import android.net.Uri
import com.jargas.si_pekat.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiException(message: String) : Exception(message)

/**
 * Kegagalan yang dibalas eksplisit oleh server (status/ok = false + pesan) — bukan soal koneksi.
 * Dipisah dari ApiException supaya UI bisa membedakan "coba lagi nanti" dari "data memang ditolak".
 */
class ServerException(message: String) : Exception(message)

/** Token sesi ditolak server (kedaluwarsa/tidak valid) — pengguna perlu keluar lalu masuk lagi. */
class AuthExpiredException(message: String) : Exception(message)

/**
 * Klien HTTP untuk Google Apps Script.
 * GAS membalas POST dengan 30x ke googleusercontent.com; redirect itu HARUS diikuti dengan GET
 * (bukan POST ulang). Logika ini sebelumnya diduplikasi di 3 service Flutter.
 */
object GasHttp {
    data class Raw(val code: Int, val body: String)

    private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)

    /** Menambahkan token sesi ke payload JSON (bila ada) supaya server bisa memberlakukan REQUIRE_TOKEN. */
    @Suppress("UNCHECKED_CAST")
    private fun withToken(payload: Any?): Any? {
        if (payload !is Map<*, *> || payload.containsKey("token")) return payload
        val token = SessionStore.token()
        if (token.isEmpty()) return payload
        return (payload as Map<String, Any?>) + ("token" to token)
    }

    /** URL baca sheet (CSV) lewat GAS, lengkap dengan token sesi. */
    fun sheetUrl(sheetName: String): String {
        val token = SessionStore.token()
        return "${AppConfig.GAS_READ_SHEET_URL}?action=read_sheet&sheet=${Uri.encode(sheetName)}" +
            if (token.isNotEmpty()) "&token=${Uri.encode(token)}" else ""
    }

    private val manual: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val auto: OkHttpClient = OkHttpClient.Builder().build()

    private fun OkHttpClient.withTimeout(sec: Int): OkHttpClient = newBuilder()
        .callTimeout(sec.toLong(), TimeUnit.SECONDS)
        .connectTimeout(sec.toLong(), TimeUnit.SECONDS)
        .readTimeout(sec.toLong(), TimeUnit.SECONDS)
        .writeTimeout(sec.toLong(), TimeUnit.SECONDS)
        .build()

    /** POST JSON, lalu ikuti redirect GAS dengan GET. IOException dibiarkan naik ke pemanggil. */
    suspend fun postJson(url: String, payload: Any?, timeoutSec: Int): Raw = withContext(Dispatchers.IO) {
        val body = Json.encode(withToken(payload)).toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()
        val (firstRaw, location) = manual.withTimeout(timeoutSec).newCall(request).execute().use {
            Raw(it.code, it.body?.string().orEmpty()) to it.header("Location")
        }
        if (firstRaw.code in REDIRECT_CODES && location != null) {
            auto.withTimeout(timeoutSec).newCall(Request.Builder().url(location).get().build())
                .execute().use { Raw(it.code, it.body?.string().orEmpty()) }
        } else {
            firstRaw
        }
    }

    /** GET biasa (redirect diikuti otomatis, seperti http.get di Dart). */
    suspend fun getText(url: String, timeoutSec: Int): Raw = withContext(Dispatchers.IO) {
        auto.withTimeout(timeoutSec).newCall(Request.Builder().url(url).get().build())
            .execute().use { Raw(it.code, it.body?.string().orEmpty()) }
    }
}
