import 'dart:async';
import 'dart:io';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:geolocator/geolocator.dart';
import 'package:image_picker/image_picker.dart';
import '../../models/ocr/customer.dart';
import '../../models/ocr/meter_reading.dart';
import '../../services/ocr_api_service.dart';
import '../../services/ocr_gemini_service.dart';
import '../../services/ocr_local_db_service.dart';
import '../../services/ocr_meter_service.dart';
import '../../services/ocr_photo_proof_service.dart';
import '../../theme/app_theme.dart';
import '../../widgets/gps_banner.dart';
import 'history_screen.dart';

class MeterReadingScreen extends StatefulWidget {
  final Customer customer;
  final String officerNama;
  final String officerEmail;

  const MeterReadingScreen({
    super.key,
    required this.customer,
    required this.officerNama,
    required this.officerEmail,
  });

  @override
  State<MeterReadingScreen> createState() => _MeterReadingScreenState();
}

class _MeterReadingScreenState extends State<MeterReadingScreen> {
  final _standCtrl = TextEditingController();
  final _catatanCtrl = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  final _ocrService = MeterOcrService();
  final _geminiOcrService = GeminiOcrService();
  final _photoProofService = PhotoProofService();

  static const String _geminiSourceLabel = 'Gemini AI (Cloud)';

  File? _photo;
  DateTime? _photoCapturedAt;
  Position? _position;
  bool _ocrLoading = false;
  bool _saving = false;
  String? _statusMsg;
  bool _statusIsError = false;
  String? _ocrSource;
  String? _photoSource;
  String _selectedKondisi = '';
  bool _gpsServiceEnabled = true;

  String _ocrSourceLabel(String? source) {
    switch (source) {
      case 'camera':
        return 'OCR lokal (foto kamera)';
      case 'gallery':
        return 'OCR lokal (foto galeri)';
      case _geminiSourceLabel:
        return _geminiSourceLabel;
      default:
        return 'OCR lokal';
    }
  }

  @override
  void initState() {
    super.initState();
    _preFetchGps();
  }

  Future<void> _preFetchGps() async {
    try {
      final svcEnabled = await Geolocator.isLocationServiceEnabled();
      if (mounted) {
        setState(() => _gpsServiceEnabled = svcEnabled);
      }
      if (!svcEnabled) return;

      final lastKnown = await Geolocator.getLastKnownPosition();
      if (lastKnown != null && mounted) {
        setState(() => _position = lastKnown);
      }
      final fresh = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        timeLimit: const Duration(seconds: 15),
      );
      if (mounted) {
        setState(() {
          _position = fresh;
          _gpsServiceEnabled = true;
        });
      }
    } catch (e) {
      if (kDebugMode) debugPrint('Pre-fetch GPS gagal: $e');
    }
  }

  Future<bool> _isOnline() async {
    final results = await Connectivity().checkConnectivity();
    return results.any((result) => result != ConnectivityResult.none);
  }

  @override
  void dispose() {
    _standCtrl.dispose();
    _catatanCtrl.dispose();
    _ocrService.close();
    unawaited(_photoProofService.deleteFile(_photo));
    super.dispose();
  }

  Future<void> _pickImage(ImageSource source) async {
    final picker = ImagePicker();
    final xFile = await picker.pickImage(
      source: source,
      imageQuality: 75,   // sedikit lebih tinggi agar OCR masih akurat
      maxWidth: 1280,
    );
    if (xFile == null) return;

    try {
      final originalFile = File(xFile.path);
      final timestampResolution = await _photoProofService.resolveCapturedAt(
        originalFile,
        originalName: xFile.name,
      );
      final workingCopy = await _photoProofService.createWorkingCopy(
        originalFile,
        idpel: widget.customer.noPelanggan,
      );
      final previousPhoto = _photo;

      if (!mounted) {
        await _photoProofService.deleteFile(workingCopy);
        return;
      }

      setState(() {
        _photo = workingCopy;
        _photoCapturedAt = timestampResolution.value;
        _photoSource = source == ImageSource.camera ? 'camera' : 'gallery';
        _ocrSource = source == ImageSource.camera ? 'camera' : 'gallery';
        _ocrLoading = true;
      });
      _standCtrl.clear();

      await _photoProofService.deleteFile(previousPhoto);
      await _runPhotoOcr(source: _ocrSource!);
    } catch (error) {
      if (mounted) {
        setState(() => _ocrLoading = false);
      }
      _showMsg(
        'Gagal menyiapkan foto: ${error.toString().replaceFirst('Exception: ', '')}',
        isError: true,
      );
    }
  }

  int? get _previousStandForOcr {
    return widget.customer.standBulanLalu ?? widget.customer.standAwal;
  }

  Future<void> _runPhotoOcr({
    required String source,
  }) async {
    if (_photo == null) {
      _showMsg('Ambil foto terlebih dahulu', isError: true);
      return;
    }

    setState(() {
      _ocrLoading = true;
    });

    try {
      final online = await _isOnline();
      if (online) {
        try {
          final geminiResult = await _geminiOcrService.scanImage(_photo!);
          if (!mounted) return;
          if (geminiResult.hasDigits) {
            setState(() => _ocrLoading = false);
            _applyOcrResult(geminiResult, source: _geminiSourceLabel);
            return;
          }
          if (kDebugMode) debugPrint('Gemini AI tidak menemukan angka yang yakin, mencoba OCR lokal...');
        } catch (err) {
          if (kDebugMode) debugPrint('Gemini Direct OCR gagal, beralih ke OCR lokal: $err');
        }
      }

      final result = await _ocrService.scanImage(
        _photo!,
        previousStand: _previousStandForOcr,
      );
      if (!mounted) return;
      setState(() => _ocrLoading = false);
      _applyOcrResult(result, source: source);
    } catch (e) {
      if (!mounted) return;
      setState(() => _ocrLoading = false);
      _showMsg(
        'OCR gagal: ${e.toString().replaceFirst('Exception: ', '')}',
        isError: true,
      );
    }
  }

  void _applyOcrResult(MeterOcrResult result, {required String source}) {
    setState(() {
      _ocrSource = source;
      if (result.hasDigits) {
        _standCtrl.text = result.digits;
      }
    });

    final sourceLabel = _ocrSourceLabel(source);
    if (!result.hasDigits) {
      _showMsg(
        'Angka meter 4-5 digit belum terbaca dari $sourceLabel. Ambil ulang foto dengan fokus ke angka putih berlatar hitam, lalu pastikan area angka utama terlihat besar.',
        isError: true,
      );
      return;
    }

    if (result.isConfident) {
      _showMsg('Stand meter terbaca otomatis dari $sourceLabel: ${result.digits}');
    } else {
      _showMsg(
        'OCR otomatis dari $sourceLabel menemukan ${result.digits}. Mohon periksa ulang sebelum simpan.',
        isError: true,
      );
    }
  }

  Future<bool> _ensureGps({
    bool showSuccessMessage = true,
  }) async {
    final svcEnabled = await Geolocator.isLocationServiceEnabled();
    if (mounted) {
      setState(() => _gpsServiceEnabled = svcEnabled);
    }
    if (!svcEnabled) {
      _showMsg('GPS wajib aktif sebelum data bisa dikirim.', isError: true);
      return false;
    }

    if (_position != null) {
      return true;
    }

    try {
      var perm = await Geolocator.checkPermission();
      if (perm == LocationPermission.denied) {
        perm = await Geolocator.requestPermission();
      }

      if (perm == LocationPermission.denied) {
        _showMsg('Izin lokasi ditolak. GPS wajib diizinkan.', isError: true);
        return false;
      }

      if (perm == LocationPermission.deniedForever) {
        _showMsg(
          'Izin lokasi ditolak permanen. Aktifkan lagi dari pengaturan aplikasi.',
          isError: true,
        );
        return false;
      }

      final pos = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        timeLimit: const Duration(seconds: 15),
      );

      if (!mounted) return true;
      setState(() => _position = pos);

      if (showSuccessMessage) {
        _showMsg(
          'Lokasi didapat: ${pos.latitude.toStringAsFixed(6)}, '
          '${pos.longitude.toStringAsFixed(6)}',
        );
      }
      return true;
    } catch (e) {
      _showMsg('GPS gagal: $e', isError: true);
      return false;
    }
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _saving = true;
      _statusMsg = null;
      _statusIsError = false;
    });

    PreparedMeterPhoto? preparedPhoto;

    try {
      final isGallery = _photoSource == 'gallery';
      Position? positionToUse;

      if (!isGallery) {
        setState(() => _statusMsg = 'Mengambil lokasi GPS...');
        final gpsReady = await _ensureGps(showSuccessMessage: false);
        if (!gpsReady || _position == null) {
          throw Exception('GPS wajib aktif dan lokasi harus berhasil diambil sebelum simpan.');
        }
        positionToUse = _position;
      }

      if (_photo != null) {
        setState(() => _statusMsg = 'Menyiapkan foto bukti...');
        preparedPhoto = await _photoProofService.prepareForUpload(
          sourceFile: _photo!,
          idpel: widget.customer.noPelanggan,
          latitude: positionToUse?.latitude,
          longitude: positionToUse?.longitude,
          capturedAt: _photoCapturedAt,
        );
      }

      final readingTimestamp =
          preparedPhoto?.capturedAt ?? _photoCapturedAt ?? DateTime.now();
      final currentStand = int.parse(_standCtrl.text.trim());
      final prevStand = widget.customer.standBulanLalu ?? widget.customer.standAwal;
      final isMinus = currentStand < prevStand;

      final List<String> ketParts = [];
      if (_selectedKondisi.isNotEmpty) {
        ketParts.add(_selectedKondisi);
      }
      if (isMinus) {
        ketParts.add('Minus');
      }
      final String combinedKeterangan = ketParts.join(', ');

      final draftReading = MeterReading(
        pelangganId: widget.customer.noPelanggan,
        petugasId: widget.officerEmail,
        standAngka: currentStand,
        fotoPath: preparedPhoto?.file.path,
        lat: positionToUse?.latitude,
        lng: positionToUse?.longitude,
        catatan: combinedKeterangan.isEmpty ? null : combinedKeterangan,
        createdAt: readingTimestamp,
        minus: isMinus ? 1 : 0,
      );

      final online = await _isOnline();
      var readingToSave = draftReading;
      var remoteSynced = false;
      String dialogMessage;

      if (online && OcrApiService.instance.canSyncPendingReadings) {
        setState(() => _statusMsg = 'Mengirim foto dan update OCRDAPEL...');
        try {
          readingToSave = await OcrApiService.instance.syncPendingReading(
            draftReading,
            petugasName: widget.officerNama,
          );
          remoteSynced = true;
          dialogMessage =
              'Data OCR berhasil dikirim ke sheet OCRDAPEL dan foto sudah masuk Google Drive.';
        } catch (error) {
          dialogMessage =
              'Data disimpan lokal karena sinkronisasi OCRDAPEL gagal: '
              '${error.toString().replaceFirst('Exception: ', '')}';
        }
      } else if (online) {
        dialogMessage =
            'Data disimpan lokal. Sinkronisasi OCRDAPEL dan upload Google Drive '
            'belum aktif karena AppConfig.googleBridgeUrl masih kosong.';
      } else {
        dialogMessage =
            'Data disimpan lokal. Sinkronisasi OCRDAPEL bisa dilakukan nanti saat '
            'internet tersedia dan bridge Google sudah aktif.';
      }

      await OcrLocalDbService.instance.saveReading(readingToSave);

      if (remoteSynced) {
        await _photoProofService.deleteFile(preparedPhoto?.file);
      }
      if (_photo != null && _photo!.path != readingToSave.fotoPath) {
        await _photoProofService.deleteFile(_photo);
      }

      if (!mounted) return;
      setState(() {
        _photo = null;
        _photoCapturedAt = null;
        _saving = false;
        _statusMsg = null;
      });

      showDialog<void>(
        context: context,
        barrierDismissible: false,
        builder: (_) => AlertDialog(
          title: Text(remoteSynced ? 'Berhasil!' : 'Tersimpan Lokal'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                remoteSynced ? Icons.check_circle : Icons.save_outlined,
                color: remoteSynced ? AppColors.selesai : AppColors.proses,
                size: 60,
              ),
              const SizedBox(height: 12),
              Text(dialogMessage),
              const SizedBox(height: 8),
              Text(
                'Stand: ${readingToSave.standAngka}',
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: AppColors.meter,
                ),
              ),
              if (!remoteSynced && preparedPhoto != null)
                const Padding(
                  padding: EdgeInsets.only(top: 8),
                  child: Text(
                    'Foto bukti tetap disimpan sementara di perangkat agar bisa disinkronkan nanti.',
                    style: TextStyle(fontSize: 12, color: AppColors.muted),
                    textAlign: TextAlign.center,
                  ),
                ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.pop(context);
                Navigator.pop(context);
              },
              child: const Text('Selesai'),
            ),
          ],
        ),
      );
    } catch (e) {
      final preparedPath = preparedPhoto?.file.path;
      if (preparedPath != null && preparedPath != _photo?.path) {
        await _photoProofService.deleteFile(preparedPhoto?.file);
      }

      if (!mounted) return;
      setState(() {
        _saving = false;
        _statusMsg = null;
      });
      _showMsg(e.toString().replaceFirst('Exception: ', ''), isError: true);
    }
  }

  void _showMsg(String msg, {bool isError = false}) {
    setState(() {
      _statusMsg = msg;
      _statusIsError = isError;
    });
  }

  Widget _buildGpsStatusWidget() {
    final active = _gpsServiceEnabled;
    return GpsBanner(
      state: active ? GpsBannerState.ok : GpsBannerState.warning,
      message: active ? 'Kondisi GPS: Aktif' : 'Kondisi GPS: Tidak Aktif',
    );
  }

  @override
  Widget build(BuildContext context) {
    final c = widget.customer;
    final selectedStand = c.standBulanLalu ?? c.standAwal;

    return Scaffold(
      backgroundColor: const Color(0xFFF0F2F5),
      appBar: AppBar(
        backgroundColor: AppColors.meter,
        foregroundColor: Colors.white,
        title: Text(c.nama, style: const TextStyle(fontSize: 17)),
        actions: [
          IconButton(
            icon: const Icon(Icons.history),
            tooltip: 'Riwayat',
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => HistoryScreen(customer: c)),
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _Card(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _row(Icons.person, 'Nama - ID Pelanggan', '${c.nama} - ${c.noPelanggan}'),
                      _row(Icons.location_on, 'Alamat', c.alamat ?? '-'),
                      _row(Icons.gas_meter, 'No. Meter', c.noMeter ?? '-'),
                      _row(Icons.calendar_month, 'Stand Meter Terakhir', selectedStand.toString()),
                      _row(Icons.grid_view_rounded, 'Sektor', c.sektorLabel),
                    ],
                  ),
                ),
                const SizedBox(height: 8),
                _Card(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Text(
                        'Stand Sekarang (m3)',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          color: AppColors.ink,
                        ),
                      ),
                      const SizedBox(height: 6),
                      TextFormField(
                        controller: _standCtrl,
                        keyboardType: TextInputType.number,
                        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 4,
                          color: AppColors.meter,
                          fontFamily: 'monospace',
                        ),
                        decoration: InputDecoration(
                          hintText: '0 0 0 0 0',
                          filled: true,
                          fillColor: AppColors.line,
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(8),
                          ),
                          contentPadding: const EdgeInsets.symmetric(vertical: 10),
                        ),
                        onChanged: (_) => setState(() {}),
                        validator: (v) {
                          if (v == null || v.trim().isEmpty) {
                            return 'Stand meter wajib diisi';
                          }

                          final angka = int.tryParse(v.trim());
                          if (angka == null) return 'Hanya angka yang diizinkan';

                          return null;
                        },
                      ),
                      if (widget.customer.standBulanLalu != null && _standCtrl.text.isNotEmpty)
                        Padding(
                          padding: const EdgeInsets.only(top: 8),
                          child: Builder(
                            builder: (_) {
                              final curr = int.tryParse(_standCtrl.text) ?? 0;
                              final diff = curr - widget.customer.standBulanLalu!;
                              return Container(
                                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                                decoration: BoxDecoration(
                                  color: diff < 0 ? AppColors.dangerBg : AppColors.selesaiBg,
                                  borderRadius: BorderRadius.circular(6),
                                ),
                                child: Text(
                                  'Pemakaian: $diff m3',
                                  style: TextStyle(
                                    color: diff < 0 ? AppColors.danger : AppColors.selesai,
                                    fontWeight: FontWeight.bold,
                                    fontSize: 14,
                                  ),
                                  textAlign: TextAlign.center,
                                ),
                              );
                            },
                          ),
                        ),
                    ],
                  ),
                ),
                const SizedBox(height: 8),
                _Card(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Text(
                        'Foto Stand Meter',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          color: AppColors.ink,
                        ),
                      ),
                      const SizedBox(height: 8),
                      if (_photo != null)
                        Container(
                          height: 200,
                          width: double.infinity,
                          decoration: BoxDecoration(
                            color: Colors.black.withValues(alpha: 0.05),
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: AppColors.muted),
                          ),
                          child: ClipRRect(
                            borderRadius: BorderRadius.circular(8),
                            child: Image.file(
                              _photo!,
                              fit: BoxFit.contain,
                            ),
                          ),
                        )
                      else
                        Container(
                          height: 100,
                          width: double.infinity,
                          decoration: BoxDecoration(
                            color: AppColors.line,
                            border: Border.all(color: AppColors.muted),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: const Center(
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(Icons.camera_alt_outlined, color: AppColors.muted, size: 28),
                                SizedBox(height: 6),
                                Text(
                                  'Belum ada foto',
                                  style: TextStyle(color: AppColors.muted, fontSize: 13),
                                ),
                              ],
                            ),
                          ),
                        ),
                      const SizedBox(height: 10),
                      Row(
                        children: [
                          Expanded(
                            child: OutlinedButton.icon(
                              style: OutlinedButton.styleFrom(
                                side: const BorderSide(color: AppColors.meter),
                                foregroundColor: AppColors.meter,
                              ),
                              icon: const Icon(Icons.photo_library, size: 16),
                              label: const Text('Upload Galeri', style: TextStyle(fontSize: 12)),
                              onPressed: () => _pickImage(ImageSource.gallery),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: OutlinedButton.icon(
                              style: OutlinedButton.styleFrom(
                                side: const BorderSide(color: AppColors.meter),
                                foregroundColor: AppColors.meter,
                              ),
                              icon: const Icon(Icons.camera_alt, size: 16),
                              label: const Text('Ambil Foto', style: TextStyle(fontSize: 12)),
                              onPressed: () => _pickImage(ImageSource.camera),
                            ),
                          ),
                        ],
                      ),
                      if (_ocrLoading) ...[
                        const SizedBox(height: 8),
                        const Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            SizedBox(
                              width: 14,
                              height: 14,
                              child: CircularProgressIndicator(strokeWidth: 1.5),
                            ),
                            SizedBox(width: 8),
                            Text(
                              'Membaca OCR otomatis...',
                              style: TextStyle(fontSize: 11, color: AppColors.muted),
                            ),
                          ],
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 8),
                _Card(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Text(
                        'Status GPS & Kondisi',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          color: AppColors.ink,
                        ),
                      ),
                      const SizedBox(height: 8),
                      _buildGpsStatusWidget(),
                      const SizedBox(height: 12),
                      const Text(
                        'Kondisi Meter (Opsional)',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: AppColors.muted,
                        ),
                      ),
                      const SizedBox(height: 6),
                      DropdownButtonFormField<String>(
                        initialValue: _selectedKondisi,
                        items: [
                          const DropdownMenuItem(
                            value: '',
                            child: Text('Pilih Kondisi (Normal/Lainnya)', style: TextStyle(fontSize: 13)),
                          ),
                          ...[
                            'Rumah Terkunci',
                            'Meter Tertimbun',
                            'Meter Tidak Terjangkau',
                            'Ganti Meter',
                            'Meter Dicabut',
                          ].map((e) => DropdownMenuItem(value: e, child: Text(e, style: const TextStyle(fontSize: 13)))),
                        ],
                        decoration: InputDecoration(
                          filled: true,
                          fillColor: AppColors.line,
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(8),
                          ),
                          contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                        ),
                        onChanged: (v) {
                          setState(() {
                            _selectedKondisi = v ?? '';
                          });
                        },
                      ),
                    ],
                  ),
                ),
                if (_statusMsg != null) ...[
                  const SizedBox(height: 8),
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: _statusIsError ? AppColors.dangerBg : AppColors.selesaiBg,
                      border: Border.all(
                        color: _statusIsError ? AppColors.danger : AppColors.selesai,
                      ),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      _statusMsg!,
                      style: TextStyle(
                        color: _statusIsError ? AppColors.danger : AppColors.selesai,
                        fontSize: 12,
                      ),
                    ),
                  ),
                ],
                const SizedBox(height: 16),
                SizedBox(
                  height: 46,
                  child: ElevatedButton.icon(
                    icon: _saving
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(
                              color: Colors.white,
                              strokeWidth: 2,
                            ),
                          )
                        : const Icon(Icons.save, size: 18),
                    label: Text(
                      _saving ? (_statusMsg ?? 'Menyimpan...') : 'Simpan Data',
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.meter,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(10),
                      ),
                    ),
                    onPressed: (_saving || _ocrLoading) ? null : _save,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _row(IconData icon, String label, String value) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(
          children: [
            Icon(icon, size: 16, color: AppColors.muted),
            const SizedBox(width: 8),
            Text(
              '$label: ',
              style: const TextStyle(color: AppColors.muted, fontSize: 13),
            ),
            Expanded(
              child: Text(
                value,
                style: const TextStyle(
                  fontWeight: FontWeight.w600,
                  fontSize: 13,
                ),
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
      );
}

class _Card extends StatelessWidget {
  final Widget child;

  const _Card({required this.child});

  @override
  Widget build(BuildContext context) => Card(
        elevation: 2,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        child: Padding(padding: const EdgeInsets.all(16), child: child),
      );
}
