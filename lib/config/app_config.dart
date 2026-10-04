/// Konfigurasi pusat SiPEKAT.
/// Setiap modul memiliki konfigurasi backend-nya sendiri, tapi login
/// berbagi satu spreadsheet yang sama.
class AppConfig {
  AppConfig._();

  static const String appVersion = 'v3.0.2';

  // ── GAS DATA ENDPOINT (FILTER-SAFE) ────────────────────────────────────
  // Endpoint Google Apps Script untuk membaca data sheet. Menggunakan
  // getValues() sehingga baris yang difilter/disembunyikan di UI tetap terbaca.
  // Lebih andal dari gviz/tq yang mengikuti status filter sheet.
  static const String gasReadSheetUrl =
      'https://script.google.com/macros/s/AKfycbxU4wPDlkG7Ab4MYFJiYgiJ-MpBoXdfYnTLmmnH2ta0TKtROrV2sTpc5RvUVS3FYJ2x5w/exec';

  // ── LOGIN (Shared across all modules) ─────────────────────────────────
  static const String loginSheetId   = '1-G0vVvkCnlWedHtWR5TIs_mxz_1wdVJjwMWMAshzy6U';
  static const String loginSheetName = 'LOGIN';

  // ── MODUL PERBAIKAN ────────────────────────────────────────────────────
  // Google Apps Script bridge untuk form penanganan/perbaikan.
  static const String repairBridgeUrl =
      'https://script.google.com/macros/s/AKfycbxU4wPDlkG7Ab4MYFJiYgiJ-MpBoXdfYnTLmmnH2ta0TKtROrV2sTpc5RvUVS3FYJ2x5w/exec';

  // ── MODUL KUNJUNGAN ────────────────────────────────────────────────────
  static const String visitSpreadsheetId    = '1-G0vVvkCnlWedHtWR5TIs_mxz_1wdVJjwMWMAshzy6U';
  static const String visitDriveFolderId    = '1OKYMEiGhx2s-Qq3NL-I0gemoAMh2h4hM';
  static const String visitBridgeUrl =
      'https://script.google.com/macros/s/AKfycbxU4wPDlkG7Ab4MYFJiYgiJ-MpBoXdfYnTLmmnH2ta0TKtROrV2sTpc5RvUVS3FYJ2x5w/exec';

  // ── MODUL PENCATATAN METER (OCR) ───────────────────────────────────────
  static const String ocrDriveFolderId  = '15qWsqFLufop5_bpthZOqa0jjJPui0o3H';
  static const String ocrBridgeUrl =
      'https://script.google.com/macros/s/AKfycbxU4wPDlkG7Ab4MYFJiYgiJ-MpBoXdfYnTLmmnH2ta0TKtROrV2sTpc5RvUVS3FYJ2x5w/exec?api=ocr';
  static const String ocrSheetName = 'dbase';
  static const String baseUrl = 'http://192.168.1.100:3000';
  static const String apiUrl  = '$baseUrl/api';
  static const String ocrServiceUrl = '';

  // Gemini Vision API untuk OCR stand meter otomatis
  static const String geminiApiKey = 'AQ.Ab8RN6IRHQKNo3ic56mTMvST9Ijzzw9eOQAzrkCtxG9lYVKTRw';
  static const String geminiModel = 'gemini-flash-latest';

  // ── MODUL PENGADUAN ────────────────────────────────────────────────────
  static const String pengaduanBridgeUrl =
      'https://script.google.com/macros/s/AKfycbxU4wPDlkG7Ab4MYFJiYgiJ-MpBoXdfYnTLmmnH2ta0TKtROrV2sTpc5RvUVS3FYJ2x5w/exec?api=pengaduan';

  // Sheet sumber data tiket Laporan Perbaikan (kolom sesuai sheet "Pengaduan")
  static const String pengaduanSheetId   = '1-G0vVvkCnlWedHtWR5TIs_mxz_1wdVJjwMWMAshzy6U';
  static const String pengaduanSheetName = 'REPORT';

  // ── TIMEOUT ────────────────────────────────────────────────────────────
  static const int connectionTimeoutSec = 30;
  static const int uploadTimeoutSec     = 60;

  // ── FOTO ───────────────────────────────────────────────────────────────
  static const int    photoMaxWidth = 800;    // turun dari 1080 → ukuran file lebih kecil
  static const double photoQuality  = 0.55;   // turun dari 0.6 → kompresi lebih agresif
  static const int    maxPhotoBytes = 20 * 1024 * 1024; // 20 MB

  // ── MODUL PERBAIKAN: MGRT triggers ────────────────────────────────────
  static const List<String> mgrtTriggers = [
    'penggantian mgrt',
    'pasang kembali',
    'catat meter manual',
    'cabut permanen',
    'cabut sementara',
  ];
  static const String mgrtBaruTrigger = 'penggantian mgrt';

  // ── STATUS ─────────────────────────────────────────────────────────────
  static const String statusOpen    = 'OPEN';
  static const String statusProses  = 'PROSES';
  static const String statusSelesai = 'SELESAI';

  // ── OCR ────────────────────────────────────────────────────────────────
  static const int meterDigitCount       = 5;
  static const int meterMaxPlausibleUsage = 5000;

  // ── SQLite ─────────────────────────────────────────────────────────────
  static const String dbName    = 'sipekat.db';
  static const int    dbVersion = 2; // Naikkan ke versi 2 untuk mengakomodasi tabel OCR
}
