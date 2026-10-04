import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:geolocator/geolocator.dart';
import 'package:image_picker/image_picker.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../config/app_config.dart';
import '../../models/perbaikan/dropdown_options.dart';
import '../../models/perbaikan/ticket.dart';
import '../../services/app_session_cache.dart';
import '../../services/perbaikan_api_service.dart';
import '../../services/perbaikan_ticket_service.dart';
import '../../services/image_service.dart';
import '../../services/location_service.dart';
import '../../theme/app_theme.dart';
import '../../utils/contact_launcher.dart';
import '../../utils/photo_metadata_resolver.dart';
import '../../utils/sheet_sanitize.dart';
import '../../widgets/empty_state.dart';
import '../../widgets/gps_banner.dart';
import '../../widgets/photo_capture_tile.dart';
import '../../widgets/section_card.dart';
import '../../widgets/status_badge.dart';
import '../../widgets/success_sheet.dart';
import '../../widgets/ticket_list_tile.dart';

// ─────────────────────────────────────────────────────────────────────────────
// Layar Daftar Tiket — menampilkan semua tiket aktif dari sheet Pengaduan
// ─────────────────────────────────────────────────────────────────────────────

class PerbaikanScreen extends StatefulWidget {
  const PerbaikanScreen({super.key});

  @override
  State<PerbaikanScreen> createState() => _PerbaikanScreenState();
}

class _PerbaikanScreenState extends State<PerbaikanScreen> {
  List<Ticket> _allTickets = [];
  List<Ticket> _filteredTickets = [];
  bool _loading = true;
  bool _refreshing = false;
  String? _error;
  final _searchCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _loadTickets();
    _searchCtrl.addListener(() => _applySearch(_searchCtrl.text));
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadTickets({bool forceRefresh = false}) async {
    setState(() {
      _loading = !_refreshing;
      _error = null;
    });
    try {
      final tickets = await PerbaikanTicketService.instance.getActiveTickets();
      if (!mounted) return;
      setState(() {
        _allTickets = tickets.where((t) {
          final k = t.kendala.toLowerCase();
          return !k.contains('buka segel') && !k.contains('pasang kembali');
        }).toList();
        _loading = false;
        _refreshing = false;
      });
      _applySearch(_searchCtrl.text);
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _refreshing = false;
        _error = e.toString().replaceFirst('Exception: ', '');
      });
    }
  }

  void _applySearch(String query) {
    final q = query.toLowerCase();
    setState(() {
      _filteredTickets = _allTickets.where((t) {
        return t.ticket.toLowerCase().contains(q) ||
            t.nama.toLowerCase().contains(q) ||
            t.idPelanggan.toLowerCase().contains(q) ||
            t.kendala.toLowerCase().contains(q);
      }).toList();
    });
  }

  void _openTindakan(Ticket ticket) async {
    // Sebelum masuk ke form, preload dropdown options
    DropdownOptions? options = AppSessionCache.instance.perbaikanOptions;
    if (options == null) {
      showDialog<void>(
        context: context,
        barrierDismissible: false,
        builder: (_) => const Center(child: CircularProgressIndicator()),
      );
      try {
        await AppSessionCache.instance.refreshPerbaikan();
        options = AppSessionCache.instance.perbaikanOptions;
      } catch (_) {}
      if (mounted) Navigator.of(context).pop();
    }

    if (!mounted) return;
    await Navigator.push<void>(
      context,
      MaterialPageRoute(
        builder: (_) => _TindakanScreen(
          ticket: ticket,
          options: options,
        ),
      ),
    );
    // Refresh list setelah kembali (tiket mungkin sudah SELESAI)
    setState(() => _refreshing = true);
    await _loadTickets(forceRefresh: true);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.canvas,
      appBar: AppBar(
        titleSpacing: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('Laporan Perbaikan',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
            Text('Tiket Aktif',
                style: TextStyle(fontSize: 11, color: Colors.white70)),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            tooltip: 'Muat ulang',
            onPressed: _loading
                ? null
                : () {
                    setState(() => _refreshing = true);
                    _loadTickets(forceRefresh: true);
                  },
          ),
        ],
      ),
      body: Column(
        children: [
          if (_refreshing) const LinearProgressIndicator(minHeight: 2),

          // ── Search bar ────────────────────────────────────────────────
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 12, 12, 4),
            child: TextField(
              controller: _searchCtrl,
              decoration: InputDecoration(
                hintText: 'Cari tiket, nama, IDPEL, atau pengaduan...',
                prefixIcon: const Icon(Icons.search_rounded, color: AppColors.brand),
                suffixIcon: _searchCtrl.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: _searchCtrl.clear,
                      )
                    : null,
              ),
            ),
          ),

          // ── Counter ───────────────────────────────────────────────────
          if (!_loading && _error == null)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
              child: Row(
                children: [
                  const Icon(Icons.assignment_rounded,
                      size: 13, color: AppColors.muted),
                  const SizedBox(width: 5),
                  Text(
                    '${_filteredTickets.length} tiket aktif',
                    style: const TextStyle(
                        fontSize: 12, color: AppColors.muted),
                  ),
                ],
              ),
            ),

          // ── List ──────────────────────────────────────────────────────
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                    ? EmptyState(
                        icon: Icons.cloud_off_rounded,
                        title: 'Gagal memuat tiket',
                        message: _error!,
                        actionLabel: 'Coba Lagi',
                        onAction: () => _loadTickets(forceRefresh: true),
                      )
                    : _filteredTickets.isEmpty
                        ? EmptyState(
                            icon: Icons.check_circle_outline_rounded,
                            title: _allTickets.isEmpty
                                ? 'Belum Ada Tiket Aktif'
                                : 'Tidak Ada Hasil',
                            message: _allTickets.isEmpty
                                ? 'Semua pengaduan sudah tertangani atau belum ada data.'
                                : 'Tidak ada tiket yang cocok dengan pencarian Anda.',
                          )
                        : RefreshIndicator(
                            onRefresh: () => _loadTickets(forceRefresh: true),
                            child: ListView.builder(
                              padding: const EdgeInsets.fromLTRB(12, 4, 12, 32),
                              itemCount: _filteredTickets.length,
                              itemBuilder: (ctx, i) {
                                final t = _filteredTickets[i];
                                return TicketListTile(
                                  ticket: t,
                                  onTap: () => _openTindakan(t),
                                );
                              },
                            ),
                          ),
          ),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Layar Form Tindakan & Dokumentasi — muncul ketika tiket diklik
// ─────────────────────────────────────────────────────────────────────────────

class _TindakanScreen extends StatefulWidget {
  final Ticket ticket;
  final DropdownOptions? options;

  const _TindakanScreen({required this.ticket, this.options});

  @override
  State<_TindakanScreen> createState() => _TindakanScreenState();
}

class _TindakanScreenState extends State<_TindakanScreen> {
  final _formKey = GlobalKey<FormState>();

  // Pilihan form
  String? _selectedJenis;
  String? _selectedTindakan;
  String _status = AppConfig.statusSelesai;

  // Controller MGRT
  final _noMgrtCtrl = TextEditingController();
  final _angkaMgrtCtrl = TextEditingController();
  final _noMgrtBaruCtrl = TextEditingController();
  final _angkaMgrtBaruCtrl = TextEditingController();

  // Foto
  File? _photoBefore;
  File? _photoAfter;
  DateTime? _photoBeforeTime;
  DateTime? _photoAfterTime;
  String? _photoBeforeBase64;
  String? _photoAfterBase64;

  // GPS
  Position? _gpsPosition;
  GpsBannerState? _gpsBannerState;
  String _gpsMessage = '';

  // Petugas
  String? _officerName;

  // Status layar
  bool _saving = false;
  String? _savingStep;

  DropdownOptions? _options;

  @override
  void initState() {
    super.initState();
    _options = widget.options ?? AppSessionCache.instance.perbaikanOptions;
    _initOfficer();
    unawaited(_prefetchGps());
  }

  @override
  void dispose() {
    _noMgrtCtrl.dispose();
    _angkaMgrtCtrl.dispose();
    _noMgrtBaruCtrl.dispose();
    _angkaMgrtBaruCtrl.dispose();
    super.dispose();
  }

  Future<void> _initOfficer() async {
    final prefs = await SharedPreferences.getInstance();
    if (!mounted) return;
    setState(() => _officerName = prefs.getString('session_nama'));
  }

  Future<void> _prefetchGps() async {
    final outcome = await LocationService.instance.getCurrentLocation();
    if (!mounted) return;
    if (outcome.isOk) setState(() => _gpsPosition = outcome.position);
  }

  // ── MGRT logic ────────────────────────────────────────────────────────────

  bool _isMgrtRequired() {
    final v = _selectedTindakan;
    if (v == null) return false;
    return AppConfig.mgrtTriggers.contains(v.toLowerCase().trim());
  }

  bool _isMgrtBaruRequired() {
    final v = _selectedTindakan;
    if (v == null) return false;
    return v.toLowerCase().trim() == AppConfig.mgrtBaruTrigger;
  }

  void _onTindakanChanged(String? val) {
    setState(() {
      _selectedTindakan = val;
      final needsMgrt =
          val != null && AppConfig.mgrtTriggers.contains(val.toLowerCase().trim());
      final needsMgrtBaru =
          val != null && val.toLowerCase().trim() == AppConfig.mgrtBaruTrigger;
      if (!needsMgrt) {
        _noMgrtCtrl.clear();
        _angkaMgrtCtrl.clear();
      }
      if (!needsMgrtBaru) {
        _noMgrtBaruCtrl.clear();
        _angkaMgrtBaruCtrl.clear();
      }
    });
  }

  // ── Foto ──────────────────────────────────────────────────────────────────

  Future<void> _pickPhoto(bool isBefore) async {
    final ImageSource? source = await showModalBottomSheet<ImageSource>(
      context: context,
      builder: (context) => SafeArea(
        child: Wrap(
          children: [
            ListTile(
              leading: const Icon(Icons.camera_alt),
              title: const Text('Ambil dari Kamera'),
              onTap: () => Navigator.of(context).pop(ImageSource.camera),
            ),
            ListTile(
              leading: const Icon(Icons.photo_library),
              title: const Text('Pilih dari Galeri'),
              onTap: () => Navigator.of(context).pop(ImageSource.gallery),
            ),
          ],
        ),
      ),
    );

    if (source == null) return;

    final picker = ImagePicker();
    XFile? xFile;
    try {
      xFile = await picker.pickImage(
          source: source, imageQuality: 90, maxWidth: 1920);
    } catch (e) {
      if (!mounted) return;
      _showSnack('Tidak bisa mengambil foto: $e', isError: true);
      return;
    }
    if (xFile == null) return;

    final file = File(xFile.path);
    final stat = await file.stat();
    if (stat.size > AppConfig.maxPhotoBytes) {
      if (!mounted) return;
      _showSnack('Ukuran foto terlalu besar (maks 20 MB).', isError: true);
      return;
    }

    final capturedTime = await PhotoMetadataResolver.resolveCapturedTime(
      file: file,
      filename: xFile.name,
    );

    if (!mounted) return;
    setState(() {
      if (isBefore) {
        _photoBefore = file;
        _photoBeforeTime = capturedTime;
        _photoBeforeBase64 = null;
      } else {
        _photoAfter = file;
        _photoAfterTime = capturedTime;
        _photoAfterBase64 = null;
      }
    });
  }

  Future<String> _encodePhoto({required bool isBefore}) async {
    final cached = isBefore ? _photoBeforeBase64 : _photoAfterBase64;
    if (cached != null) return cached;
    final file = isBefore ? _photoBefore : _photoAfter;
    if (file == null) throw Exception('Foto belum dipilih.');
    final encoded = await ImageService.instance.compressToBase64(file);
    if (mounted) {
      setState(() {
        if (isBefore) {
          _photoBeforeBase64 = encoded;
        } else {
          _photoAfterBase64 = encoded;
        }
      });
    }
    return encoded;
  }

  // ── Submit ────────────────────────────────────────────────────────────────

  Future<void> _submitForm() async {
    if (_saving) return;
    if (!_formKey.currentState!.validate()) return;

    if (_selectedJenis == null) {
      _showSnack('Pilih jenis pengaduan terlebih dahulu.', isError: true);
      return;
    }
    if (_selectedTindakan == null) {
      _showSnack('Pilih tindakan penanganan terlebih dahulu.', isError: true);
      return;
    }
    final isSelesai = _status == AppConfig.statusSelesai;
    if (isSelesai && (_photoBefore == null || _photoAfter == null)) {
      _showSnack('Foto sebelum & sesudah wajib untuk status SELESAI.', isError: true);
      return;
    }

    setState(() {
      _saving = true;
      _savingStep = 'Memeriksa status tiket di server...';
    });

    // ── Cek status tiket saat ini ───────────────────────────────────────────
    try {
      final found = await PerbaikanApiService.instance.findRow(
        sheetName: AppConfig.pengaduanSheetName,
        keyValue: widget.ticket.ticket,
        keyColumn: 'Ticket_RP',
      );
      final row = found;
      final currentStatus = (row['STATUS_RP']?.toString() ?? row['Status']?.toString() ?? '').trim().toUpperCase();
      if (currentStatus == AppConfig.statusSelesai.toUpperCase()) {
        if (!mounted) return;
        setState(() => _saving = false);
        _showSnack('Tiket ${widget.ticket.ticket} sudah berstatus SELESAI, tidak bisa diproses ulang.', isError: true);
        return;
      }
    } catch (e) {
      if (!mounted) return;
      setState(() => _saving = false);
      final msg = e is ApiException || e is ServerException ? e.toString() : 'Gagal memeriksa status tiket.';
      _showSnack('$msg Coba lagi.', isError: true);
      return;
    }

    setState(() => _savingStep = 'Mengunci titik GPS...');
    setState(() {
      _gpsBannerState = GpsBannerState.loading;
      _gpsMessage = 'Mengunci koordinat Anda...';
    });

    final gps = await LocationService.instance.getCurrentLocation();
    if (!mounted) return;
    setState(() {
      _gpsPosition = gps.isOk ? gps.position : null;
      _gpsBannerState = gps.isOk ? GpsBannerState.ok : GpsBannerState.warning;
      _gpsMessage = gps.isOk
          ? 'Lokasi terkunci: ${gps.position!.latitude.toStringAsFixed(5)}, '
              '${gps.position!.longitude.toStringAsFixed(5)}'
          : LocationService.instance.messageFor(gps.result);
    });

    String two(int n) => n < 10 ? '0$n' : '$n';
    String fmt(DateTime d) =>
        '${d.year}-${two(d.month)}-${two(d.day)} ${two(d.hour)}:${two(d.minute)}:${two(d.second)}';

    final lat = _gpsPosition?.latitude.toString() ?? '';
    final lng = _gpsPosition?.longitude.toString() ?? '';
    final locationStr = (lat.isNotEmpty && lng.isNotEmpty) ? '$lat,$lng' : '';
    final parts = locationStr.split(',');
    
    final waktuSebelumStr = _photoBeforeTime != null ? fmt(_photoBeforeTime!) : '';
    final waktuSesudahStr = _photoAfterTime != null ? fmt(_photoAfterTime!) : '';

    String? urlSeb;
    String? urlSes;
    if (isSelesai) {
      setState(() => _savingStep = 'Mengompresi & mengunggah foto...');
      try {
        // Kompres kedua foto secara paralel terlebih dahulu
        final results = await Future.wait([
          _encodePhoto(isBefore: true),
          _encodePhoto(isBefore: false),
        ]);
        // Upload keduanya secara paralel (hemat ~50% waktu upload)
        final urls = await Future.wait([
          PerbaikanApiService.instance.uploadPhoto(
            base64: results[0],
            filename: '${PhotoMetadataResolver.toFilenameStamp(_photoBeforeTime!)}_before.jpg',
          ),
          PerbaikanApiService.instance.uploadPhoto(
            base64: results[1],
            filename: '${PhotoMetadataResolver.toFilenameStamp(_photoAfterTime!)}_after.jpg',
          ),
        ]);
        urlSeb = urls[0];
        urlSes = urls[1];
      } catch (e) {
        if (!mounted) return;
        setState(() => _saving = false);
        final msg = e is ApiException || e is ServerException ? e.toString() : 'Gagal upload foto: $e';
        _showSnack(msg, isError: true);
        return;
      }
    }


    // ── Susun kolom yang ditimpa ─────────────────────────────────────────
    final isMGRT = _isMgrtRequired() || _isMgrtBaruRequired();
    final noMGRT = _isMgrtRequired() ? _noMgrtCtrl.text.trim() : _noMgrtBaruCtrl.text.trim();
    final angkaMGRT = _isMgrtRequired() ? _angkaMgrtCtrl.text.trim() : _angkaMgrtBaruCtrl.text.trim();

    final updates = <String, dynamic>{
      'STATUS_RP': sanitizeForSheet(_status),
      'Petugas_RP': sanitizeForSheet(_officerName ?? ''),
      'PENGADUAN_RP': sanitizeForSheet(_selectedJenis),
      'Tindakan_RP': sanitizeForSheet(_selectedTindakan),
      if (isSelesai) 'Foto_Seb_RP': urlSeb ?? '',
      if (isSelesai) 'Foto_Ses_RP': urlSes ?? '',
      if (isSelesai) 'Waktu_Seb_RP': waktuSebelumStr,
      if (isSelesai) 'Waktu_Ses_RP': waktuSesudahStr,
      if (isSelesai && locationStr.isNotEmpty) 'Latitude_RP': parts.isNotEmpty ? parts[0] : '',
      if (isSelesai && locationStr.isNotEmpty) 'Longitude_RP': parts.length > 1 ? parts[1] : '',
      if (isMGRT) 'No_MGRT_Baru_RP': noMGRT,
      if (isMGRT) 'Angka_MGRT_Baru_RP': angkaMGRT,

      'Status': sanitizeForSheet(_status),
      'Petugas': sanitizeForSheet(_officerName ?? ''),
      'Pengaduan': sanitizeForSheet(_selectedJenis),
      'Keterangan': sanitizeForSheet(_selectedTindakan),
      if (isSelesai) 'Foto Sebelum': urlSeb ?? '',
      if (isSelesai) 'Foto Sesudah': urlSes ?? '',
      if (isSelesai) 'Waktu Sebelum': waktuSebelumStr,
      if (isSelesai) 'Waktu Sesudah': waktuSesudahStr,
      'Latitude': lat,
      'Longitude': lng,
      if (_isMgrtRequired()) 'No MGRT': sanitizeForSheet(_noMgrtCtrl.text.trim()),
      if (_isMgrtRequired()) 'Angka MGRT': sanitizeForSheet(_angkaMgrtCtrl.text.trim()),
      if (_isMgrtBaruRequired()) 'No MGRT Baru': sanitizeForSheet(_noMgrtBaruCtrl.text.trim()),
      if (_isMgrtBaruRequired()) 'Angka MGRT Baru': sanitizeForSheet(_angkaMgrtBaruCtrl.text.trim()),
    };

    setState(() => _savingStep = 'Menyimpan ke server...');
    try {
      await PerbaikanApiService.instance.updateRowCells(
        sheetName: AppConfig.pengaduanSheetName,
        keyValue: widget.ticket.ticket,
        keyColumn: 'Ticket_RP',
        updates: updates,
      );

      // Notifikasi bersifat best-effort (tidak menggagalkan submit kalau
      // gagal terkirim) — meniru perilaku lama yang selalu mengirim
      // notifikasi setiap update tersimpan, apapun statusnya.
      unawaited(PerbaikanApiService.instance.notifySelesai(
        ticket: widget.ticket.ticket,
        idpel: widget.ticket.idPelanggan,
        nama: widget.ticket.nama,
        pengaduan: widget.ticket.kendala,
        petugas: _officerName ?? '',
        tindakan: _selectedTindakan ?? '',
        fotoSeb: urlSeb,
        fotoSes: urlSes,
        lat: lat.isNotEmpty ? lat : null,
        lng: lng.isNotEmpty ? lng : null,
      ));

      if (!mounted) return;
      setState(() => _saving = false);
      _showSuccess(widget.ticket.ticket, _status);
    } catch (e) {
      if (!mounted) return;
      setState(() => _saving = false);
      final msg = e is ApiException || e is ServerException ? e.toString() : 'Gagal mengirim laporan.';
      _showSnack('$msg Data yang sudah diisi tetap tersimpan — coba kirim lagi.',
          isError: true);
    }
  }

  void _showSuccess(String ticket, String status) {
    showSuccessSheet(
      context: context,
      ticket: ticket,
      status: status,
      onFinish: () {
        Navigator.pop(context); // tutup success sheet
        Navigator.pop(context); // kembali ke daftar tiket
      },
      onContinue: () {
        Navigator.pop(context); // tutup success sheet
        Navigator.pop(context); // kembali ke daftar tiket
      },
    );
  }

  void _showSnack(String message, {bool isError = false}) {
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError ? AppColors.danger : AppColors.selesai,
      ),
    );
  }

  // ── UI ────────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    final isSelesai = _status == AppConfig.statusSelesai;

    return Scaffold(
      backgroundColor: AppColors.canvas,
      appBar: AppBar(
        titleSpacing: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: _saving ? null : () => Navigator.pop(context),
        ),
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Tindakan & Dokumentasi',
                style: TextStyle(fontSize: 15, fontWeight: FontWeight.w700)),
            Text(
              widget.ticket.ticket,
              style: const TextStyle(fontSize: 11, color: Colors.white70),
            ),
          ],
        ),
      ),
      body: Stack(
        children: [
          Form(
            key: _formKey,
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 40),
              children: [
                if (_gpsBannerState != null) ...[
                  GpsBanner(state: _gpsBannerState!, message: _gpsMessage),
                  const SizedBox(height: 12),
                ],

                // ── Ringkasan tiket ────────────────────────────────────
                _buildTicketSummary(),
                const SizedBox(height: 16),

                // ── Tindakan Penanganan ────────────────────────────────
                SectionCard(
                  icon: Icons.construction_rounded,
                  title: 'Tindakan Penanganan',
                  child: Column(
                    children: [
                      DropdownButtonFormField<String>(
                        initialValue: _selectedJenis,
                        isExpanded: true,
                        decoration: const InputDecoration(
                            labelText: 'Jenis Pengaduan (Validasi)'),
                        items: (_options?.jenis?.isNotEmpty == true
                                ? _options!.jenis
                                : ['Data Kosong'])
                            .map((e) => DropdownMenuItem(value: e, child: Text(e)))
                            .toList(),
                        onChanged: (v) => setState(() => _selectedJenis = v == 'Data Kosong' ? null : v),
                        validator: (v) =>
                            (v == null || v == 'Data Kosong') ? 'Jenis pengaduan wajib dipilih' : null,
                      ),
                      const SizedBox(height: 16),
                      DropdownButtonFormField<String>(
                        initialValue: _selectedTindakan,
                        isExpanded: true,
                        decoration: const InputDecoration(labelText: 'Aksi / Tindakan'),
                        items: (_options?.penanganan?.isNotEmpty == true
                                ? _options!.penanganan
                                : ['Data Kosong'])
                            .map((e) => DropdownMenuItem(value: e, child: Text(e)))
                            .toList(),
                        onChanged: _onTindakanChanged,
                        validator: (v) =>
                            (v == null || v == 'Data Kosong') ? 'Tindakan penanganan wajib dipilih' : null,
                      ),
                      if (_isMgrtRequired()) ...[
                        const SizedBox(height: 16),
                        const Divider(),
                        const SizedBox(height: 16),
                        TextFormField(
                          controller: _noMgrtCtrl,
                          decoration: const InputDecoration(
                              labelText: 'No MGRT Lama',
                              hintText: 'Contoh: 12345678'),
                          validator: (v) => (v == null || v.trim().isEmpty)
                              ? 'No MGRT Lama wajib diisi'
                              : null,
                        ),
                        const SizedBox(height: 16),
                        TextFormField(
                          controller: _angkaMgrtCtrl,
                          keyboardType: TextInputType.number,
                          inputFormatters: [
                            FilteringTextInputFormatter.digitsOnly
                          ],
                          decoration: const InputDecoration(
                              labelText: 'Angka MGRT Lama',
                              hintText: 'Masukkan angka meteran'),
                          validator: (v) => (v == null || v.trim().isEmpty)
                              ? 'Angka MGRT Lama wajib diisi'
                              : null,
                        ),
                      ],
                      if (_isMgrtBaruRequired()) ...[
                        const SizedBox(height: 16),
                        const Divider(),
                        const SizedBox(height: 16),
                        TextFormField(
                          controller: _noMgrtBaruCtrl,
                          decoration: const InputDecoration(
                              labelText: 'No MGRT Baru',
                              hintText: 'Contoh: 87654321'),
                          validator: (v) => (v == null || v.trim().isEmpty)
                              ? 'No MGRT Baru wajib diisi'
                              : null,
                        ),
                        const SizedBox(height: 16),
                        TextFormField(
                          controller: _angkaMgrtBaruCtrl,
                          keyboardType: TextInputType.number,
                          inputFormatters: [
                            FilteringTextInputFormatter.digitsOnly
                          ],
                          decoration: const InputDecoration(
                              labelText: 'Angka MGRT Baru',
                              hintText: 'Masukkan angka meteran awal'),
                          validator: (v) => (v == null || v.trim().isEmpty)
                              ? 'Angka MGRT Baru wajib diisi'
                              : null,
                        ),
                      ],
                      const SizedBox(height: 16),
                      DropdownButtonFormField<String>(
                        key: ValueKey(_status),
                        initialValue: _status,
                        isExpanded: true,
                        decoration: const InputDecoration(labelText: 'Update Status'),
                        items: const [
                          DropdownMenuItem(
                            value: AppConfig.statusSelesai,
                            child: Text('🟢 SELESAI (Pekerjaan Tuntas)'),
                          ),
                          DropdownMenuItem(
                            value: AppConfig.statusProses,
                            child: Text('🟡 PROSES (Sedang Dikerjakan)'),
                          ),
                        ],
                        onChanged: (v) =>
                            setState(() => _status = v ?? AppConfig.statusSelesai),
                      ),
                    ],
                  ),
                ),

                // ── Dokumentasi ───────────────────────────────────────
                if (isSelesai) ...[
                  const SizedBox(height: 16),
                  SectionCard(
                    icon: Icons.camera_alt_rounded,
                    title: 'Dokumentasi',
                    subtitle: 'Foto sebelum & sesudah wajib untuk status SELESAI.',
                    child: Column(
                      children: [
                        PhotoCaptureTile(
                          label: 'Foto Sebelum Pengerjaan',
                          photo: _photoBefore,
                          capturedAt: _photoBeforeTime,
                          onCapture: () => _pickPhoto(true),
                        ),
                        const SizedBox(height: 16),
                        PhotoCaptureTile(
                          label: 'Foto Sesudah Pengerjaan',
                          photo: _photoAfter,
                          capturedAt: _photoAfterTime,
                          onCapture: () => _pickPhoto(false),
                        ),
                      ],
                    ),
                  ),
                ],

                // ── Kirim ─────────────────────────────────────────────
                const SizedBox(height: 16),
                SectionCard(
                  icon: Icons.send_rounded,
                  title: 'Kirim Laporan',
                  subtitle: 'Pastikan seluruh data sudah benar sebelum mengirim.',
                  child: SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                      ),
                      icon: const Icon(Icons.send_rounded, size: 20),
                      label: const Text('Simpan Penanganan'),
                      onPressed: _submitForm,
                    ),
                  ),
                ),
              ],
            ),
          ),
          if (_saving) _buildSavingOverlay(),
        ],
      ),
    );
  }

  Widget _buildTicketSummary() {
    final t = widget.ticket;
    final hasPhone = t.telepon.isNotEmpty;
    final hasLocation = t.lat != null && t.lng != null;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [AppColors.brand, Color(0xFF0A4E54)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: AppColors.brand.withValues(alpha: 0.18),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Baris atas: tiket + status badge
          Row(
            children: [
              const Icon(Icons.assignment_rounded,
                  color: Colors.white70, size: 16),
              const SizedBox(width: 6),
              Expanded(
                child: Text(
                  t.ticket,
                  style: const TextStyle(
                      color: Colors.white70,
                      fontSize: 12,
                      fontWeight: FontWeight.w600),
                ),
              ),
              StatusBadge(t.status),
            ],
          ),
          const SizedBox(height: 8),
          // Nama
          Text(
            t.nama.isNotEmpty ? t.nama : '—',
            style: const TextStyle(
                color: Colors.white,
                fontSize: 17,
                fontWeight: FontWeight.bold),
          ),
          if (t.idPelanggan.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 2),
              child: Text(
                'IDPEL: ${t.idPelanggan}',
                style: const TextStyle(color: Colors.white60, fontSize: 11.5),
              ),
            ),
          if (t.alamat.isNotEmpty) ...[
            const SizedBox(height: 8),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(Icons.location_on_outlined,
                    color: Colors.white54, size: 14),
                const SizedBox(width: 4),
                Expanded(
                  child: Text(
                    t.alamat,
                    style: const TextStyle(
                        color: Colors.white70, fontSize: 12, height: 1.3),
                  ),
                ),
              ],
            ),
          ],
          if (t.kendala.isNotEmpty) ...[
            const SizedBox(height: 6),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(Icons.report_problem_outlined,
                    color: Colors.orangeAccent, size: 14),
                const SizedBox(width: 4),
                Expanded(
                  child: Text(
                    t.kendala,
                    style: const TextStyle(
                        color: Colors.orange,
                        fontSize: 12,
                        height: 1.3,
                        fontStyle: FontStyle.italic),
                  ),
                ),
              ],
            ),
          ],
          // Aksi cepat WA & Map
          if (hasPhone || hasLocation) ...[
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                if (hasPhone)
                  _SummaryChip(
                    icon: Icons.chat_rounded,
                    label: t.telepon,
                    onTap: () async {
                      final ok = await ContactLauncher.openWhatsApp(
                        t.telepon,
                        message:
                            'Halo ${t.nama}, kami dari petugas Jargas ingin menindaklanjuti laporan Anda (No. Tiket: ${t.ticket}).',
                      );
                      if (!ok && mounted) {
                        ContactLauncher.showLaunchFailure(context, 'WhatsApp');
                      }
                    },
                  ),
                if (hasLocation)
                  _SummaryChip(
                    icon: Icons.map_rounded,
                    label: 'Lihat Lokasi',
                    onTap: () async {
                      final ok = await ContactLauncher.openMap(
                          t.lat!, t.lng!,
                          label: t.nama);
                      if (!ok && mounted) {
                        ContactLauncher.showLaunchFailure(context, 'aplikasi peta');
                      }
                    },
                  ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildSavingOverlay() {
    return Container(
      color: Colors.black.withValues(alpha: 0.45),
      alignment: Alignment.center,
      child: Container(
        width: 220,
        padding: const EdgeInsets.symmetric(vertical: 24, horizontal: 20),
        decoration: BoxDecoration(
            color: AppColors.surface,
            borderRadius: BorderRadius.circular(18)),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const CircularProgressIndicator(color: AppColors.brand),
            const SizedBox(height: 16),
            Text(
              _savingStep ?? 'Menyimpan...',
              textAlign: TextAlign.center,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Widget helper untuk kartu ringkasan tiket di form tindakan ────────────────

class _SummaryChip extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _SummaryChip({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.15),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.white.withValues(alpha: 0.3)),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: Colors.white, size: 13),
            const SizedBox(width: 5),
            ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 160),
              child: Text(
                label,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}