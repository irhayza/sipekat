package com.sipekat.app.config

object AppConfig {
    const val APP_VERSION = "v3.0.2"

    const val GAS_READ_SHEET_URL =
        "https://script.google.com/macros/s/AKfycbxU4wPDlkG7Ab4MYFJiYgiJ-MpBoXdfYnTLmmnH2ta0TKtROrV2sTpc5RvUVS3FYJ2x5w/exec"

    const val LOGIN_SHEET_NAME = "LOGIN"

    const val REPAIR_BRIDGE_URL =
        "https://script.google.com/macros/s/AKfycbxU4wPDlkG7Ab4MYFJiYgiJ-MpBoXdfYnTLmmnH2ta0TKtROrV2sTpc5RvUVS3FYJ2x5w/exec"

    const val VISIT_DRIVE_FOLDER_ID = "1OKYMEiGhx2s-Qq3NL-I0gemoAMh2h4hM"
    const val VISIT_BRIDGE_URL =
        "https://script.google.com/macros/s/AKfycbxU4wPDlkG7Ab4MYFJiYgiJ-MpBoXdfYnTLmmnH2ta0TKtROrV2sTpc5RvUVS3FYJ2x5w/exec"

    const val OCR_DRIVE_FOLDER_ID = "15qWsqFLufop5_bpthZOqa0jjJPui0o3H"
    const val OCR_BRIDGE_URL =
        "https://script.google.com/macros/s/AKfycbxU4wPDlkG7Ab4MYFJiYgiJ-MpBoXdfYnTLmmnH2ta0TKtROrV2sTpc5RvUVS3FYJ2x5w/exec?api=ocr"
    const val OCR_SHEET_NAME = "dbase"

    const val PENGADUAN_BRIDGE_URL =
        "https://script.google.com/macros/s/AKfycbxU4wPDlkG7Ab4MYFJiYgiJ-MpBoXdfYnTLmmnH2ta0TKtROrV2sTpc5RvUVS3FYJ2x5w/exec?api=pengaduan"
    const val PENGADUAN_SHEET_NAME = "REPORT"

    // Gemini Vision API
    const val GEMINI_API_KEY = "AQ.Ab8RN6KnBM0eQ8Yrey6lFzxR9Dw3cqGuDJnDtOlMgw7h0ldInA"
    const val GEMINI_MODEL = "gemini-flash-latest"

    const val CONNECTION_TIMEOUT_SEC = 30L
    const val UPLOAD_TIMEOUT_SEC     = 60L

    const val PHOTO_MAX_WIDTH   = 800
    const val PHOTO_QUALITY     = 55
    const val MAX_PHOTO_BYTES   = 20 * 1024 * 1024

    val MGRT_TRIGGERS = listOf(
        "penggantian mgrt", "pasang kembali", "catat meter manual",
        "cabut permanen", "cabut sementara"
    )
    const val MGRT_BARU_TRIGGER = "penggantian mgrt"

    const val STATUS_OPEN    = "OPEN"
    const val STATUS_PROSES  = "PROSES"
    const val STATUS_SELESAI = "SELESAI"

    const val METER_DIGIT_COUNT          = 5
    const val METER_MAX_PLAUSIBLE_USAGE  = 5000

    const val DB_NAME    = "sipekat.db"
    const val DB_VERSION = 2

    // SharedPreferences keys
    const val PREF_SESSION_USER              = "session_user"
    const val PREF_SESSION_NAMA              = "session_nama"
    const val PREF_SESSION_ROLE              = "session_role"
    const val PREF_CACHE_PERBAIKAN_OPTIONS   = "cache_perbaikan_options"
    const val PREF_CACHE_PERBAIKAN_TICKETS   = "cache_perbaikan_tickets"
    const val PREF_CACHE_KUNJUNGAN_LIST      = "cache_kunjungan_list"
    const val PREF_CACHE_PEMBUKAAN_LIST      = "cache_pembukaan_list"
    const val PREF_CACHE_OCR_CUSTOMERS       = "cache_ocr_customers"
    const val PREF_CACHE_DAPELL_CUSTOMERS    = "cache_dapell_customers"
    const val PREF_CACHE_LAST_LOADED_AT      = "cache_last_loaded_at"
}
