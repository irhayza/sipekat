package com.jargas.si_pekat.data

import com.google.gson.JsonParseException
import com.jargas.si_pekat.config.AppConfig
import com.jargas.si_pekat.core.GasHttp
import com.jargas.si_pekat.core.Json
import com.jargas.si_pekat.core.asJsonMap
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.io.InterruptedIOException

class AuthException(message: String) : Exception(message)

data class LoginResult(val nama: String, val email: String, val token: String)

/**
 * Autentikasi lewat server (action `login` / `changePassword` di Code.gs). Berbeda dari versi Flutter, sheet LOGIN
 * TIDAK lagi diunduh ke HP — pencocokan password dilakukan di server, dan server mengembalikan token sesi bertanda tangan.
 */
object AuthService {
    private suspend fun call(payload: Map<String, Any?>, timeoutSec: Int): Map<String, Any?> {
        try {
            val res = GasHttp.postJson(AppConfig.GAS_READ_SHEET_URL, payload, timeoutSec)
            if (res.code != 200) throw AuthException("Gagal terhubung ke server (Status ${res.code}).")
            val decoded = try {
                Json.decode(res.body).asJsonMap()
            } catch (e: JsonParseException) {
                null
            } ?: throw AuthException("Respons server tidak valid. Hubungi Admin.")
            val message = decoded["message"]?.toString()
            if (decoded["status"] == null && message?.startsWith("Unknown action") == true) {
                throw AuthException("Server belum diperbarui (aksi tidak dikenal). Pasang Code.patched.gs lebih dulu.")
            }
            return decoded
        } catch (e: AuthException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: InterruptedIOException) {
            throw AuthException("Koneksi timeout. Periksa koneksi internet Anda.")
        } catch (e: IOException) {
            throw AuthException("Tidak ada koneksi internet. Sambungkan ke internet lalu coba lagi.")
        }
    }

    suspend fun login(email: String, password: String): LoginResult {
        val d = call(mapOf("action" to "login", "user" to email.trim(), "password" to password), AppConfig.CONNECTION_TIMEOUT_SEC)
        if (d["status"] != "success") throw AuthException(d["message"]?.toString() ?: "Email atau password salah.")
        return LoginResult(
            nama = d["nama"]?.toString().orEmpty(),
            email = d["email"]?.toString().orEmpty(),
            token = d["token"]?.toString().orEmpty(),
        )
    }

    /** Ganti password akun [email] (token sesi dikirim otomatis oleh GasHttp). */
    suspend fun changePassword(email: String, oldPassword: String, newPassword: String) {
        val d = call(
            mapOf("action" to "changePassword", "user" to email, "oldPassword" to oldPassword, "newPassword" to newPassword),
            AppConfig.CONNECTION_TIMEOUT_SEC,
        )
        if (d["status"] != "success") throw AuthException(d["message"]?.toString() ?: "Gagal mengubah password.")
    }
}
