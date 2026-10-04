# 📋 Blueprint Migrasi Logika: Code.gs → Flutter (v3 — Terverifikasi ke Kode Flutter)

Dokumen ini adalah revisi dari `migration_plan.md`, disusun setelah membandingkan rencana dengan isi `Code.gs` (1596 baris) **dan** source Flutter `lib/` (dari `lib.rar`, versi app `v3.0.2`). Struktur besarnya tetap sama — pindahkan kalkulasi berat ke Flutter, `Code.gs` jadi ringan — tapi beberapa dugaan di revisi v2 sekarang bisa dipastikan benar atau salah karena sudah dicocokkan langsung ke kode aslinya.

**Prinsip yang direvisi:** *"Code.gs kurir data murni"* diubah jadi ***"Code.gs kurir data murni + penjaga gerbang murah."*** Kalkulasi berat (matematika, string building, loop besar) pindah ke Flutter. Tapi validasi murah (cek field wajib, cek duplikasi, cek kredensial) **tetap di server**, karena endpoint Apps Script Anda bersifat publik — siapa pun yang tahu URL-nya bisa POST langsung tanpa lewat APK sama sekali.

---

## ✅ Hasil Verifikasi ke `lib.rar`

| Dugaan di v2 | Status | Detail |
|---|---|---|
| Flutter memakai action `submit_pengaduan` → `doSubmitPengaduan` | **Terkonfirmasi** | `pengaduan_api_service.dart` baris 54: `body = jsonEncode({'action': 'submit_pengaduan', ...})`. Ada komentar `[FIX v9.7]` di file yang sama yang secara eksplisit menjelaskan action ini baru ditambahkan supaya submit tidak jatuh ke jalur webhook WAHA. |
| `get_active_tickets` sudah di-cache di `AppSessionCache` untuk pre-check duplikasi | **Sebagian salah, tapi kabar baik** | Tidak ada modul "Pengaduan" di `AppSessionCache` — yang ada `perbaikanTickets` (dimuat dari `getActiveTicketsFull()`). Tapi `repairBridgeUrl` dan `pengaduanBridgeUrl` di `app_config.dart` menunjuk ke **URL Apps Script & sheet `REPORT` yang sama persis**. Jadi `AppSessionCache.instance.perbaikanTickets` sebenarnya **sudah berisi data yang tepat** untuk pre-check ini — cuma belum dipakai di `pengaduan_screen.dart`. Lihat Fase 2 di bawah, ini implementasi siap pakai. |
| Sanitasi 4 karakter (`=+-@`) perlu ditambahkan di Flutter | **Sudah ada, tapi salah tempat** | `utils/sheet_sanitize.dart` sudah meniru persis `Validation.sanitize()` GAS (4 karakter, sama seperti rekomendasi v2). Tapi hanya dipakai di `perbaikan_screen.dart` (jalur `update_row_cells`) — **tidak dipanggil sama sekali** di `pengaduan_screen.dart` sebelum submit. |
| Validasi nomor telepon berisiko drift dari server | **Tidak masalah** | Regex di `pengaduan_screen.dart` (`^(08|\+62|62)\d{7,12}$`) sudah identik dengan `Validation.isValidPhone` di `Code.gs`. Tidak perlu diubah. |

### 🆕 Temuan Baru dari Kode Flutter

Saya buka `doSubmitPengaduan` sekali lagi dengan fokus ke bagian yang belum saya soroti di v2 — ternyata gapnya lebih dari sekadar "tidak ada cek duplikasi":

```js
// doSubmitPengaduan di Code.gs — dibandingkan dengan PengaduanService.submit
var newRow = [
  nowIso_(), ticket,
  String(data.idPelanggan || ''),
  String(data.nama || ''),       // ← tidak di-uppercase
  String(data.alamat || ''),     // ← tidak di-uppercase
  String(data.pengaduan || ''),  // ← tidak di-uppercase, TIDAK disanitasi
  String(data.telpon || ''),
  'OPEN',
  String(data.petugas || ''),
  '', '', '', '', '', '', '', '', '', '', ''
];
```

Dibanding `PengaduanService.submit` (jalur WA) yang selalu memanggil `.toUpperCase()` dan `Validation.sanitize()` untuk keempat field itu, `doSubmitPengaduan` (jalur Flutter) **tidak melakukan keduanya**. Dan karena `sanitizeForSheet()` di Flutter juga belum dipanggil di layar Pengaduan (lihat tabel di atas), tidak ada satu pun lapisan — client atau server — yang men-sanitasi input pengaduan baru dari app. Dampaknya dua:
1. **Data tidak konsisten** — baris dari WA selalu UPPERCASE, baris dari app bisa campur (mengganggu tampilan rekap & pencarian case-sensitive).
2. **Celah formula-injection kembali terbuka** khusus untuk baris yang masuk dari app — kalau pelanggan/petugas mengetik kendala diawali `=`, `+`, `-`, atau `@`, itu akan tersimpan mentah dan berpotensi dibaca sebagai formula oleh Google Sheets saat sel dibuka.

---

## ⚠️ Temuan Kritis (Baca Ini Dulu)

| # | Temuan | Dampak |
|---|--------|--------|
| 1 | **[Terkonfirmasi]** Dua jalur submit pengaduan: `PengaduanService.submit` (bot WA, ada cooldown 24 jam + uppercase + sanitize) vs `doSubmitPengaduan` (dipanggil Flutter via `submit_pengaduan`, **tidak punya ketiganya**). | Tiket duplikat tanpa terkontrol + data tidak konsisten + celah formula-injection, khusus untuk laporan yang masuk dari app. |
| 2 | `DapellService.getById` dan `LaloService.get` melakukan **linear scan** penuh sheet `dbase`/`LOLA` setiap submit (kalau cache kosong) — ini lebih berat daripada `TicketService.generateId` yang cuma satu `PropertiesService` get/set. | Diagnosis "ID generator = penyebab lambat" di Tahap 1 kurang akurat; potensi optimasi terbesar justru di sini, dan bisa dikerjakan tanpa nunggu Flutter. |
| 3 | Format tiket pendek `PGD-KD-YYMMDD-NNN` dipakai `handleOpen_` untuk filter area via `ticket.includes('-KD-')`, dan dibaca manual petugas di WhatsApp. | Kalau ID generator pindah ke HP pakai timestamp+random, format ini rawan berubah dan merusak fitur `#OPEN#KD`. |
| 4 | Kredensial API tagihan (`adminsdj`/`mgK` ke `ptgn.mdp.net.id`) dan (jika dipakai) service-account Drive tersimpan aman di Script Properties server-side saat ini. | Tidak boleh ikut pindah ke APK Flutter — APK bisa didekompilasi, kredensial akan bocor. |
| 5 | `Code.gs` membangun satu teks kanonik bertag HTML (`<b>`, `<i>`, `<code>`, `<a>`), lalu mengonversinya otomatis: apa adanya ke Telegram (`parse_mode: HTML`), diregex jadi markdown ke WhatsApp (`sendWhatsAppTo_`). | Kalau Flutter cuma menyusun markdown WhatsApp, notifikasi Telegram admin (`NOTIF_CHATID`) berisiko kehilangan format bold/italic. |
| 6 | Koordinat notifikasi berbeda sumber: `notifPengaduanBaru_` pakai `LaloService.get(idpel)` (lookup lokasi pelanggan tersimpan di sheet `LOLA`), `notifSelesai_` pakai GPS live petugas (`d.lat`/`d.lng`, sudah dikirim Flutter). | Hanya kasus kedua yang bisa 100% dibangun di HP tanpa panggilan server tambahan. |
| 7 | **[Baru]** Upload foto Perbaikan (`perbaikan_api_service.dart` → `update_penyelesaian`) masih base64-in-JSON, dan **tidak** memakai pola antrian offline yang justru sudah ada & jalan baik di modul OCR (`OcrSyncService` + `OcrLocalDbService`, pakai `connectivity_plus`). | Fase 4 (upload foto) tidak perlu membangun pola baru dari nol — tinggal meniru pola yang sudah terbukti di modul lain. |

---

## FASE 0: Pengaman & Baseline (BARU — Kerjakan Sebelum Fase 1)

Ini bukan bagian dari migrasi, tapi prasyarat supaya migrasi aman dijalankan di sistem yang sudah live dipakai petugas lapangan.

1. **Tambal `doSubmitPengaduan` sekarang juga — bukan cuma soal duplikasi.** Setelah dicek ke kode Flutter, gap-nya ada tiga sekaligus dibanding `PengaduanService.submit`: tidak ada cek cooldown 24 jam, tidak ada `.toUpperCase()`, tidak ada `Validation.sanitize()`. Perbaikan paling sederhana & paling aman: ubah `doSubmitPengaduan` supaya cukup memanggil `PengaduanService.submit(payload.data)` lalu bungkus hasilnya — bukan menduplikasi logika. Dengan begitu cuma ada satu jalur kebenaran untuk "apa itu pengaduan valid", dan WA + Flutter otomatis selalu konsisten ke depannya. Ini independen dari rencana Flutter — sebaiknya dikerjakan minggu ini, sebelum fase manapun di bawah.
2. **(Pelengkap, opsional) Panggil `sanitizeForSheet()` juga di sisi Flutter** sebelum mengirim `nama`/`alamat`/`pengaduan` di `pengaduan_screen.dart`. Fungsinya sudah ada di `utils/sheet_sanitize.dart` (dipakai `perbaikan_screen.dart`, tinggal di-import) — ini bukan pengganti perbaikan server, tapi lapis pertahanan tambahan yang murah untuk UX (petugas langsung lihat teks ter-uppercase di ringkasan sebelum kirim).
2. **Snapshot versi.** Sebelum mengubah `Code.gs` di tiap fase, buat *deployment version* baru di Apps Script (Deploy → Manage Deployments → New version) supaya bisa rollback satu klik kalau ada regresi.
3. **Backup sheet `REPORT` secara berkala** (export CSV harian) selama masa transisi — sampai Fase 6 (server Ubuntu) benar-benar stabil, sheet ini masih jadi sumber kebenaran satu-satunya.
4. **Siapkan mode "shadow test".** Untuk tiap kalkulasi yang dipindah ke Dart (terutama Fase 1 format tiket & Fase 3 denda), jalankan dulu kalkulasi Flutter berdampingan dengan hasil `Code.gs` lama, log kalau hasilnya beda, baru matikan kalkulasi lama setelah sekian hari tanpa selisih. Ini menegaskan prinsip yang sudah Anda tulis sendiri di rencana awal: *"dipastikan fungsi dan tampilannya 100% identik"* — bikin itu terukur, bukan cuma feeling.

---

## FASE 1: Waktu & ID Tiket (Revisi)

**Revisi rekomendasi:** biarkan generator ID tetap di server. Alasan: `TicketService.generateId` sudah ringan (satu `PropertiesService.getProperty`/`setProperty` di dalam `LockService` 10 detik, bukan loop), sudah atomik/anti-tabrakan by design, dan formatnya dipakai fitur lain (`handleOpen_`, log admin di WA). Beban sesungguhnya bukan di sini.

- **1.1 — Format tanggal (tetap jalan, risiko rendah).** Flutter format tanggal via `intl` lalu kirim string jadi. Cukup pastikan formatnya kompatibel dengan `parseTimestamp_` yang sudah mendukung ISO (`yyyy-MM-ddTHH:mm:ss`) *dan* `dd/MM/yyyy HH:mm:ss`, jadi kedua format aman diterima — tidak perlu ganti header sheet.
- **1.2 — (Opsional, lanjutan) Skema block-reservation** — hanya kerjakan kalau tujuan sebenarnya adalah *dukungan offline* (petugas submit saat tanpa sinyal), bukan sekadar kecepatan. Server memberi jatah nomor urut (misal 10 nomor) ke tiap device saat login; device pakai nomor dari jatah itu untuk submit offline, lalu minta jatah baru saat online lagi. Ini menjaga format `NNN` tetap pendek & sequential tanpa harus generate di server tiap submit. Jangan pakai skema timestamp+random kecuali format tiket pendek memang boleh diubah.
- **1.3 — Quick win independen (bisa jalan duluan, tidak perlu ubah Flutter sama sekali):** cache hasil `DapellService.getById` dan `LaloService.get` dalam bentuk objek JSON tunggal di `CacheService` (refresh berkala, misal tiap 1–6 jam atau lewat trigger), ganti linear scan per-request jadi lookup di objek in-memory. Ini realistis mengurangi *latency* submit lebih besar daripada memindah ID generator, dan tidak butuh perubahan apa pun di sisi Flutter.

**Target file:** `pengaduan_api_service.dart`, `kunjungan_api_service.dart` (hanya untuk format tanggal).

---

## FASE 2: Validasi & Anti-Duplikasi (Revisi)

**Model hybrid, bukan pindah total — dan ini sudah bisa langsung diimplementasi:**
- **Di HP (UX instan, siap pakai sekarang):** `AppSessionCache.instance.perbaikanTickets` sudah dimuat saat login (lewat `getActiveTicketsFull()`) dan **berasal dari sheet `REPORT` yang sama** dengan yang ditulis `doSubmitPengaduan` (dikonfirmasi dari `app_config.dart`: `repairBridgeUrl` dan `pengaduanBridgeUrl` sama-sama menunjuk ke deployment & sheet yang identik). Di `pengaduan_screen.dart`, saat `_onIdChanged()` menemukan ID pelanggan, tambahkan satu pengecekan:
  ```dart
  final existing = AppSessionCache.instance.perbaikanTickets.where(
    (t) => t.idPelanggan.toLowerCase() == val.toLowerCase() &&
           t.status.toUpperCase() != 'SELESAI',
  ).toList();
  if (existing.isNotEmpty) {
    // tampilkan warning: "Pelanggan ini masih punya tiket ${existing.first.ticket} berstatus ${existing.first.status}"
  }
  ```
  Tidak perlu endpoint baru, tidak perlu request jaringan tambahan — datanya sudah ada di memori.
- **Di server (otoritatif, wajib tetap ada):** cek cooldown 24 jam + reuse tiket aktif **tetap dijalankan di `Code.gs`** sebelum insert — baik dari jalur Flutter maupun WA — supaya dua channel yang berbeda tidak saling membuat tiket duplikat untuk pelanggan yang sama (lihat Temuan #1). Ini bukan soal performa, tapi soal *satu sumber kebenaran* untuk status tiket.

**Detail sanitasi teks** — sinkronkan persis dengan `Validation.sanitize` yang sudah ada, bukan cuma tanda `=`:
```dart
String sanitize(String v) {
  final s = v.trim().toUpperCase();
  if (s.isEmpty) return s;
  const guardChars = ['=', '+', '-', '@'];
  return guardChars.contains(s[0]) ? "'$s" : s;
}
```

**Validasi telepon** — regex server saat ini menerima `08`, `+62`, maupun `62` di depan (`^(08|\+62|62)\d{7,12}$`). Kalau Flutter menormalkan ke `0` di depan sebelum kirim, pastikan konsisten dua arah: tampilan lokal boleh `08xxx`, tapi jangan sampai ada state di mana Flutter kirim `+62xxx` sedangkan validasi lokal sudah dirancang untuk `0xxx` saja — pilih satu standar penyimpanan (disarankan `08xxx` karena itu yang dipakai di seluruh sheet & pesan WA saat ini) dan taati di semua layar.

**Target file:** `pengaduan_screen.dart` + tambalan di `doSubmitPengaduan` (Fase 0).

---

## FASE 3: Kalkulasi Keuangan — Denda & Tunggakan (Revisi)

Fitur ini saat ini **hanya ada di jalur bot WhatsApp** (`#TAG#idpel` → `handleTag_`), belum ada endpoint untuk Flutter. Jadi Fase ini bukan "menyederhanakan yang sudah ada", tapi **menambah endpoint baru** sekaligus memindah kalkulasinya.

1. **Endpoint baru di `Code.gs`** (misal action `get_tunggakan_raw`): hanya melakukan `loginTunggakan_` + fetch ke `ptgn.mdp.net.id`, filter `lunas === 't'`, lalu kembalikan data mentah (`rptagihan`, `bulanrek`, dst) ke Flutter — **tanpa** hitung denda/admin. Kredensial (`adminsdj`/`mgK`) tetap tinggal di Script Properties, tidak pernah dikirim ke client.
2. **Kalkulator Dart** mengambil alih: denda Rp 15.000 untuk periode di bawah bulan berjalan, admin Rp 3.500 flat, total pakai `NumberFormat.currency(locale: 'id_ID', symbol: 'Rp ')`.
3. **⚠️ Jebakan konversi bulan (wajib diuji):** logika asli `Code.gs` ditulis untuk `Date.getMonth()` JS yang **0-indexed** (Jan=0):
   ```js
   const billMonth = currMonth === 0 ? 12 : currMonth; // currMonth: 0-11
   ```
   Kalau disalin mentah ke Dart, di mana `DateTime.now().month` itu **1-indexed** (Jan=1), logikanya harus disesuaikan:
   ```dart
   final currMonth = DateTime.now().month; // 1-12
   final billMonth = currMonth == 1 ? 12 : currMonth - 1;
   final billYear  = currMonth == 1 ? DateTime.now().year - 1 : DateTime.now().year;
   ```
   Salah konversi di sini berarti tagihan bisa salah dikenai/tidak dikenai denda 15rb — dampaknya langsung terlihat pelanggan. **Wajib** ada test case di batas bulan (1 Januari, 31 Desember) sebelum rilis, dan idealnya jalankan mode shadow test (Fase 0) dulu.
4. Pertimbangkan cache hasil tagihan per sesi di HP (misal 30 menit, meniru `CacheService` token 30 menit yang sudah ada di `loginTunggakan_`) supaya tidak berulang kali fetch ke API eksternal yang lambat.

**Keuntungan:** tetap seperti rencana awal — loading cek tagihan instan di HP, tanpa membocorkan kredensial.

---

## FASE 4: Upload Foto (Revisi — Dampak Terbesar, Perlu Hati-Hati Soal Kredensial)

Setuju ini prioritas dampak tertinggi. Revisi ada di *cara* direct upload-nya, bukan tujuannya.

- **Jangan** menanam service-account key Drive langsung di APK — itu bisa diekstrak siapa pun yang membongkar APK, dan scope-nya biasanya lebih luas dari sekadar satu folder upload.
- **Opsi A (tetap pakai Drive, lebih aman):** `Code.gs` mengeluarkan **token OAuth sementara** per sesi upload via `ScriptApp.getOAuthToken()`, dikirim ke Flutter untuk sekali pakai. Flutter melakukan *resumable upload* langsung ke Drive REST API pakai token itu. `Code.gs` tidak pernah lagi menyentuh body Base64 (tujuan awal tercapai), tapi kredensial tetap terkontrol & berumur pendek.
- **Opsi B (lebih standar untuk mobile):** pindah ke **Firebase Storage**. Didesain memang untuk direct-upload dari client mobile, dengan Firebase Auth + Security Rules per folder — biasanya lebih sederhana diimplementasi di Flutter dibanding hack OAuth Drive dari client. Trade-off: perlu setup Firebase project & migrasi struktur folder (`FOLDER_ID_SRC1`) kalau mau, atau cukup jalankan dua penyimpanan berdampingan sementara.
- **Kompresi 300KB via `flutter_image_compress`** — lanjutkan seperti rencana, sudah sejalan dengan `ImageCompressService` yang pernah dibuat di app lain.
- **Tambahan untuk konteks lapangan (Lamongan/Sidoarjo), dan ini tidak perlu dibangun dari nol:** modul OCR (`OcrSyncService` + `OcrLocalDbService`, sudah pakai `connectivity_plus`) sudah punya pola simpan-lokal-lalu-sync-saat-online yang jalan baik — cek konektivitas, baca antrian belum-tersinkron dari SQLite lokal, kirim satu per satu, hapus file lokal setelah sukses. Modul Perbaikan (`perbaikan_api_service.dart`) belum memakai pola ini sama sekali untuk foto sebelum/sesudah — upload masih langsung sekali tembak tanpa antrian. Meniru/mengekstrak pola `OcrSyncService` ke modul Perbaikan akan jauh lebih cepat dan konsisten daripada merancang mekanisme retry baru dari awal.
- Pertahankan (atau evaluasi ulang secara sadar, bukan default) kebijakan `ANYONE_WITH_LINK` VIEW yang dipakai sekarang — kalau pindah ke Firebase Storage, ini setara dengan public read rule per file.

---

## FASE 5: Perakitan Pesan (Revisi)

- **Flutter tetap menyusun satu teks kanonik**, tapi jangan langsung dalam format markdown WhatsApp final — pertahankan konvensi tag ringan (`<b>`, `<i>`, `<code>`, `<a href>`) seperti yang dipakai `Code.gs` sekarang, supaya `Code.gs` cukup menjalankan fungsi adapter yang **sudah ada dan murah** (`sendWhatsAppTo_` sudah punya regex converter HTML→markdown WA, `sendTelegramTo_` cukup kirim apa adanya dengan `parse_mode: HTML`). Dengan begini "perakitan data" (yang berat — kumpulkan field, format tanggal, format Rupiah) pindah ke Flutter, tapi "fan-out ke 2 channel dengan 2 format" tetap 1 fungsi kecil di server tanpa perlu Flutter tahu bedanya.
- **Koordinat — pisahkan dua kasus** (lihat Temuan #6):
  - Notifikasi **pengaduan baru**: koordinat berasal dari lokasi pelanggan terdaftar (`LaloService`, dari sheet `LOLA`), bukan GPS device pelapor. Ini **tidak bisa** sepenuhnya dibangun di Flutter kecuali data `LOLA` juga di-preload ke `AppSessionCache` — kalau belum, perlu endpoint ringan tambahan (`get_coordinat`) atau tetap biarkan `Code.gs` yang menempelkan link peta di notifikasi ini.
  - Notifikasi **selesai/penyelesaian**: koordinat dari GPS live petugas (`d.lat`/`d.lng`) — ini aman 100% dibangun di Flutter seperti rencana.
- **Quick win tambahan:** `notifSelesai_` punya fallback membaca ulang sheet kalau `waktuSesudah` tidak dikirim. Kalau Flutter (Fase 1) selalu mengirim `waktuSesudahStr` secara eksplisit, fallback baca-sheet ini bisa dihapus — satu lagi pengurangan beban `Code.gs` yang gratis.

---

## FASE 6: Independensi Total — Server Ubuntu Mandiri (Revisi & Lebih Rinci)

> Catatan penomoran: di dokumen asli, bagian ini disebut ulang sebagai "TAHAP 2" — bentrok dengan Tahap 2 di rencana Fase 1-5. Di sini saya beri nama **Fase 6** supaya urutannya jelas dan tidak tertukar.

Ini proyek terpisah & jauh lebih besar dari Fase 1-5 (yang murni pemindahan logika Flutter↔Sheets). Rencana infrastrukturnya (PostgreSQL/MySQL, Node.js/Express, Nginx, Cloudflared) sudah tepat arahnya. Berikut detail eksekusi yang belum ada di draft awal:

### 6.1 Skema Database
Rancang tabel berdasarkan struktur sheet yang sudah ada supaya migrasi data 1:1:
- `pengaduan` (mapping `CONFIG.HEADERS` di `REPORT` — 20 kolom yang sudah didefinisikan jadi acuan kolom tabel)
- `pelanggan` (dari sheet `dbase`)
- `lokasi_pelanggan` (dari sheet `LOLA`)
- `petugas` (dari sheet `Nama petugas`)
- `ticket_counter` (pengganti `PropertiesService` counter — bisa pakai sequence/auto-increment per `wilayah+tanggal`, atau tetap simpan sebagai baris counter kalau format `NNN` mau dipertahankan persis)
- `log` (pengganti sheet `Log` / `LoggerService`)

### 6.2 Strategi Migrasi Data (belum ada di draft awal)
1. Tulis skrip migrasi satu kali (Apps Script `exportData` → JSON/CSV → import script Node) untuk memindahkan histori sheet ke database baru.
2. Validasi jumlah baris & spot-check beberapa tiket lama secara manual sebelum go-live.
3. **Jalankan periode dual-write**: selama beberapa minggu, backend Node menulis ke Postgres/MySQL **dan** tetap menulis ke Google Sheets (via panggilan ke `Code.gs` yang masih hidup) sebagai jaring pengaman, sebelum Sheets benar-benar ditinggalkan. Ini menghindari skenario "database baru ternyata ada bug, tapi sudah tidak ada cadangan data".

### 6.3 Keamanan API Backend Baru
- Endpoint lama pakai token statis sederhana (`WEBHOOK_SECRET`). Untuk API Node/Express yang publik di `api.irsyam.my.id`, naikkan levelnya: JWT per-device atau per-akun petugas (bukan satu token dibagi rata), dengan expiry, supaya kalau satu APK/device hilang, aksesnya bisa dicabut sendiri-sendiri.
- Rate limiting di level Nginx/Express (mis. `express-rate-limit`) untuk endpoint publik, mengingat WA webhook & Flutter sama-sama akan menembak server yang sama.

### 6.4 Penyimpanan Foto Lokal
- Simpan di `/var/www/sipekat/uploads/`, tapi rencanakan **rotasi/backup** sejak awal — foto lapangan menumpuk terus, disk server bisa penuh dalam hitungan bulan. Siapkan cron `rsync`/backup ke storage terpisah (bisa juga tetap ke Google Drive sebagai arsip dingin, atau object storage murah).
- Pastikan Nginx men-serve folder ini dengan header cache yang wajar & tanpa listing folder (`autoindex off`).

### 6.5 Observability (Pengganti `LoggerService`)
- Sheet `Log` diganti proper logging: PM2 log rotation, atau library seperti `winston`/`pino` dengan retensi berbasis waktu. Tambahkan health-check endpoint (`/health`) yang bisa dipantau uptime monitor sederhana, mengingat WAHA + API + DB sekarang jadi satu titik kegagalan di satu server.

### 6.6 Rencana Rollback
- Jangan matikan Apps Script/Sheets sampai backend Node terbukti stabil minimal 2-4 minggu berjalan paralel. Simpan `WEBAPP_URL_FALLBACK` yang sudah ada di `CONFIG` sebagai jalur darurat kalau backend baru bermasalah saat jam kerja petugas lapangan.

---

## Urutan Eksekusi & Prioritas yang Disarankan

Urutan direvisi berdasarkan rasio *dampak nyata* terhadap *risiko perubahan*, bukan sekadar nomor tahap:

| Prioritas | Fase | Alasan |
|---|---|---|
| 1 (segera) | Fase 0 — tambal `doSubmitPengaduan` | Bug produksi aktif, independen dari migrasi |
| 2 | Fase 1.3 — cache `DapellService`/`LaloService` | Quick win murni di `Code.gs`, tidak perlu ubah Flutter, dampak performa nyata |
| 3 | Fase 4 — upload foto | Dampak terbesar sesuai identifikasi awal Anda, tapi eksekusi pakai token sementara/Firebase, bukan embed kredensial |
| 4 | Fase 2 — validasi & anti-duplikasi (server tetap otoritatif) | Menutup celah dari Temuan #1 |
| 5 | Fase 1.1 — format tanggal saja | Aman, dampak kecil, ID generator tetap di server |
| 6 | Fase 3 — kalkulasi keuangan | Perlu endpoint baru + hati-hati bug indeks bulan |
| 7 | Fase 5 — perakitan pesan | Risiko rendah tapi harus jaga kompatibilitas Telegram |
| 8 (proyek terpisah) | Fase 6 — server Ubuntu mandiri | Mulai setelah Fase 1-5 stabil, jalankan dual-write sebelum cutover |

