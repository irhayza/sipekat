package com.jargas.si_pekat.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Scope level aplikasi untuk pekerjaan "kirim lalu lupakan" (mis. notifikasi) yang harus selesai walau layar sudah ditutup. */
object AppScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO)
