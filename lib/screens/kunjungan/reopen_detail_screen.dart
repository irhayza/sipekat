import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:geolocator/geolocator.dart';
import 'package:image_picker/image_picker.dart';
import 'dart:async';
import '../../services/app_session_cache.dart';
import '../../services/kunjungan_api_service.dart';
import '../../services/perbaikan_api_service.dart' hide ServerException, ApiException;
import '../../services/kunjungan_location_service.dart';
import '../../services/kunjungan_local_cache_service.dart';
import '../../services/kunjungan_image_compress_service.dart';
import '../../theme/app_theme.dart';
import '../../utils/photo_metadata_resolver.dart';
import '../../widgets/customer_info_card.dart';
import '../../widgets/gps_banner.dart';
import '../../widgets/photo_capture_tile.dart';
import '../../config/app_config.dart';

class ReopenDetailScreen extends StatefulWidget {
  final VisitCustomer customer;

  const ReopenDetailScreen({
    super.key,
    required this.customer,
  });

  @override
  State<ReopenDetailScreen> createState() => _ReopenDetailScreenState();
}

class _ReopenDetailScreenState extends State<ReopenDetailScreen> {
  final _formKey = GlobalKey<FormState>();

  String _actionType = 'BUKA SEGEL';
  final _noMgrtBaruCtrl = TextEditingController();
  final _stMgrtCtrl = TextEditingController();

  File? _photoBefore;
  File? _photoAfter;
  DateTime? _photoBeforeTime;
  DateTime? _photoAfterTime;

  Position? _gpsPosition;
  bool _gpsServiceEnabled = true;
  String? _gpsPermissionWarning;
  bool _saving = false;
  String? _savingStep;

  @override
  void initState() {
    super.initState();
    _preFetchGps();
  }

  @override
  void dispose() {
    _noMgrtBaruCtrl.dispose();
    _stMgrtCtrl.dispose();
    super.dispose();
  }

  Future<void> _preFetchGps() async {
    final status = await KunjunganLocationService.ensurePermission();
    if (!mounted) return;

    String? warning;
    if (status == LocationReadyStatus.permissionDenied) {
      warning = 'Izin lokasi belum diberikan. Ketuk untuk memberi izin.';
    } else if (status == LocationReadyStatus.permissionDeniedForever) {
      warning = 'Izin lokasi ditolak permanen. Ketuk untuk membuka Pengaturan Aplikasi.';
    }

    setState(() {
      _gpsServiceEnabled = status != LocationReadyStatus.serviceDisabled;
      _gpsPermissionWarning = warning;
    });

    if (status != LocationReadyStatus.ready) return;

    try {
      final lastKnown = await Geolocator.getLastKnownPosition();
      if (lastKnown != null && mounted) {
        setState(() => _gpsPosition = lastKnown);
      }

      final fresh = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        timeLimit: const Duration(seconds: 15),
      );
      if (mounted) {
        setState(() => _gpsPosition = fresh);
      }
    } catch (e) {
      if (kDebugMode) debugPrint('Pre-fetch GPS gagal: $e');
    }
  }

  Future<void> _pickPhoto(bool isBefore) async {
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.camera_alt_rounded),
              title: const Text('Kamera (Ambil Foto Baru)'),
              onTap: () => Navigator.pop(ctx, ImageSource.camera),
            ),
            ListTile(
              leading: const Icon(Icons.photo_library_rounded),
              title: const Text('Galeri (Pilih dari HP / WhatsApp)'),
              onTap: () => Navigator.pop(ctx, ImageSource.gallery),
            ),
          ],
        ),
      ),
    );

    if (source == null) return;

    final picker = ImagePicker();
    final xFile = await picker.pickImage(
      source: source,
      imageQuality: 55,
      maxWidth: 800,
    );

    if (xFile == null) return;

    final file = File(xFile.path);
    final capturedTime = await PhotoMetadataResolver.resolveCapturedTime(
      file: file,
      filename: xFile.name,
    );

    if (!mounted) return;
    setState(() {
      if (isBefore) {
        _photoBefore = file;
        _photoBeforeTime = capturedTime;
      } else {
        _photoAfter = file;
        _photoAfterTime = capturedTime;
      }
    });
  }

  Future<String?> _compressAndEncodeImage(File file) async {
    // Gunakan native codec — jauh lebih cepat
    return KunjunganImageCompressService.compressFileToBase64(file);
  }


  /// Menyimpan submission ke antrean offline lokal (dipakai kalau memang
  /// tidak ada koneksi). Format payload ini sengaja dipertahankan sama
  /// seperti sebelumnya karena [KunjunganLocalCacheService.syncPendingSubmissions]
  /// masih memutar ulang antrean ini lewat submitAction() lama — itu jalur
  /// yang paling teruji untuk retry di background, jadi tidak diutak-atik.
  Future<void> _queueOffline(Map<String, dynamic> payload) async {
    await KunjunganLocalCacheService.instance.savePendingSubmission(payload);
    payload['status'] = 'pending';
    await KunjunganLocalCacheService.instance.saveHistorySubmission(payload);
    await KunjunganLocalCacheService.instance.removeCustomerFromCache(widget.customer.idpel);
    if (!mounted) return;
    setState(() => _saving = false);
    _showSuccessDialog(isOffline: true);
  }

  Future<void> _submitForm() async {
    if (!_formKey.currentState!.validate()) return;

    if (_photoBefore == null || _photoAfter == null) {
      _showSnackbar('Foto sebelum dan sesudah pengerjaan wajib dilampirkan.', isError: true);
      return;
    }

    setState(() {
      _saving = true;
      _savingStep = 'Mengunci koordinat GPS...';
    });

    final gpsStatus = await KunjunganLocationService.ensurePermission();
    setState(() => _gpsServiceEnabled = gpsStatus != LocationReadyStatus.serviceDisabled);

    if (gpsStatus == LocationReadyStatus.serviceDisabled) {
      setState(() => _saving = false);
      _showSnackbar('GPS wajib aktif sebelum laporan dikirim.', isError: true);
      return;
    }

    if (gpsStatus == LocationReadyStatus.permissionDenied) {
      setState(() => _saving = false);
      _showSnackbar(
        'Izin lokasi ditolak. Berikan izin lokasi agar koordinat GPS dapat dicatat.',
        isError: true,
      );
      return;
    }

    if (gpsStatus == LocationReadyStatus.permissionDeniedForever) {
      setState(() => _saving = false);
      _showSnackbar(
        'Izin lokasi ditolak permanen. Buka Pengaturan Aplikasi > Izin > Lokasi, lalu aktifkan secara manual.',
        isError: true,
      );
      return;
    }

    if (_gpsPosition == null) {
      try {
        _gpsPosition = await Geolocator.getCurrentPosition(
          desiredAccuracy: LocationAccuracy.high,
          timeLimit: const Duration(seconds: 10),
        );
      } catch (e) {
        try {
          _gpsPosition = await Geolocator.getLastKnownPosition();
        } catch (_) {}

        if (_gpsPosition == null) {
          try {
            _gpsPosition = await Geolocator.getCurrentPosition(
              desiredAccuracy: LocationAccuracy.medium,
              timeLimit: const Duration(seconds: 5),
            );
          } catch (err) {
            setState(() => _saving = false);
            _showSnackbar('Gagal mendapatkan koordinat GPS: $e', isError: true);
            return;
          }
        }
      }
    }

    String pad(int n) => n < 10 ? '0$n' : '$n';
    String formatTimeStr(DateTime d) {
      return '${d.year}-${pad(d.month)}-${pad(d.day)} ${pad(d.hour)}:${pad(d.minute)}:${pad(d.second)}';
    }

    final lat = _gpsPosition?.latitude.toStringAsFixed(6) ?? '';
    final lng = _gpsPosition?.longitude.toStringAsFixed(6) ?? '';
    final timeBeforeStr = _photoBeforeTime != null ? formatTimeStr(_photoBeforeTime!) : '';
    final timeAfterStr = _photoAfterTime != null ? formatTimeStr(_photoAfterTime!) : '';
    final metadataStr = 'GPS: $lat, $lng | Sebelum: $timeBeforeStr | Sesudah: $timeAfterStr';
    final isPasang = _actionType == 'PASANG KEMBALI';

    // Bentuk payload lama tetap disiapkan (hanya dipakai untuk antrean
    // offline, lihat _queueOffline).
    final legacyPayload = <String, dynamic>{
      'idpel': widget.customer.idpel,
      'nama': widget.customer.nama,
      'alamat': widget.customer.alamat,
      'actionType': _actionType.toLowerCase(),
      'nomgrtBru': isPasang ? _noMgrtBaruCtrl.text.trim() : null,
      'stMgrt': isPasang ? _stMgrtCtrl.text.trim() : null,
      'metadata': metadataStr,
      'lat': lat,
      'lng': lng,
      'timeBefore': timeBeforeStr,
      'timeAfter': timeAfterStr,
    };

    // ── 1) Cek status tiket saat ini di server ────────────────────────────
    // [PINDAH KE FLUTTER] Aturan "hanya tiket OPEN/PROSES yang boleh
    // diproses" dulu ada di GAS dan gagal senyap kalau statusnya sudah
    // berubah (mis. sudah PROSES lewat dashboard). Sekarang Flutter yang
    // mengecek lebih dulu dan memberi pesan yang jelas kalau tidak valid.
    setState(() => _savingStep = 'Memeriksa status tiket di server...');
    bool assumedOffline = false;
    try {
      final found = await KunjunganApiService.instance.findRow(
        sheetName: AppConfig.pengaduanSheetName,
        idpel: widget.customer.idpel,
        keyColumn: 'IDPEL_RP',
        filterColumn: 'PENGADUAN_RP',
        filterValue: _actionType,
      );
      final currentStatus = (found.row['STATUS_RP']?.toString() ?? found.row['Status']?.toString() ?? '').trim().toUpperCase();
      if (currentStatus.isNotEmpty && currentStatus != 'OPEN' && currentStatus != 'PROSES') {
        setState(() => _saving = false);
        _showSnackbar(
          'Tiket pelanggan ${widget.customer.idpel} sudah berstatus $currentStatus, tidak bisa diproses ulang dari sini.',
          isError: true,
        );
        return;
      }
    } on ServerException catch (e) {
      // GAS tegas menjawab error (bukan soal koneksi) -> tampilkan apa
      // adanya, jangan diam-diam diantrekan offline.
      setState(() => _saving = false);
      _showSnackbar(e.message, isError: true);
      return;
    } on ApiException {
      assumedOffline = true; // kemungkinan besar tidak ada koneksi.
    } catch (_) {
      assumedOffline = true;
    }

    // ── 2) Kompres & encode foto ───────────────────────────────────────────
    setState(() => _savingStep = 'Mengompresi foto...');
    String? b64Before;
    String? b64After;
    try {
      // Kompres kedua foto paralel
      final results = await Future.wait([
        _compressAndEncodeImage(_photoBefore!),
        _compressAndEncodeImage(_photoAfter!),
      ]);
      b64Before = results[0];
      b64After  = results[1];
    } catch (e) {
      setState(() => _saving = false);
      _showSnackbar('Gagal memproses gambar: $e', isError: true);
      return;
    }

    if (b64Before == null || b64After == null) {
      setState(() => _saving = false);
      _showSnackbar('Format foto tidak didukung.', isError: true);
      return;
    }

    final offlinePayload = Map<String, dynamic>.from(legacyPayload)
      ..['fotoSebBase64'] = b64Before
      ..['fotoSesBase64'] = b64After;

    if (assumedOffline) {
      await _queueOffline(offlinePayload);
      return;
    }

    // ── 3) Upload foto paralel ────────────────────────────────────
    String urlSeb;
    String urlSes;
    setState(() => _savingStep = 'Mengunggah foto...');
    try {
      final uploadResults = await Future.wait([
        KunjunganApiService.instance.uploadPhoto(
          base64: b64Before!,
          filename: '${PhotoMetadataResolver.toFilenameStamp(_photoBeforeTime!)}_seb.jpg',
        ),
        KunjunganApiService.instance.uploadPhoto(
          base64: b64After!,
          filename: '${PhotoMetadataResolver.toFilenameStamp(_photoAfterTime!)}_ses.jpg',
        ),
      ]);
      urlSeb = uploadResults[0];
      urlSes = uploadResults[1];
    } on ServerException catch (e) {
      setState(() => _saving = false);
      _showSnackbar('Gagal upload foto: ${e.message}', isError: true);
      return;
    } on ApiException {
      await _queueOffline(offlinePayload);
      return;
    }

    // ── 4) Tentukan kolom yang ditimpa ─────────────────────────────────────
    // [PINDAH KE FLUTTER] Pemetaan actionType -> kolom yang diisi dulu ada
    // di submitAction() versi GAS. Sekarang Flutter yang memutuskan persis
    // kolom mana (nama header sheet) yang mau ditulis.
    final updates = <String, dynamic>{
      'STATUS_RP': 'SELESAI',
      'Tindakan_RP': _actionType,
      'Foto_Seb_RP': urlSeb,
      'Foto_Ses_RP': urlSes,
      if (timeBeforeStr.isNotEmpty) 'Waktu_Seb_RP': timeBeforeStr,
      if (timeAfterStr.isNotEmpty) 'Waktu_Ses_RP': timeAfterStr,
      if (lat.isNotEmpty) 'Latitude_RP': lat,
      if (lng.isNotEmpty) 'Longitude_RP': lng,
      if (isPasang) 'No_MGRT_Baru_RP': _noMgrtBaruCtrl.text.trim(),
      if (isPasang) 'Angka_MGRT_Baru_RP': _stMgrtCtrl.text.trim(),
      'Petugas_RP': AppSessionCache.instance.officerNama,
      
      // Fallback for older GAS if needed (can be kept or removed, keeping them safe)
      'Status': 'SELESAI',
      'Keterangan': _actionType,
      'Foto Sebelum': urlSeb,
      'Foto Sesudah': urlSes,
      if (timeBeforeStr.isNotEmpty) 'Waktu Sebelum': timeBeforeStr,
      if (timeAfterStr.isNotEmpty) 'Waktu Sesudah': timeAfterStr,
      if (lat.isNotEmpty) 'Latitude': lat,
      if (lng.isNotEmpty) 'Longitude': lng,
      if (isPasang) 'No MGRT Baru': _noMgrtBaruCtrl.text.trim(),
      if (isPasang) 'Angka MGRT Baru': _stMgrtCtrl.text.trim(),
      'Petugas': AppSessionCache.instance.officerNama,
      'Tindakan': _actionType,
    };

    setState(() => _savingStep = 'Menyimpan ke server...');
    try {
      await KunjunganApiService.instance.updateRowCells(
        sheetName: AppConfig.pengaduanSheetName,
        idpel: widget.customer.idpel,
        keyColumn: 'IDPEL_RP',
        filterColumn: 'PENGADUAN_RP',
        filterValue: _actionType,
        updates: updates,
      );

      // Kirim notifikasi ke grup WA/Telegram bahwa pembukaan telah selesai
      // (sama dengan alur Laporan Perbaikan).
      // Untuk Pembukaan, kolom 'Ticket' tidak ditarik dari KunjunganApiService, 
      // tapi server tidak memerlukannya karena ticket hanya text display di Telegram, 
      // jadi kita lewatkan idpel saja jika tidak ada tiket.
      unawaited(PerbaikanApiService.instance.notifySelesai(
        ticket: widget.customer.idpel, // Fallback pakai idpel
        idpel: widget.customer.idpel,
        nama: widget.customer.nama,
        pengaduan: widget.customer.kendala,
        petugas: AppSessionCache.instance.officerNama,
        tindakan: _actionType,
        fotoSeb: urlSeb,
        fotoSes: urlSes,
        lat: lat.isNotEmpty ? lat : null,
        lng: lng.isNotEmpty ? lng : null,
      ));

      final sentPayload = Map<String, dynamic>.from(offlinePayload)
        ..['fotoSebBase64'] = urlSeb
        ..['fotoSesBase64'] = urlSes
        ..['status'] = 'sent';
      await KunjunganLocalCacheService.instance.saveHistorySubmission(sentPayload);
      await KunjunganLocalCacheService.instance.removeCustomerFromCache(widget.customer.idpel);

      setState(() => _saving = false);
      _showSuccessDialog(isOffline: false);
    } on ServerException catch (e) {
      // Sekarang pesannya spesifik, mis. "ditemukan tapi status sudah
      // SELESAI" vs "IDPEL not found" — bukan lagi gagal generik.
      setState(() => _saving = false);
      _showSnackbar(e.message, isError: true);
    } on ApiException {
      await _queueOffline(offlinePayload);
    }
  }

  void _showSnackbar(String message, {bool isError = false}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError ? AppColors.danger : AppColors.selesai,
      ),
    );
  }

  void _showSuccessDialog({bool isOffline = false}) {
    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) {
        return AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          title: Text(
            isOffline ? 'Tersimpan Secara Lokal!' : 'Pembukaan Dicatat!',
            style: const TextStyle(fontWeight: FontWeight.bold),
            textAlign: TextAlign.center,
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                isOffline ? Icons.offline_pin_rounded : Icons.check_circle_rounded,
                size: 72,
                color: isOffline ? AppColors.proses : AppColors.selesai,
              ),
              const SizedBox(height: 16),
              Text(
                isOffline
                    ? 'Status pelanggan ${widget.customer.idpel} disimpan lokal karena offline. Jangan lupa sinkronisasi data saat online.'
                    : 'Status pelanggan ${widget.customer.idpel} berhasil diubah menjadi $_actionType ke server.',
                textAlign: TextAlign.center,
              ),
            ],
          ),
          actionsAlignment: MainAxisAlignment.center,
          actions: [
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.kunjungan,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
              onPressed: () {
                Navigator.pop(ctx);
                Navigator.pop(context);
              },
              child: const Text('OK'),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final isPasang = _actionType == 'PASANG KEMBALI';

    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'Form Pembukaan Aliran',
          style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
        ),
      ),
      body: SafeArea(
        child: _saving
            ? Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const CircularProgressIndicator(color: AppColors.kunjungan),
                    const SizedBox(height: 20),
                    Text(
                      _savingStep ?? 'Menyimpan...',
                      style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
              )
            : Form(
                key: _formKey,
                child: ListView(
                  padding: const EdgeInsets.all(16),
                  children: [
                    if (!_gpsServiceEnabled)
                      const Padding(
                        padding: EdgeInsets.only(bottom: 4),
                        child: GpsBanner(
                          state: GpsBannerState.warning,
                          message: 'Peringatan: GPS tidak aktif! Nyalakan lokasi perangkat Anda.',
                        ),
                      ),
                    if (_gpsServiceEnabled && _gpsPermissionWarning != null)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 4),
                        child: GpsBanner(
                          state: GpsBannerState.warning,
                          message: _gpsPermissionWarning!,
                          onTap: () async {
                            if (_gpsPermissionWarning!.contains('permanen')) {
                              await Geolocator.openAppSettings();
                            } else {
                              await _preFetchGps();
                            }
                          },
                        ),
                      ),
                    _buildSectionHeader('Detail Pelanggan'),
                    CustomerInfoCard(
                      idPelanggan: widget.customer.idpel,
                      nama: widget.customer.nama,
                      alamat: widget.customer.alamat,
                      telepon: widget.customer.telepon.isNotEmpty ? widget.customer.telepon : null,
                      kendala: widget.customer.kendala.isNotEmpty ? widget.customer.kendala : null,
                      lat: widget.customer.lat,
                      lng: widget.customer.lng,
                      extraRows: [MapEntry('No MGRT', widget.customer.nomgrt)],
                    ),
                    const SizedBox(height: 12),
                    _buildSectionHeader('Pilih Tindakan Pembukaan'),
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(8),
                        child: RadioGroup<String>(
                          groupValue: _actionType,
                          onChanged: (val) {
                            if (val != null) setState(() => _actionType = val);
                          },
                          child: const Column(
                            children: [
                              RadioListTile<String>(
                                title: Text('BUKA SEGEL',
                                    style: TextStyle(fontWeight: FontWeight.bold)),
                                value: 'BUKA SEGEL',
                                activeColor: AppColors.kunjungan,
                              ),
                              RadioListTile<String>(
                                title: Text('PASANG KEMBALI',
                                    style: TextStyle(fontWeight: FontWeight.bold)),
                                value: 'PASANG KEMBALI',
                                activeColor: AppColors.kunjungan,
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),
                    if (isPasang) ...[
                      _buildSectionHeader('Data MGRT Baru'),
                      Card(
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            children: [
                              TextFormField(
                                controller: _noMgrtBaruCtrl,
                                decoration: const InputDecoration(
                                  labelText: 'No MGRT Baru *',
                                  border: OutlineInputBorder(),
                                  prefixIcon: Icon(Icons.looks_one_outlined),
                                  hintText: 'Contoh: 87654321',
                                ),
                                validator: (v) => (v == null || v.trim().isEmpty)
                                    ? 'No MGRT Baru wajib diisi'
                                    : null,
                              ),
                              const SizedBox(height: 16),
                              TextFormField(
                                controller: _stMgrtCtrl,
                                keyboardType: TextInputType.number,
                                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                                decoration: const InputDecoration(
                                  labelText: 'Stand Meter Baru *',
                                  border: OutlineInputBorder(),
                                  prefixIcon: Icon(Icons.speed_outlined),
                                  hintText: 'Masukkan angka stand meter',
                                ),
                                validator: (v) => (v == null || v.trim().isEmpty)
                                    ? 'Stand Meter Baru wajib diisi'
                                    : null,
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 12),
                    ],
                    _buildSectionHeader('Dokumentasi Pembukaan'),
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          children: [
                            PhotoCaptureTile(
                              label: 'Foto Sebelum Pengerjaan *',
                              photo: _photoBefore,
                              capturedAt: _photoBeforeTime,
                              onCapture: () => _pickPhoto(true),
                            ),
                            const SizedBox(height: 16),
                            const Divider(height: 1),
                            const SizedBox(height: 16),
                            PhotoCaptureTile(
                              label: 'Foto Sesudah Pengerjaan *',
                              photo: _photoAfter,
                              capturedAt: _photoAfterTime,
                              onCapture: () => _pickPhoto(false),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 20),
                    SizedBox(
                      height: 52,
                      child: ElevatedButton.icon(
                        icon: const Icon(Icons.check_circle_rounded, size: 20),
                        label: const Text(
                          'Simpan Pembukaan',
                          style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                        ),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.kunjungan,
                          foregroundColor: Colors.white,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                        onPressed: _submitForm,
                      ),
                    ),
                    const SizedBox(height: 24),
                  ],
                ),
              ),
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.only(left: 4, bottom: 6),
      child: Text(
        title.toUpperCase(),
        style: const TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.bold,
          color: AppColors.muted,
          letterSpacing: 0.5,
        ),
      ),
    );
  }
}