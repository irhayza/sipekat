package com.sipekat.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.sipekat.app.config.AppConfig

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sipekat_prefs", Context.MODE_PRIVATE)

    var sessionUser: String?
        get() = prefs.getString(AppConfig.PREF_SESSION_USER, null)
        set(value) = prefs.edit().putString(AppConfig.PREF_SESSION_USER, value).apply()

    var sessionNama: String?
        get() = prefs.getString(AppConfig.PREF_SESSION_NAMA, null)
        set(value) = prefs.edit().putString(AppConfig.PREF_SESSION_NAMA, value).apply()

    var sessionRole: String
        get() = prefs.getString(AppConfig.PREF_SESSION_ROLE, "petugas") ?: "petugas"
        set(value) = prefs.edit().putString(AppConfig.PREF_SESSION_ROLE, value).apply()

    val isLoggedIn: Boolean
        get() = !sessionUser.isNullOrEmpty() && !sessionNama.isNullOrEmpty()

    fun logout() {
        prefs.edit()
            .remove(AppConfig.PREF_SESSION_USER)
            .remove(AppConfig.PREF_SESSION_NAMA)
            .remove(AppConfig.PREF_SESSION_ROLE)
            .remove(AppConfig.PREF_CACHE_PERBAIKAN_OPTIONS)
            .remove(AppConfig.PREF_CACHE_PERBAIKAN_TICKETS)
            .remove(AppConfig.PREF_CACHE_KUNJUNGAN_LIST)
            .remove(AppConfig.PREF_CACHE_PEMBUKAAN_LIST)
            .remove(AppConfig.PREF_CACHE_OCR_CUSTOMERS)
            .remove(AppConfig.PREF_CACHE_DAPELL_CUSTOMERS)
            .remove(AppConfig.PREF_CACHE_LAST_LOADED_AT)
            .apply()
    }

    // Cache helpers
    fun saveString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun getString(key: String): String? = prefs.getString(key, null)
    fun remove(key: String) = prefs.edit().remove(key).apply()
}
