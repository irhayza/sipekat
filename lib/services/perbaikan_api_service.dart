import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import '../models/perbaikan/dropdown_options.dart';
import '../models/perbaikan/officer.dart';
import '../models/perbaikan/ticket.dart';
import '../models/perbaikan/update_result.dart';
import 'app_session_cache.dart';


class ApiException implements Exception {
  final String message;
  ApiException(this.message);

  @override
  String toString() => message;
}

/// [BARU] Dulu semua kegagalan (tidak ada internet vs server menolak
/// permintaan) sama-sama dilempar sebagai ApiException, jadi Flutter tidak
/// bisa membedakan "coba lagi nanti" dari "datanya memang tidak valid".
/// ServerException dipakai khusus untuk kegagalan yang server balas
/// eksplisit (status/ok = false dengan pesan) — bukan soal koneksi.
class ServerException implements Exception {
  final String message;
  ServerException(this.message);

  @override
  String toString() => message;
}

class PerbaikanApiService {
  PerbaikanApiService._();
  static final PerbaikanApiService instance = PerbaikanApiService._();

  Uri get _bridgeUri => Uri.parse(AppConfig.repairBridgeUrl);

  // ── In-memory cache ─────────────────────────────────────────────────────
  DropdownOptions? _dropdownCache;
  DateTime? _dropdownCachedAt;
  static const _dropdownTtl = Duration(hours: 1); // dropdown jarang berubah

  List<Ticket>? _ticketCache;
  DateTime? _ticketCachedAt;
  static const _ticketTtl = Duration(minutes: 2); // tiket lebih dinamis

  /// Invalidasi semua cache (dipanggil setelah submit berhasil)
  void clearCache() {
    _dropdownCache = null;
    _dropdownCachedAt = null;
    _ticketCache = null;
    _ticketCachedAt = null;
  }

  Future<Map<String, dynamic>> _post(
    Map<String, dynamic> body, {
    required int timeoutSec,
  }) async {
    late final http.Response res;
    final client = http.Client();
    try {
      final request = http.Request('POST', _bridgeUri)
        ..headers['Content-Type'] = 'application/json'
        ..body = jsonEncode(body)
        ..followRedirects = false;

      final streamedRes =
          await client.send(request).timeout(Duration(seconds: timeoutSec));
      var tempRes = await http.Response.fromStream(streamedRes);

      if (tempRes.statusCode == 302 ||
          tempRes.statusCode == 307 ||
          tempRes.statusCode == 308 ||
          tempRes.statusCode == 301 ||
          tempRes.statusCode == 303) {
        final redirectUrl =
            tempRes.headers['location'] ?? tempRes.headers['Location'];
        if (redirectUrl != null) {
          final redirectRequest = http.Request('GET', Uri.parse(redirectUrl))
            ..followRedirects = true;
          final redirectStreamedRes = await client
              .send(redirectRequest)
              .timeout(Duration(seconds: timeoutSec));
          res = await http.Response.fromStream(redirectStreamedRes);
        } else {
          res = tempRes;
        }
      } else {
        res = tempRes;
      }
    } on TimeoutException {
      throw ApiException(
          'Server tidak merespon. Periksa koneksi internet Anda lalu coba lagi.');
    } on SocketException {
      throw ApiException(
          'Tidak ada koneksi internet. Sambungkan ke internet lalu coba lagi.');
    } on http.ClientException {
      throw ApiException(
          'Gagal terhubung ke server. Coba lagi beberapa saat.');
    } finally {
      client.close();
    }

    if (res.statusCode != 200) {
      throw ApiException(
          'Server merespon dengan status ${res.statusCode}. Coba lagi.');
    }

    dynamic decoded;
    try {
      decoded = jsonDecode(res.body);
    } catch (_) {
      throw ApiException(
          'Server mengalami gangguan saat memproses permintaan. Coba lagi.');
    }

    if (decoded is List) return {'_list': decoded};
    if (decoded is Map) return decoded.cast<String, dynamic>();
    throw ApiException('Format respons server tidak dikenali.');
  }

  Future<DropdownOptions> getDropdownOptions({bool forceRefresh = false}) async {
    final now = DateTime.now();
    if (!forceRefresh &&
        _dropdownCache != null &&
        _dropdownCachedAt != null &&
        now.difference(_dropdownCachedAt!) < _dropdownTtl) {
      return _dropdownCache!;
    }

    final data = await _post(
      {'action': 'get_dropdown_options'},
      timeoutSec: AppConfig.connectionTimeoutSec,
    );

    final petugasRaw    = (data['petugas']     as List?) ?? const [];
    final jenisRaw      = (data['jenis']        as List?) ?? const [];
    final penangananRaw = (data['penanganan']   as List?) ?? const [];

    final officers = petugasRaw.map((p) {
      if (p is Map) return Officer.fromJson(p.cast<String, dynamic>());
      return Officer(nama: p.toString(), area: '');
    }).toList();

    _dropdownCache = DropdownOptions(
      petugas: officers,
      jenis: jenisRaw.map((e) => e.toString()).toList(),
      penanganan: penangananRaw.map((e) => e.toString()).toList(),
    );
    _dropdownCachedAt = now;
    return _dropdownCache!;
  }

  Future<List<Ticket>> getActiveTickets({bool forceRefresh = false}) async {
    final emailLower = AppSessionCache.instance.officerEmail.trim().toLowerCase();
    final nameLower  = AppSessionCache.instance.officerNama.trim().toLowerCase();
    final isCg = emailLower.contains('cg') || nameLower.contains('cg');

    if (isCg) return const <Ticket>[];

    // Sajikan dari cache jika masih segar
    final now = DateTime.now();
    if (!forceRefresh &&
        _ticketCache != null &&
        _ticketCachedAt != null &&
        now.difference(_ticketCachedAt!) < _ticketTtl) {
      return _ticketCache!;
    }

    final data = await _post(
      {'action': 'get_active_tickets'},
      timeoutSec: AppConfig.connectionTimeoutSec,
    );
    final list = (data['_list'] as List?) ?? const [];
    final allTickets = list
        .whereType<Map>()
        .map((e) => Ticket.fromJson(e.cast<String, dynamic>()))
        .toList();

    final isWr = emailLower.contains('wr') || nameLower.contains('wr');
    final isKd = emailLower.contains('kd') || nameLower.contains('kd');

    final normalizedNama = AppSessionCache.instance.officerNama.replaceAll(RegExp(r'\s+'), ' ').trim().toLowerCase();

    _ticketCache = allTickets.where((t) {
      // 1) Status harus open
      if (t.status.trim().toLowerCase() != 'open') return false;

      // 2) Pengaduan tidak boleh mengandung 'buka segel' atau 'pasang kembali'
      final kendalaLower = t.kendala.toLowerCase();
      if (kendalaLower.contains('buka segel') || kendalaLower.contains('pasang kembali')) {
        return false;
      }

      // 3) Filter wilayah berdasarkan tipe akun
      if (isWr) {
        return t.ticket.toLowerCase().contains('wr');
      } else if (isKd) {
        return t.ticket.toLowerCase().contains('kd');
      }

      // 4) Admin & user lain lolos
      return true;
    }).toList();
    _ticketCachedAt = now;
    return _ticketCache!;
  }

  Future<UpdateResult> updatePenyelesaian(Map<String, dynamic> payload) async {
    final data = await _post(
      {'action': 'update_penyelesaian', ...payload},
      timeoutSec: AppConfig.uploadTimeoutSec,
    );

    if (data['ok'] == true) {
      return UpdateResult(
        ticket: data['ticket']?.toString() ??
            payload['ticket']?.toString() ??
            '',
        status: data['status']?.toString() ??
            payload['status']?.toString() ??
            '',
      );
    }

    final message = data['message'] ?? data['error'];
    throw ApiException(
        message?.toString() ?? 'Server menolak permintaan tanpa keterangan.');
  }

  // ═══════════════════════════════════════════════════════════════════
  // [API BARU] Endpoint generik (find_row / update_row_cells / upload_photo
  // / get_sheet_rows), sama seperti yang dipakai modul Kunjungan. Dipakai
  // supaya validasi (tiket sudah SELESAI atau belum) dan pemetaan kolom
  // yang ditulis jadi keputusan Flutter, bukan tersembunyi di GAS.
  // ═══════════════════════════════════════════════════════════════════

  /// Mencari satu baris di [sheetName] berdasarkan [keyValue] pada kolom
  /// [keyColumn] (mis. "Ticket"). Melempar [ServerException] kalau server
  /// menjawab tidak ditemukan, atau [ApiException] kalau soal koneksi.
  Future<Map<String, dynamic>> findRow({
    required String sheetName,
    required String keyValue,
    String? keyColumn,
  }) async {
    final data = await _post({
      'action': 'find_row',
      'sheetName': sheetName,
      'idpel': keyValue,
      if (keyColumn != null) 'keyColumn': keyColumn,
    }, timeoutSec: AppConfig.connectionTimeoutSec);

    if (data['status'] != 'success') {
      throw ServerException(data['message']?.toString() ?? 'Data tidak ditemukan.');
    }
    return (data['row'] as Map?)?.cast<String, dynamic>() ?? {};
  }

  /// Menulis [updates] (key = nama kolom persis seperti header sheet
  /// "Pengaduan", yang sudah diketahui pasti) ke baris yang cocok dengan
  /// [keyValue] pada kolom [keyColumn].
  Future<void> updateRowCells({
    required String sheetName,
    required String keyValue,
    String? keyColumn,
    String? filterColumn,
    String? filterValue,
    required Map<String, dynamic> updates,
  }) async {
    final data = await _post({
      'action': 'update_row_cells',
      'sheetName': sheetName,
      'idpel': keyValue,
      if (keyColumn != null) 'keyColumn': keyColumn,
      if (filterColumn != null) 'filterColumn': filterColumn,
      if (filterValue != null) 'filterValue': filterValue,
      'updates': updates,
    }, timeoutSec: AppConfig.uploadTimeoutSec);

    if (data['status'] != 'success') {
      throw ServerException(data['message']?.toString() ?? 'Gagal menyimpan perubahan ke server.');
    }
  }

  /// Upload satu foto base64 ke Drive folder khusus Perbaikan dan
  /// kembalikan URL-nya. Dipisah dari [updateRowCells] supaya kegagalan
  /// upload (izin folder Drive, dsb) bisa dibedakan dari kegagalan tulis
  /// sheet.
  Future<String> uploadPhoto({
    required String base64,
    required String filename,
  }) async {
    final data = await _post({
      'action': 'upload_photo',
      'base64': base64,
      'filename': filename,
      'target': 'perbaikan',
    }, timeoutSec: AppConfig.uploadTimeoutSec);

    if (data['status'] != 'success' || data['url'] == null) {
      throw ServerException(data['message']?.toString() ?? 'Gagal upload foto ke server.');
    }
    return data['url'].toString();
  }

  /// Kirim notifikasi WA/Telegram "pengaduan selesai". Dipanggil setelah
  /// [updateRowCells] sukses — kegagalan di sini tidak boleh menggagalkan
  /// keseluruhan submit, karena datanya sudah tersimpan.
  Future<void> notifySelesai({
    required String ticket,
    required String idpel,
    required String nama,
    required String pengaduan,
    required String petugas,
    required String tindakan,
    String? fotoSeb,
    String? fotoSes,
    String? lat,
    String? lng,
  }) async {
    try {
      await _post({
        'action': 'notify_selesai',
        'ticket': ticket,
        'idpel': idpel,
        'nama': nama,
        'pengaduan': pengaduan,
        'petugas': petugas,
        'tindakan': tindakan,
        if (fotoSeb != null) 'fotoSeb': fotoSeb,
        if (fotoSes != null) 'fotoSes': fotoSes,
        if (lat != null) 'lat': lat,
        if (lng != null) 'lng': lng,
      }, timeoutSec: AppConfig.connectionTimeoutSec);
    } catch (_) {
      // Sengaja diabaikan — notifikasi bersifat best-effort.
    }
  }

  /// [FIX] Pengganti [getActiveTickets] lama. GAS `getActiveTickets()`
  /// hanya mengembalikan {ticket, nama, status, area} — TIDAK termasuk
  /// idPelanggan/alamat/telepon/kendala/lat/lng. Akibatnya: (1) tombol
  /// telepon/WhatsApp di layar tiket selalu kosong meski sudah dibuatkan
  /// UI-nya, dan (2) filter "kecualikan tiket buka segel/pasang kembali"
  /// di bawah tidak pernah aktif karena `kendala` selalu kosong. Endpoint
  /// generik ini mengambil SEMUA kolom, jadi kedua bug itu otomatis
  /// hilang begitu layar memakai method ini.
  Future<List<Ticket>> getActiveTicketsFull() async {
    final emailLower = AppSessionCache.instance.officerEmail.trim().toLowerCase();
    final nameLower = AppSessionCache.instance.officerNama.trim().toLowerCase();
    final isCg = emailLower.contains('cg') || nameLower.contains('cg');
    if (isCg) {
      return const <Ticket>[];
    }

    final data = await _post(
      {'action': 'readSheet', 'sheetName': AppConfig.pengaduanSheetName},
      timeoutSec: AppConfig.connectionTimeoutSec,
    );
    if (data['ok'] != true && data['status'] != 'success') {
      throw ServerException(data['message']?.toString() ?? 'Gagal memuat daftar tiket.');
    }

    final list = (data['data'] as List?) ?? (data['rows'] as List?) ?? const [];
    final allTickets = list.whereType<Map>().map((raw) {
      final r = raw.cast<String, dynamic>();
      return Ticket.fromJson({
        'ticket': r['Ticket_RP'] ?? r['Ticket'],
        'nama': r['Nama_RP'] ?? r['Nama'],
        'status': r['STATUS_RP'] ?? r['Status'],
        'area': ((r['Ticket_RP'] ?? r['Ticket'])?.toString().toUpperCase().contains('KD') ?? false)
            ? 'KD'
            : (((r['Ticket_RP'] ?? r['Ticket'])?.toString().toUpperCase().contains('WR') ?? false) ? 'WR' : ''),
        'idPelanggan': r['IDPEL_RP'] ?? r['ID Pelanggan'],
        'alamat': r['Alamat_RP'] ?? r['Alamat'],
        'telepon': r['TELFON_RP'] ?? r['Telepon'],
        'kendala': r['PENGADUAN_RP'] ?? r['Pengaduan'],
        'lat': r['Latitude_RP'] ?? r['Latitude'],
        'lng': r['Longitude_RP'] ?? r['Longitude'],
      });
    }).toList();

    final isWr = emailLower.contains('wr') || nameLower.contains('wr');
    final isKd = emailLower.contains('kd') || nameLower.contains('kd');

    return allTickets.where((t) {
      if (t.status.trim().toLowerCase() != 'open') return false;

      final kendalaLower = t.kendala.toLowerCase();
      if (kendalaLower.contains('buka segel') || kendalaLower.contains('pasang kembali')) {
        return false;
      }

      if (isWr) {
        return t.ticket.toLowerCase().contains('wr');
      } else if (isKd) {
        return t.ticket.toLowerCase().contains('kd');
      }
      return true;
    }).toList();
  }
}
