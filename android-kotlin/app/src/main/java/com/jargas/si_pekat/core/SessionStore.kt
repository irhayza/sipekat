package com.jargas.si_pekat.core

import android.content.Context
import android.content.SharedPreferences

/**
 * Penyimpanan sesi login (setara SharedPreferences di Flutter).
 * Satu pintu untuk simpan/hapus sesi → tidak ada lagi ketidakcocokan nama kunci
 * ('session_user' vs 'session_email') antara login dan logout.
 */
object SessionStore {
    private const val FILE = "sipekat_prefs"
    private const val KEY_USER = "session_user"
    private const val KEY_NAMA = "session_nama"
    private const val KEY_ROLE = "session_role"
    private const val KEY_TOKEN = "session_token"

    data class Session(val email: String, val nama: String)

    val prefs: SharedPreferences
        get() = AppContext.app.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): Session? {
        val email = prefs.getString(KEY_USER, null)
        val nama = prefs.getString(KEY_NAMA, null)
        return if (email != null && nama != null) Session(email, nama) else null
    }

    fun save(email: String, nama: String, token: String = "") {
        prefs.edit().putString(KEY_USER, email).putString(KEY_NAMA, nama).putString(KEY_TOKEN, token).apply()
    }

    /** Token sesi dari server (kosong bila sesi lama/belum login ulang). */
    fun token(): String = prefs.getString(KEY_TOKEN, null).orEmpty()

    fun clear() {
        prefs.edit().remove(KEY_USER).remove(KEY_NAMA).remove(KEY_ROLE).remove(KEY_TOKEN).apply()
    }
}
