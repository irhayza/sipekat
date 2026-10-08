package com.jargas.si_pekat

import android.app.Application
import com.jargas.si_pekat.core.AppContext
import kotlinx.coroutines.launch

class SipekatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.init(this)
        // Bersihkan foto sementara/yatim di latar belakang.
        com.jargas.si_pekat.core.AppScope.launch { com.jargas.si_pekat.core.PhotoCache.purge() }
    }
}
