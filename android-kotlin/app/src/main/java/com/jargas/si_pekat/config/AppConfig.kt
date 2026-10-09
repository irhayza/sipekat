package com.jargas.si_pekat.config

/**
 * Konfigurasi pusat SiPEKAT (port dari lib/config/app_config.dart).
 * Setiap modul punya konfigurasi backend sendiri, tetapi login memakai satu spreadsheet yang sama.
 */
object AppConfig {
    const val APP_VERSION = "v3.0.2"

    // ── GAS (Google Apps Script) ───────────────────────────────────────────
    // Satu deployment Web App dipakai semua modul; pembeda modul ada di query ?api=...
    private const val GAS_EXEC_URL = "https://script.google.com/macros/s/AKfycbxU4wPDlkG7Ab4MYFJiYgiJ-MpBoXdfYnTLmmnH2ta0TKtROrV2sTpc5RvUVS3FYJ2x5w/exec"

    /** Endpoint baca sheet (getValues → baris yang difilter/disembunyikan tetap terbaca). */
    const val GAS_READ_SHEET_URL = GAS_EXEC_URL

    // ── LOGIN (dibagi ke semua modul) ──────────────────────────────────────
    const val LOGIN_SHEET_ID = "1-G0vVvkCnlWedHtWR5TIs_mxz_1wdVJjwMWMAshzy6U"
    const val LOGIN_SHEET_NAME = "LOGIN"

    // ── MODUL PERBAIKAN ────────────────────────────────────────────────────
    const val REPAIR_BRIDGE_URL = GAS_EXEC_URL

    // ── MODUL KUNJUNGAN ────────────────────────────────────────────────────
    const val VISIT_SPREADSHEET_ID = "1-G0vVvkCnlWedHtWR5TIs_mxz_1wdVJjwMWMAshzy6U"
    const val VISIT_DRIVE_FOLDER_ID = "1OKYMEiGhx2s-Qq3NL-I0gemoAMh2h4hM"
    const val VISIT_BRIDGE_URL = GAS_EXEC_URL

    // ── MODUL PENCATATAN METER (OCR) ───────────────────────────────────────
    const val OCR_DRIVE_FOLDER_ID = "15qWsqFLufop5_bpthZOqa0jjJPui0o3H"
    const val OCR_BRIDGE_URL = "$GAS_EXEC_URL?api=ocr"
    const val OCR_SHEET_NAME = "dbase"

    /** Peninggalan backend lokal (tidak dipakai bila OCR_BRIDGE_URL terisi). */
    const val BASE_URL = "http://192.168.1.100:3000"
    const val API_URL = "$BASE_URL/api"
    const val OCR_SERVICE_URL = ""

    // OCR cloud (Gemini) dipanggil lewat server (action `gemini_ocr`); kunci API hanya ada di Script Properties GAS.

    // ── MODUL PENGADUAN ────────────────────────────────────────────────────
    const val PENGADUAN_BRIDGE_URL = "$GAS_EXEC_URL?api=pengaduan"
    const val PENGADUAN_SHEET_ID = "1-G0vVvkCnlWedHtWR5TIs_mxz_1wdVJjwMWMAshzy6U"
    const val PENGADUAN_SHEET_NAME = "REPORT"

    // ── TIMEOUT (detik) ────────────────────────────────────────────────────
    const val CONNECTION_TIMEOUT_SEC = 30
    const val UPLOAD_TIMEOUT_SEC = 60

    // ── FOTO ───────────────────────────────────────────────────────────────
    const val PHOTO_MAX_WIDTH = 800
    const val PHOTO_QUALITY = 0.55
    const val MAX_PHOTO_BYTES = 20 * 1024 * 1024

    // ── MODUL PERBAIKAN: pemicu MGRT ───────────────────────────────────────
    val mgrtTriggers = listOf(
        "penggantian mgrt",
        "pasang kembali",
        "catat meter manual",
        "cabut permanen",
        "cabut sementara",
    )
    const val MGRT_BARU_TRIGGER = "penggantian mgrt"

    // ── DAFTAR KUNJUNGAN ───────────────────────────────────────────────────
    /** Pelanggan dengan tunggakan 0 dan 1 bulan tidak ditampilkan di daftar Kunjungan (hanya ≥ 2 bulan). */
    const val MIN_ARREARS_MONTHS = 2

    /** Semua IDPEL berpanjang 10 digit → server baru ditanya setelah ID lengkap. */
    const val CUSTOMER_ID_LENGTH = 10

    // ── STATUS ─────────────────────────────────────────────────────────────
    const val STATUS_OPEN = "OPEN"
    const val STATUS_PROSES = "PROSES"
    const val STATUS_SELESAI = "SELESAI"

    // ── OCR ────────────────────────────────────────────────────────────────
    const val METER_DIGIT_COUNT = 5
    const val METER_MAX_PLAUSIBLE_USAGE = 5000

    // ── DB lokal ───────────────────────────────────────────────────────────
    const val DB_NAME = "sipekat.db"
    const val DB_VERSION = 2
}
