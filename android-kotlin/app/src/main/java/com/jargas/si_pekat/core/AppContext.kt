package com.jargas.si_pekat.core

import android.app.Application

/** Pegangan global ke Application (dipakai singleton service yang butuh Context). */
object AppContext {
    lateinit var app: Application
        private set

    fun init(application: Application) {
        app = application
    }
}
