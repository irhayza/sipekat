package com.sipekat.app

import android.app.Application
import com.sipekat.app.data.local.AppDatabase
import com.sipekat.app.data.local.SessionManager

class SiPekatApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val sessionManager: SessionManager by lazy { SessionManager(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SiPekatApp
            private set
    }
}
