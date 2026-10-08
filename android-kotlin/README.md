# SiPEKAT — versi Kotlin (Jetpack Compose)

Port dari repo Flutter `irhayza/sipekat` (v3.0.2), disesuaikan dengan `Code.gs` backend Anda (Google Apps Script,
Sheets, Drive). Semua modul sudah ada: Pengaduan, Perbaikan, Kunjungan/Pembukaan, Pencatatan Meter + OCR.

## 1. WAJIB sebelum memakai aplikasi: pasang server (`server/Code.patched.gs`)
Aplikasi ini **tidak lagi menyimpan kunci API apa pun** dan tidak mengunduh sheet LOGIN. Login, ganti password, dan OCR
cloud (Gemini) berjalan lewat server, jadi `Code.gs` harus diperbarui lebih dulu:

1. Apps Script → ganti isi `Code.gs` dengan `server/Code.patched.gs` (ringkasan perubahan: `server/Code.patch.diff`).
2. **Project Settings → Script Properties**, tambahkan:
   | Properti | Wajib | Keterangan |
   |---|---|---|
   | `GEMINI_API_KEY` | ya (untuk OCR cloud) | Kunci Gemini **baru**. Satu-satunya tempat kunci disimpan. |
   | `GEMINI_MODEL` | tidak | default `gemini-flash-latest` |
   | `GEMINI_LIMIT` | tidak | maks panggilan OCR per petugas per 6 jam, default 150 |
   | `TOKEN_TTL_DAYS` | tidak | masa berlaku token login, default 14 hari |
   | `REQUIRE_TOKEN` | tidak | `true` = semua aksi data wajib token (aktifkan setelah semua petugas memakai aplikasi baru) |
   | `ALLOW_LEGACY_LOGIN_READ` | tidak | `true` = sheet LOGIN masih boleh dibaca lewat GET, **hanya** selama aplikasi Flutter lama masih dipakai |
   (`TOKEN_SECRET` dibuat otomatis.)
3. **Deploy → Manage deployments → ikon pensil → Version: New version → Deploy.** URL `/exec` tetap sama.
   Akses: *Execute as: Me*, *Who has access: Anyone*.
4. Uji dari aplikasi: login → buka Pencatatan Meter → foto meter (kartu hasil menyebut "Gemini AI").

Uji server (tiruan layanan Apps Script, bukan Apps Script asli): `node server/test_gas.js` (44 pemeriksaan: token, login, ganti password,
Gemini, penjaga akses) dan `node server/test_contract.js` (23 pemeriksaan kontrak: payload yang dikirim aplikasi → Drive & Sheet).

## 2. Menjalankan aplikasi
Android Studio (Ladybug+), Gradle 8.11.1, AGP 8.7.3, Kotlin 2.0.21, minSdk 24. Salin `local.properties.example` →
`local.properties` (isi `sdk.dir`). Build rilis bertanda tangan: salin `keystore.properties.example` → `keystore.properties`.

> **Belum pernah dikompilasi di Android Studio** (dibuat tanpa Android SDK). Yang sudah diperiksa: 62 berkas bebas galat
> sintaks, semua impor antar-berkas ada, logika murni (CSV, sanitasi, model, tanggal, peringkat OCR, kolom Sheet) lulus uji kotlinc.
> Build pertama kemungkinan memunculkan galat kecil (impor, nama ikon). Uji unit: `./gradlew test`.
> Yang BELUM terbukti tanpa perangkat: kompres/EXIF/kamera/ML Kit/GPS pada Android sungguhan dan koneksi ke Apps Script asli.

## 3. Kesesuaian dengan Code.gs
| Panggilan aplikasi | Aksi Code.gs | Hasil pemeriksaan |
|---|---|---|
| login / ganti password | `login`, `changePassword` (baru) | Sebelumnya tidak ada di server → ditambahkan; password dicocokkan di server, ada pembatasan 10 gagal/15 menit |
| OCR cloud | `gemini_ocr` (baru) | Kunci di Script Properties; prompt & skema dikunci di server; token wajib; batas per petugas |
| baca sheet CSV | `doGet ?action=read_sheet` | Sesuai. Sheet `LOGIN`/`Log` kini diblokir |
| daftar tiket | `readSheet`/CSV REPORT | Sesuai (kolom `*_RP`) |
| dropdown | `get_dropdown_options` | Sesuai |
| pengaduan | `submit_pengaduan` | Server **tidak mengirim `isOverdueUpdate`** → ditambahkan; aplikasi tidak lagi memblokir tiket aktif sendiri (server yang memutuskan: <24 jam ditolak, ≥24 jam jadi pembaruan) |
| cek pelanggan | `get_customer_by_id`, `find_row` | Sesuai |
| status tiket Reopen | `find_row` + `filterColumn` | Server **mengabaikan filter** (baris lama ikut terbaca) → diperbaiki |
| simpan penanganan | `update_row_cells` | Sesuai. Perbaikan di aplikasi: MGRT **lama → `NoMGRT_RP`/`Angka_MGRT_RP`**, **baru → `*_Baru_RP`** (sebelumnya nilai lama tertulis di kolom "Baru" dan nomor meter baru hilang saat penggantian) |
| foto | `upload_photo` | Sesuai |
| notifikasi | `notify_selesai` | Sesuai; Reopen kini mengirim nomor tiket asli (sebelumnya IDPEL) |
| sinkron meter | `sync_ocr_dapel` | Sesuai. Perbaikan: `koordinat` tidak dikirim lagi (membuat `LATITUDE_OCR` berisi "lat, lng" dan `LONGITUDE_OCR` kosong), `EXIF_OCR` kini terisi, sinkron offline menulis **nama** petugas (sebelumnya email) ke `PERSONIL`, admin tidak menimpa `PERSONIL` |
| `doPost` | — | Bug lama: variabel `payload` tidak terbaca di blok `catch` → setiap galat menjadi halaman HTML, bukan JSON → diperbaiki |

Hardening server lain: `readSheet/insertRow/updateRow/uploadFile` lama tidak lagi menerima `sheetId`/`folderId` dari klien dan
tidak bisa menyentuh `LOGIN`; `sync_ocr_dapel` memakai spreadsheet & folder tetap.

## 3b. Hasil pemeriksaan menyeluruh Flutter → Kotlin
**Cakupan.** Flutter: 57 berkas Dart (15.034 baris). Semua berkas yang benar-benar dipakai aplikasi sudah punya padanan Kotlin
(65 berkas, 8.559 baris — lebih ringkas karena `GasHttp`, `SessionStore`, parser CSV, dan komponen Compose dipakai bersama).
Pencocokan literal: tidak ada nama kolom/aksi Dart yang hilang di Kotlin (yang tidak ada hanya data tiruan, backend lokal lama,
dan skema Gemini yang kini ada di server).

**Kode mati di Flutter (tidak pernah terjangkau, tidak dipindahkan apa adanya):** `pending_sync_screen.dart`,
`ocr_image_compress_service.dart`, `OcrApiService.getCustomer/submitReading` (backend lokal `192.168.1.100`),
`PerbaikanApiService.updatePenyelesaian`, tab Riwayat/Akun di dashboard Kunjungan, tab Profil di beranda Meter.
Di Kotlin riwayat Kunjungan + sinkronisasi dan Ganti Password dibuat dapat dijangkau.

**Alur foto → Drive → Sheet:**
| Tahap | Implementasi Kotlin | Status |
|---|---|---|
| Ambil foto | kamera (`TakePicture` + FileProvider) / galeri (`PickVisualMedia`), maks 20 MB, tolak berkas bukan gambar ("Format foto tidak didukung") | kode diperiksa; belum diuji di perangkat |
| Waktu foto | EXIF asli → pola nama berkas (WA/kamera) → waktu sekarang | logika sama dengan Flutter |
| Kompres (Perbaikan/Kunjungan) | orientasi EXIF, **lebar 800 px** (sebelumnya sisi terpanjang → foto potret lebih kecil dari Flutter), JPEG q55, turun bertahap bila > 250 KB | diperbaiki di audit ini |
| Foto bukti meter | lebar 1600 px, JPEG q82, cap waktu/koordinat/IDPEL | sama dengan Flutter |
| Simpan sementara | kerja di cache (dibersihkan > 1 hari); **antrean offline di `filesDir`** (tahan lama, bukan cache; antrean Kunjungan sebelumnya berupa base64 di SharedPreferences yang membengkak) | diperbaiki di audit ini |
| Unggah | `upload_photo` → folder Drive tetap → URL `drive.google.com/file/d/...` (uji kontrak: byte identik) | lulus uji kontrak |
| Tulis Sheet | `update_row_cells` (REPORT `*_RP`, dbase `KJG/LINK_*`…), `sync_ocr_dapel` (OCRDAPEL) | lulus uji kontrak; nama kolom `*_RP` cocok dengan `CONFIG.HEADERS` |
| Kolom tak ditemukan | server **melewati diam-diam** dan tetap "success"; patch kini mengembalikan `skipped`, aplikasi menampilkan peringatan merah bila kolom wajib hilang | ditambahkan di audit ini |
| Koordinat | ditulis dengan koma desimal; pembaca memperbaiki format lokal ID (`-7123456` → `-7.123456`) | sama dengan Flutter |

**Catatan perilaku:** pengiriman foto Perbaikan/Pengaduan butuh internet (tidak ada antrean offline, sama seperti Flutter);
Kunjungan/Pembukaan dan Pencatatan Meter punya antrean offline.

## 4. Perubahan dibanding Flutter
Logout benar-benar menghapus sesi · tombol Riwayat di Pencatatan Meter berfungsi · Riwayat Pengaduan & Perbaikan terisi ·
Ganti Password terhubung ke server · layar dikunci portrait & perubahan konfigurasi tidak menghapus isian form ·
banner antrean offline di Kunjungan · pull-to-refresh · snackbar berwarna · tombol Kembali ditahan saat kirim ·
notifikasi tetap terkirim walau layar ditutup · tanda tangan rilis lewat `keystore.properties`.

## 5. Yang belum / perlu diketahui
- Password di sheet LOGIN masih teks biasa (format sheet tidak diubah). Sebaiknya diganti hash di tahap berikutnya.
- Selama `REQUIRE_TOKEN` belum `true`, aksi data lain (selain login/OCR/ganti password) masih bisa dipanggil tanpa token oleh
  siapa pun yang tahu URL `/exec`. Aktifkan setelah semua petugas memakai aplikasi ini.
- Header dashboard tidak mengecil saat digulir seperti di Flutter (kosmetik).
- Pasang menimpa APK Flutter hanya bila kunci tanda tangan sama; login diulang sekali; antrean Kunjungan lama tidak ikut
  (sinkronkan dulu). Antrean meter offline ikut terbaca (DB & skema sama).

## 6. Daftar uji manual
1. Login (GPS mati → galat; sandi salah → pesan server) · Profil → Ganti Password → login ulang dengan sandi baru.
2. Pengaduan: ID ≥ 8 digit mengisi nama/alamat; pelanggan dengan tiket <24 jam ditolak server; kirim → tiket muncul.
3. Perbaikan: "penggantian MGRT" menyimpan kolom lama **dan** baru; status SELESAI wajib 2 foto.
4. Kunjungan/Pembukaan: kirim online; mode pesawat → PENDING → online → Riwayat → "Sinkronkan Semua".
5. Pencatatan Meter: kamera (GPS wajib) & galeri; OCR Gemini (server) dan OCR lokal (matikan internet); simpan offline →
   online → terkirim otomatis; cek kolom `LATITUDE_OCR`, `LONGITUDE_OCR`, `EXIF_OCR`, `PERSONIL` di sheet dbase.
