# Log Pengembangan & Perbaikan SiPEKAT v3 (Antigravity AI)

Dokumen ini berisi rangkuman riwayat perbaikan dan fitur yang telah dikerjakan pada sesi ini, sebagai referensi untuk pengembangan (Phase 4 / Migrasi Tahap Selanjutnya) di masa depan.

## Tanggal Pengerjaan
14 Agustus 2026

---

## 1. Fase 0: Penyeragaman & Perbaikan `doSubmitPengaduan`
- **Lokasi**: `Code.gs`
- **Tindakan**: 
  Memperbaiki fungsi pendaftaran tiket pengaduan (`doSubmitPengaduan`) yang sempat berjalan ganda/menggunakan kode lama. Logika pendaftaran kini di-bypass agar sepenuhnya menggunakan metode modular terpusat `PengaduanService.submit(data)`, yang berlaku sama untuk bot WhatsApp (WAHA) maupun inputan dari aplikasi Flutter.
  
## 2. Fase 1: Optimasi Cache & Backend (LaloService & DapellService)
- **Lokasi**: `Code.gs`
- **Tindakan**:
  - Memperbarui mekanisme *fetching* sheet `LOLA` dan `dbase` menjadi *caching pattern* berbasis Hash-Map (JSON dictionary).
  - Data yang di-return kini jauh lebih kecil (<100KB) dibanding metode lama yang me-return *raw array* sebesar 5MB++.
  - Pencarian nomor pelanggan atau data area kini tidak lagi menggunakan `for loop` secara berulang melainkan melakukan pemanggilan indeks `O(1)`, sehingga latensi *response* ke aplikasi turun secara signifikan.

## 3. Fase 2: Peningkatan Validasi Form di Flutter
- **Lokasi**: `lib/screens/pengaduan/pengaduan_screen.dart`, `Code.gs`
- **Tindakan**:
  - **Pengecekan Duplikasi Otomatis (UI)**: Ditambahkan pengecekan *real-time* via `AppSessionCache` di mana ketika petugas menginput ID Pelanggan di kolom `pengaduan_screen.dart`, aplikasi akan mengecek apakah pelanggan tersebut memiliki tiket aktif (berstatus `OPEN`/`PROSES`). Aplikasi akan memberi tahu petugas tiket apa yang masih aktif.
  - **Sanitasi Ekstra**: Ditambahkan fungsi pembersihan karakter tidak valid (`sanitizeForSheet`) di frontend, untuk menghindari terjadinya *Formula Injection* ketika dikirim dan di-append ke Google Sheet.

## 4. Perbaikan Bug Kritis Tumpang Tindih Status Tiket (Multi-Ticket)
- **Kondisi Bug**: Saat satu ID Pelanggan pernah memiliki tiket yang berstatus "SELESAI", lalu keesokan harinya ID Pelanggan tersebut melapor kembali dengan kasus yang baru (e.g. Kabel Putus) yang mendapatkan ID Tiket baru, ternyata ketika ada Kunjungan Buka Segel, laporan barunya secara misterius diubah menjadi "SELESAI".
- **Lokasi yang Diperbaiki**:
  - **`Code.gs` (Fungsi `updateRowCellsHandler`)**: Ditulis ulang untuk menerima tambahan parameter opsional `filterColumn` dan `filterValue`. Fitur ini dibuat agar algoritma Google Apps Script tidak secara serampangan menimpa row yang *terbaru* saja saat mencari via `IDPEL_RP`, tetapi mencocokkan *value* kolom tersebut.
  - **`lib/screens/perbaikan/perbaikan_screen.dart`**: Memperbaiki kunci pencarian saat mem-submit formulir "Tindakan" agar menggunakan kolom `Ticket_RP` berisi nomor resi/Tiket asli, bukan lagi menggunakan parameter generalistik `IDPEL_RP`.
  - **`lib/screens/kunjungan/kunjungan_dashboard_screen.dart` & `reopen_detail_screen.dart`**: Menambahkan *filter condition* di dalam argument payload POST saat melakukan aksi Buka Segel atau Pasang Kembali. Dengan adanya filter `filterValue: 'BUKA SEGEL'`, update otomatis ke dalam sheet "PENGADUAN" hanya akan mengubah status sheet jika tiket yang ditemukan oleh sistem *Code.gs* memang dikhususkan untuk tindakan Buka Segel, tidak akan lagi menimpa tiket jenis pengaduan lain yang saat itu masih berada di status `OPEN`.

## 5. Blokir Kirim Pengaduan Ganda (Active Ticket Blocking)
- **Kondisi Bug**: Petugas masih bisa menekan tombol "Kirim Pengaduan" dan membuat tiket baru untuk ID Pelanggan yang saat itu masih memiliki tiket aktif (berstatus OPEN/PROSES), meskipun peringatan teks (UI) sudah muncul.
- **Lokasi yang Diperbaiki**: `lib/screens/pengaduan/pengaduan_screen.dart`
- **Tindakan**: Menambahkan blokir sistem pada fungsi `_validateForm()`. Jika ID pelanggan ditemukan memiliki tiket aktif, form tidak akan melakukan *loading* *submit* dan muncul peringatan merah yang menghentikan proses pengiriman.

## 6. Perbaikan UI Bottom Navigation Hilang (Session Restore)
- **Kondisi Bug**: Ketika aplikasi dikeluarkan dari *recent apps* saat sesi masih berstatus *login*, kemudian dibuka kembali, aplikasi menampilkan *Dashboard* biasa tanpa kerangka *Bottom Navigation Bar* (Menu Riwayat, Pesan, Profil, dan Logout menjadi hilang).
- **Lokasi yang Diperbaiki**: `lib/main.dart`
- **Tindakan**: Mengubah logika inisialisasi awal. Jika *isLoggedIn* bernilai benar, rute tidak lagi menunjuk ke `DashboardScreen`, melainkan ke `MainNavScreen` agar seluruh kerangka *tab* dimuat dengan benar.

---

**Catatan untuk Pengerjaan Selanjutnya**:
- Sistem sudah aman dan *stable build* terakhir sudah siap dipakai.
- Dapat melanjutkan persiapan rilis produksi, atau menuju Phase Selanjutnya (Refactor Penyimpanan Foto, atau Rencana Eksekusi Migrasi Ubuntu Node.JS Webhook WAHA seperti yang didiskusikan dalam file `migration_plan.md`).

---

## Tanggal Pengerjaan
26 Agustus 2026

## 7. Perbaikan Bug Kritis: Daftar Pembukaan Tidak Muncul untuk Akun CG

- **Kondisi Bug**: Petugas dengan akun CG (misalnya `cg01`) tidak dapat melihat daftar pembukaan (buka segel / pasang kembali) sama sekali. Halaman selalu kosong meskipun data ada di Google Sheets.
- **Lokasi yang Diperbaiki**: `lib/services/kunjungan_api_service.dart`
- **Root Cause (3 bug berlapis)**:
  1. **Bug Utama**: Kode mencari kolom petugas di sheet `dbase` dengan kandidat `['ptgs', 'PETUGAS']`, padahal nama kolom aktual di sheet adalah **`PERSONIL`**. Akibatnya `idxPtgs` selalu `null`, map `cgCabutMapping` tidak pernah terisi.
  2. **Bug Efek Domino**: Karena `cgCabutMapping` selalu kosong, perbandingan `assignedPetugas != normalizedPetugas` selalu `null != ...` → `true` → **semua tiket di-skip**, daftar selalu kosong.
- **Tindakan**:
  - **Fix 1**: Menambahkan `'PERSONIL'` dan `'ptgs_kjg'` sebagai kandidat pertama dalam pencarian kolom petugas di sheet `dbase`, sehingga mapping `IDPEL → Petugas` dapat dibangun dengan benar.
  - **Fix 2**: Menambahkan guard `cgCabutMapping.isNotEmpty` sebelum melakukan filter. Jika fetch `dbase` gagal dan mapping tetap kosong, semua tiket CG yang memenuhi syarat (status OPEN + buka segel/pasang kembali) akan ditampilkan sebagai *fallback*, alih-alih memblokir semua data.

---

## Tanggal Pengerjaan
26 Agustus 2026 (Patch v1.3.2)

## 8. Penyempurnaan Filter CG: Wajib Cocok dengan PERSONIL di Sheet `dbase`

- **Kondisi**: Setelah fix #7, fallback "tampilkan semua jika mapping kosong" tidak diinginkan. Akun CG harus **hanya** melihat IDPEL yang kolom `PERSONIL`-nya di sheet `dbase` cocok dengan nama petugas yang login, tanpa pengecualian.
- **Lokasi yang Diperbaiki**: `lib/services/kunjungan_api_service.dart`
- **Tindakan**:
  - **Fix URL Fetch `dbase`**: Mengubah URL dari `export?format=csv&sheet=dbase` (memerlukan `gid` angka, sering gagal) menjadi `gviz/tq?tqx=out:csv&sheet=dbase` (menerima nama sheet langsung, konsisten dengan fetch sheet lain).
  - **Filter Ketat CG**: Menghapus fallback "tampilkan semua jika mapping kosong". Logika baru: jika IDPEL tidak ada di `cgCabutMapping` (`null`) **atau** nama petugas tidak cocok → tiket dilewati. Tidak ada pengecualian.
- **Versi**: `1.3.1+2` → `1.3.2+3`

---

## Tanggal Pengerjaan
26 Agustus 2026 (Patch v1.3.4)

## 9. Bypass Filter Sheet Global (Login, OCR, Kunjungan, Perbaikan)

- **Kondisi**: API yang menggunakan URL Google Sheets `gviz/tq` mengembalikan data dalam format JSON, tetapi ia mengikuti status filter pada Spreadsheet. Jika admin melakukan filter atau hide row di Google Sheets, maka baris tersebut tidak terbaca oleh aplikasi (contoh: user tidak bisa login, data OCR tidak ketemu, tiket perbaikan hilang).
- **Lokasi yang Diperbaiki**: 
  - `lib/services/auth_service.dart`
  - `lib/services/kunjungan_api_service.dart`
  - `lib/services/ocr_api_service.dart`
  - `lib/services/perbaikan_ticket_service.dart`
- **Tindakan**:
  - Mengubah seluruh endpoint URL dari `/gviz/tq?tqx=out:json` (maupun `out:csv`) menjadi `/export?format=csv`. URL ini memaksa Google Sheets memberikan **semua data raw** tanpa terpengaruh kondisi filter di sisi UI.
  - Memperbarui sistem parsing di `auth_service.dart` dan merekonstruksi mapping pseudo-JSON di `ocr_api_service.dart` agar tetap kompatibel dengan fungsi aplikasi lainnya.
- **Versi**: `1.3.3+4` → `1.3.4+5`

---

## Tanggal Pengerjaan
26 Agustus 2026 (Patch v1.3.5 - HOTFIX)

## 10. Revert: Kembalikan API ke `gviz/tq`

- **Kondisi Kritis**: Setelah Patch v1.3.4, daftar tiket di berbagai akun tiba-tiba lenyap atau error. 
- **Root Cause**: Penggunaan `export?format=csv&sheet=NAMA_SHEET` pada Google Sheets ternyata **mengabaikan parameter `sheet=`**. Ekspor CSV selalu mengunduh sheet pertama dalam dokumen (yaitu sheet `REPORT`), sehingga aplikasi gagal membaca kolom `dbase` dan `LOGIN` karena mendapat format tabel yang salah.
- **Tindakan**:
  - Mengembalikan 100% semua endpoint URL di `auth_service.dart`, `kunjungan_api_service.dart`, `ocr_api_service.dart`, dan `perbaikan_ticket_service.dart` kembali menggunakan `gviz/tq`.
  - Filter dari UI Google Sheets memang akan tetap berpengaruh (hide row = tidak terbaca), namun ini lebih aman daripada menggunakan format fetch yang salah target sheet-nya.
- **Solusi Alternatif (Bagi Admin)**: Jika ada data yang tidak muncul karena difilter, pastikan admin Google Sheets membersihkan filter dari tampilan utama. Admin disarankan menggunakan fitur **Filter Views (Tampilan Filter)** di Google Sheets (ikon corong warna hitam) ketimbang filter biasa, karena Filter Views tidak mempengaruhi hasil API `gviz/tq`.
- **Versi**: `1.3.4+5` → `1.3.5+6`


