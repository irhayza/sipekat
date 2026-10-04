package com.sipekat.app.data.repository

import com.sipekat.app.config.AppConfig
import com.sipekat.app.data.remote.ApiException
import com.sipekat.app.data.remote.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

data class LoginResult(val nama: String, val email: String)

class AuthRepository {

    suspend fun login(userId: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        val url = "${AppConfig.GAS_READ_SHEET_URL}?action=read_sheet&sheet=${
            URLEncoder.encode(AppConfig.LOGIN_SHEET_NAME, "UTF-8")
        }"
        val body = HttpClient.getAsText(url = url)

        val lines = body.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        if (lines.isEmpty()) throw ApiException("Format data login tidak valid.")

        val emailLower  = userId.trim().lowercase()
        val passwordTrim= password.trim()

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.trim().isEmpty()) continue
            val cells       = parseCsvRow(line)
            val rowEmail    = cells.getOrElse(0) { "" }.lowercase().trim()
            val rowPassword = cells.getOrElse(1) { "" }.trim()
            val rowNama     = cells.getOrElse(2) { "" }.trim()

            if (rowEmail == emailLower && rowPassword == passwordTrim) {
                return@withContext LoginResult(nama = rowNama, email = rowEmail)
            }
        }
        throw ApiException("Email atau password salah.")
    }

    private fun parseCsvRow(line: String): List<String> {
        val result  = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"'); i++
                }
                c == '"'              -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { result.add(current.toString()); current.clear() }
                else                  -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
