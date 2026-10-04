package com.sipekat.app.data.remote

import com.sipekat.app.config.AppConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class ApiException(message: String)   : Exception(message)
class ServerException(message: String): Exception(message)

/** Shared OkHttp helper — semua repository pakai ini */
object HttpClient {
    val gson = Gson()
    val jsonType: MediaType = MediaType.parse("application/json; charset=utf-8")!!

    val default: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(AppConfig.CONNECTION_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(AppConfig.CONNECTION_TIMEOUT_SEC, TimeUnit.SECONDS)
        .writeTimeout(AppConfig.UPLOAD_TIMEOUT_SEC, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun postJson(
        client: OkHttpClient = default,
        url: String,
        body: Map<String, Any?>,
        timeoutSec: Long = AppConfig.CONNECTION_TIMEOUT_SEC
    ): Map<String, Any?> = withContext(Dispatchers.IO) {
        val jsonBody = gson.toJson(body)
        val request  = Request.Builder()
            .url(url)
            .post(RequestBody.create(jsonType, jsonBody))
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (e: SocketTimeoutException) {
            throw ApiException("Server tidak merespon (timeout). Periksa koneksi lalu coba lagi.")
        } catch (e: java.net.SocketException) {
            throw ApiException("Tidak ada koneksi internet. Sambungkan ke internet lalu coba lagi.")
        }

        if (!response.isSuccessful)
            throw ApiException("Server merespon status ${response.code()}. Coba lagi.")

        val bodyStr = response.body()?.string()
            ?: throw ApiException("Response body kosong.")

        if (bodyStr.trim().startsWith("<"))
            throw ApiException("Server mengembalikan HTML, bukan JSON. Periksa konfigurasi GAS deployment.")

        val type = object : TypeToken<Map<String, Any?>>() {}.type
        try {
            gson.fromJson(bodyStr, type)
        } catch (e: Exception) {
            throw ApiException("Server mengalami gangguan saat memproses permintaan. Coba lagi.")
        }
    }

    suspend fun getAsText(
        client: OkHttpClient = default,
        url: String
    ): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).get().build()
        val response = try {
            client.newCall(request).execute()
        } catch (e: SocketTimeoutException) {
            throw ApiException("Koneksi timeout. Periksa koneksi internet.")
        } catch (e: java.net.SocketException) {
            throw ApiException("Tidak ada koneksi internet.")
        }
        if (!response.isSuccessful)
            throw ApiException("Server merespon status ${response.code()}.")
        response.body()?.string() ?: ""
    }
}
