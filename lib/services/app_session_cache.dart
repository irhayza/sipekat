import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter/foundation.dart';
import '../models/ocr/customer.dart';
import '../models/perbaikan/dropdown_options.dart';
import '../models/perbaikan/ticket.dart';
import 'kunjungan_api_service.dart';
import 'ocr_api_service.dart';
import 'perbaikan_api_service.dart';

/// Sederhana: hasil pencarian pelanggan lintas-modul untuk autofill
/// (dipakai misalnya oleh modul Pengaduan saat mengetik ID Pelanggan).
class CachedCustomerMatch {
  final String idPelanggan;
  final String nama;
  final String alamat;
  final String telepon;
  final String source;

  const CachedCustomerMatch({
    required this.idPelanggan,
    required this.nama,
    required this.alamat,
    required this.telepon,
    required this.source,
  });
}

/// Cache sesi aplikasi (menyimpan ke local storage / SharedPreferences).
///
/// Tujuannya: semua daftar yang dibutuhkan tiap modul (opsi dropdown &
/// tiket Perbaikan, daftar Kunjungan/Pembukaan, daftar pelanggan
/// Pencatatan Meter) diambil SEKALI saja — sesaat setelah login — lalu
/// disimpan ke penyimpanan lokal. Layar membaca dari sini terlebih dulu (instan),
/// dan hanya melakukan panggilan jaringan baru saat pengguna menekan
/// "Segarkan" (pull-to-refresh) secara eksplisit.
class AppSessionCache {
  AppSessionCache._();
  static final AppSessionCache instance = AppSessionCache._();

  String officerNama = '';
  String officerEmail = '';
  String officerRole = 'petugas';

  // ── Modul Perbaikan ────────────────────────────────────────────────
  DropdownOptions? perbaikanOptions;
  List<Ticket> perbaikanTickets = const [];
  String? perbaikanError;
  bool perbaikanLoading = false;

  // ── Modul Kunjungan (penutupan) ───────────────────────────────────
  List<VisitCustomer> kunjunganList = const [];
  String? kunjunganError;
  bool kunjunganLoading = false;

  // ── Modul Pembukaan (reopen) ──────────────────────────────────────
  List<VisitCustomer> pembukaanList = const [];
  String? pembukaanError;
  bool pembukaanLoading = false;

  // ── Modul Pencatatan Meter (OCR) ──────────────────────────────────
  List<Customer> ocrCustomers = const [];
  String? ocrError;
  bool ocrLoading = false;

  // ── Cache Global Dapell (untuk auto-fill seluruh form) ───────────────
  /// Seluruh data pelanggan dari sheet "dapell" – dipakai untuk auto-fill
  /// nama & alamat di form Pengaduan tanpa perlu query ulang ke server.
  List<Customer> dapellCustomers = const [];
  String? dapellError;
  bool dapellLoading = false;

  bool _everPreloaded = false;
  bool _preloading = false;
  DateTime? lastLoadedAt;

  bool get isPreloaded => _everPreloaded;
  bool get isPreloading => _preloading;

  /// Memuat data yang tersimpan di memori lokal HP (SharedPreferences) saat aplikasi dibuka
  Future<void> initFromLocal(String nama, String email, [String role = 'petugas']) async {
    officerNama = nama;
    officerEmail = email;
    officerRole = role.isEmpty ? 'petugas' : role;

    try {
      final prefs = await SharedPreferences.getInstance();
      
      final optStr = prefs.getString('cache_perbaikan_options');
      if (optStr != null && optStr.isNotEmpty) {
        perbaikanOptions = DropdownOptions.fromJson(jsonDecode(optStr));
      }
      
      final tickStr = prefs.getString('cache_perbaikan_tickets');
      if (tickStr != null && tickStr.isNotEmpty) {
        final decoded = jsonDecode(tickStr) as List;
        perbaikanTickets = decoded.map((item) => Ticket.fromJson(Map<String, dynamic>.from(item))).toList();
      }

      final kunStr = prefs.getString('cache_kunjungan_list');
      if (kunStr != null && kunStr.isNotEmpty) {
        final decoded = jsonDecode(kunStr) as List;
        kunjunganList = decoded.map((item) => VisitCustomer.fromJson(Map<String, dynamic>.from(item))).toList();
      }

      final pemStr = prefs.getString('cache_pembukaan_list');
      if (pemStr != null && pemStr.isNotEmpty) {
        final decoded = jsonDecode(pemStr) as List;
        pembukaanList = decoded.map((item) => VisitCustomer.fromJson(Map<String, dynamic>.from(item))).toList();
      }

      final ocrStr = prefs.getString('cache_ocr_customers');
      if (ocrStr != null && ocrStr.isNotEmpty) {
        final decoded = jsonDecode(ocrStr) as List;
        ocrCustomers = decoded.map((item) => Customer.fromJson(Map<String, dynamic>.from(item))).toList();
      }

      final dapStr = prefs.getString('cache_dapell_customers');
      if (dapStr != null && dapStr.isNotEmpty) {
        final decoded = jsonDecode(dapStr) as List;
        dapellCustomers = decoded.map((item) => Customer.fromJson(Map<String, dynamic>.from(item))).toList();
      }

      final lastStr = prefs.getString('cache_last_loaded_at');
      if (lastStr != null && lastStr.isNotEmpty) {
        lastLoadedAt = DateTime.tryParse(lastStr);
      }

      _everPreloaded = true;
    } catch (e) {
      if (kDebugMode) debugPrint('Gagal memuat cache sesi dari penyimpanan lokal: $e');
    }
  }

  /// Menyimpan seluruh data cache aktif ke memori lokal HP (SharedPreferences)
  Future<void> saveToLocal() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      if (perbaikanOptions != null) {
        await prefs.setString('cache_perbaikan_options', jsonEncode(perbaikanOptions!.toJson()));
      }
      await prefs.setString('cache_perbaikan_tickets', jsonEncode(perbaikanTickets.map((t) => t.toJson()).toList()));
      await prefs.setString('cache_kunjungan_list', jsonEncode(kunjunganList.map((k) => k.toJson()).toList()));
      await prefs.setString('cache_pembukaan_list', jsonEncode(pembukaanList.map((p) => p.toJson()).toList()));
      await prefs.setString('cache_ocr_customers', jsonEncode(ocrCustomers.map((c) => c.toJson()).toList()));
      await prefs.setString('cache_dapell_customers', jsonEncode(dapellCustomers.map((c) => c.toJson()).toList()));
      await prefs.setString('cache_last_loaded_at', lastLoadedAt?.toIso8601String() ?? DateTime.now().toIso8601String());
    } catch (e) {
      if (kDebugMode) debugPrint('Gagal menyimpan cache sesi ke penyimpanan lokal: $e');
    }
  }

  /// Dipanggil sekali tepat setelah login berhasil untuk mengisi seluruh
  /// cache secara paralel dan menyimpannya ke memori lokal HP.
  Future<void> preloadAll({
    required String nama,
    required String email,
    String role = 'petugas',
    void Function(String stage)? onProgress,
  }) async {
    if (_preloading) return;
    _preloading = true;
    officerNama = nama;
    officerEmail = email;
    officerRole = role.isEmpty ? 'petugas' : role;

    onProgress?.call('Memuat data Perbaikan...');
    final perbaikanFuture = _loadPerbaikan();

    onProgress?.call('Memuat data Kunjungan...');
    final kunjunganFuture = _loadKunjungan();

    onProgress?.call('Memuat data Pembukaan...');
    final pembukaanFuture = _loadPembukaan();

    onProgress?.call('Memuat data Pencatatan Meter...');
    final ocrFuture = _loadOcrCustomers();

    onProgress?.call('Memuat data pelanggan lokal...');
    final dapellFuture = _loadDapellCustomers();

    await Future.wait([perbaikanFuture, kunjunganFuture, pembukaanFuture, ocrFuture, dapellFuture]);

    _everPreloaded = true;
    _preloading = false;
    lastLoadedAt = DateTime.now();
    await saveToLocal();
  }

  Future<void> _loadPerbaikan() async {
    perbaikanLoading = true;
    try {
      final options = await PerbaikanApiService.instance.getDropdownOptions();
      // [FIX] getActiveTickets() lama tidak mengirim alamat/telepon/kendala,
      // jadi kalau pencarian ID pelanggan lintas-modul (mis. di layar
      // Pengaduan) menemukan datanya lewat cache tiket Perbaikan ini,
      // alamat & teleponnya kosong. getActiveTicketsFull() membawa semua
      // kolom.
      final tickets = await PerbaikanApiService.instance.getActiveTicketsFull();
      perbaikanOptions = options;
      perbaikanTickets = tickets;
      perbaikanError = null;
    } catch (e) {
      perbaikanError = e.toString();
    } finally {
      perbaikanLoading = false;
    }
  }

  Future<void> _loadKunjungan() async {
    kunjunganLoading = true;
    try {
      final list = await KunjunganApiService.instance.getList(officerNama, officerRole, 'kunjungan');
      kunjunganList = list;
      kunjunganError = null;
    } catch (e) {
      kunjunganError = e.toString();
    } finally {
      kunjunganLoading = false;
    }
  }

  Future<void> _loadPembukaan() async {
    pembukaanLoading = true;
    try {
      final list = await KunjunganApiService.instance.getList(officerNama, officerRole, 'pembukaan');
      pembukaanList = list;
      pembukaanError = null;
    } catch (e) {
      pembukaanError = e.toString();
    } finally {
      pembukaanLoading = false;
    }
  }

  Future<void> _loadOcrCustomers() async {
    ocrLoading = true;
    try {
      final list = await OcrApiService.instance.getAssignedCustomers(officerNama);
      ocrCustomers = list;
      ocrError = null;
    } catch (e) {
      ocrError = e.toString();
    } finally {
      ocrLoading = false;
    }
  }

  Future<void> _loadDapellCustomers() async {
    dapellLoading = true;
    try {
      final list = await OcrApiService.instance.getAllDapellCustomers();
      dapellCustomers = list;
      dapellError = null;
    } catch (e) {
      dapellError = e.toString();
    } finally {
      dapellLoading = false;
    }
  }

  // ── Refresh per-modul (dipakai oleh tombol/pull-to-refresh) ───────
  Future<void> refreshPerbaikan() async {
    await _loadPerbaikan();
    await saveToLocal();
  }
  Future<void> refreshKunjungan() async {
    await _loadKunjungan();
    await saveToLocal();
  }
  Future<void> refreshPembukaan() async {
    await _loadPembukaan();
    await saveToLocal();
  }
  Future<void> refreshOcrCustomers() async {
    await _loadOcrCustomers();
    await saveToLocal();
  }
  Future<void> refreshDapellCustomers() async {
    await _loadDapellCustomers();
    await saveToLocal();
  }

  /// Mencari data pelanggan lintas seluruh cache berdasarkan ID Pelanggan.
  /// Dipakai modul Pengaduan agar autofill nama/alamat/telepon tidak perlu
  /// query baru ke Google Sheet setiap kali petugas mengetik ID.
  CachedCustomerMatch? findCustomerById(String rawId) {
    final id = rawId.trim().toLowerCase();
    if (id.isEmpty) return null;

    // Prioritas 1: Data global dapell (paling lengkap & untuk semua akun)
    for (final c in dapellCustomers) {
      if (c.noPelanggan.toLowerCase() == id || c.id.toLowerCase() == id) {
        return CachedCustomerMatch(
          idPelanggan: c.noPelanggan,
          nama: c.nama,
          alamat: c.alamat ?? '',
          telepon: '',
          source: 'Dapell',
        );
      }
    }
    // Prioritas 2: Tiket Perbaikan
    for (final t in perbaikanTickets) {
      if (t.idPelanggan.toLowerCase() == id) {
        return CachedCustomerMatch(
          idPelanggan: t.idPelanggan,
          nama: t.nama,
          alamat: t.alamat,
          telepon: t.telepon,
          source: 'Perbaikan',
        );
      }
    }
    // Prioritas 3: Daftar Kunjungan
    for (final c in kunjunganList) {
      if (c.idpel.toLowerCase() == id) {
        return CachedCustomerMatch(
          idPelanggan: c.idpel,
          nama: c.nama,
          alamat: c.alamat,
          telepon: c.telepon,
          source: 'Kunjungan',
        );
      }
    }
    // Prioritas 4: Daftar Pembukaan
    for (final c in pembukaanList) {
      if (c.idpel.toLowerCase() == id) {
        return CachedCustomerMatch(
          idPelanggan: c.idpel,
          nama: c.nama,
          alamat: c.alamat,
          telepon: c.telepon,
          source: 'Pembukaan',
        );
      }
    }
    // Prioritas 5: Daftar Pencatatan Meter (OCR)
    for (final c in ocrCustomers) {
      if (c.noPelanggan.toLowerCase() == id || c.id.toLowerCase() == id) {
        return CachedCustomerMatch(
          idPelanggan: c.noPelanggan,
          nama: c.nama,
          alamat: c.alamat ?? '',
          telepon: '',
          source: 'Pencatatan Meter',
        );
      }
    }
    return null;
  }

  /// Dipanggil saat logout supaya sesi berikutnya (mungkin petugas lain di
  /// perangkat yang sama) tidak melihat data cache milik petugas sebelumnya.
  void clear() {
    officerNama = '';
    officerEmail = '';
    officerRole = 'petugas';
    perbaikanOptions = null;
    perbaikanTickets = const [];
    perbaikanError = null;
    kunjunganList = const [];
    kunjunganError = null;
    pembukaanList = const [];
    pembukaanError = null;
    ocrCustomers = const [];
    ocrError = null;
    dapellCustomers = const [];
    dapellError = null;
    dapellLoading = false;
    _everPreloaded = false;
    _preloading = false;
    lastLoadedAt = null;

    SharedPreferences.getInstance().then((prefs) {
      prefs.remove('cache_perbaikan_options');
      prefs.remove('cache_perbaikan_tickets');
      prefs.remove('cache_kunjungan_list');
      prefs.remove('cache_pembukaan_list');
      prefs.remove('cache_ocr_customers');
      prefs.remove('cache_dapell_customers');
      prefs.remove('cache_last_loaded_at');
    }).catchError((e) {
      if (kDebugMode) debugPrint('Gagal membersihkan cache lokal: $e');
    });
  }
}

