import 'dart:async';
import 'dart:io';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import '../models/perbaikan/ticket.dart';
import 'app_session_cache.dart';


/// Service untuk mengambil data tiket aktif langsung dari Google Sheets
/// pada sheet "Pengaduan" di spreadsheet yang dikonfigurasi di [AppConfig].
///
/// Menggunakan Google Sheets CSV export untuk mem-bypass filter aktif pada UI sheet.
class PerbaikanTicketService {
  PerbaikanTicketService._();
  static final PerbaikanTicketService instance = PerbaikanTicketService._();

  // Menggunakan GAS endpoint filter-safe sebagai pengganti gviz/tq.
  // getValues() di GAS membaca semua baris termasuk yang difilter/disembunyikan.
  Uri get _sheetUri => Uri.parse(
        '${AppConfig.gasReadSheetUrl}?action=read_sheet&sheet=${Uri.encodeComponent(AppConfig.pengaduanSheetName)}',
      );

  /// Mengambil daftar tiket aktif (status bukan SELESAI) dari sheet Pengaduan.
  Future<List<Ticket>> getActiveTickets() async {
    final emailLower = AppSessionCache.instance.officerEmail.trim().toLowerCase();
    final nameLower = AppSessionCache.instance.officerNama.trim().toLowerCase();
    final isCg = emailLower.contains('cg') || nameLower.contains('cg');
    if (isCg) {
      return const <Ticket>[];
    }

    late final http.Response res;
    final client = http.Client();
    try {
      final request = http.Request('GET', _sheetUri)
        ..followRedirects = true
        ..headers['Accept'] = 'text/csv';

      final streamed = await client
          .send(request)
          .timeout(const Duration(seconds: AppConfig.connectionTimeoutSec));
      res = await http.Response.fromStream(streamed);
    } on TimeoutException {
      throw Exception('Server tidak merespon. Periksa koneksi internet Anda.');
    } on SocketException {
      throw Exception('Tidak ada koneksi internet. Sambungkan ke internet lalu coba lagi.');
    } catch (e) {
      throw Exception('Gagal terhubung ke server: $e');
    } finally {
      client.close();
    }

    if (res.statusCode != 200) {
      throw Exception('Server merespon status ${res.statusCode}.');
    }

    final csvText = res.body;
    if (csvText.trim().isEmpty) return [];

    // Parse CSV
    final lines = csvText.replaceAll('\r\n', '\n').replaceAll('\r', '\n').split('\n');
    final List<List<String>> allRows = [];
    for (final line in lines) {
      if (line.trim().isEmpty) continue;
      allRows.add(_parseCsvRow(line));
    }

    if (allRows.isEmpty) return [];

    final cols = allRows.first;
    final rows = allRows.sublist(1);

    // ── Bangun peta nama_kolom → index ──────────────────────────────────────
    final colIndex = <String, int>{};
    for (int i = 0; i < cols.length; i++) {
      final label = cols[i].trim().toLowerCase();
      if (label.isNotEmpty) colIndex[label] = i;
    }

    // ── Helper: ambil nilai sel sebagai String ──────────────────────────────
    String pickStr(List<String> row, List<String> keys) {
      for (final key in keys) {
        final idx = colIndex[key.toLowerCase()];
        if (idx == null || idx >= row.length) continue;
        final v = row[idx].trim();
        if (v.isNotEmpty && v != 'null') return v;
      }
      return '';
    }

    // ── Helper: ambil nilai sel sebagai double dengan perbaikan format ──────
    double? pickDouble(List<String> row, List<String> keys, {required bool isLat}) {
      for (final key in keys) {
        final idx = colIndex[key.toLowerCase()];
        if (idx == null || idx >= row.length) continue;
        final raw = row[idx].trim();
        if (raw.isEmpty || raw == 'null') continue;

        // Bersihkan spasi, ganti koma dengan titik desimal
        String cleaned = raw.replaceAll(RegExp(r'\s+'), '').replaceAll(',', '.');
        bool isNegative = cleaned.startsWith('-');
        String digits = cleaned.replaceAll(RegExp(r'[^0-9]'), '');
        if (digits.isEmpty) continue;

        if (isLat) {
          // Latitude Indonesia di sekitar Sidoarjo/Jawa Timur (harus -7.xxxxx)
          if (digits.startsWith('7')) {
            final rest = digits.substring(1);
            final val = double.tryParse('-7.$rest');
            if (val != null) return val;
          } else {
            // Fallback umum
            final first = digits.substring(0, 1);
            final rest = digits.substring(1);
            final val = double.tryParse('${isNegative ? "-" : ""}$first.$rest');
            if (val != null) return val;
          }
        } else {
          // Longitude Indonesia (harus 112.xxxxx)
          if (digits.startsWith('112')) {
            final rest = digits.substring(3);
            final val = double.tryParse('112.$rest');
            if (val != null) return val;
          } else if (digits.startsWith('11') && digits.length >= 3) {
            final first3 = digits.substring(0, 3);
            final rest = digits.substring(3);
            final val = double.tryParse('$first3.$rest');
            if (val != null) return val;
          } else {
            // Fallback: ambil 3 angka pertama jika ada
            if (digits.length >= 3) {
              final first3 = digits.substring(0, 3);
              final rest = digits.substring(3);
              final val = double.tryParse('$first3.$rest');
              if (val != null) return val;
            } else {
              final val = double.tryParse(cleaned);
              if (val != null) return val;
            }
          }
        }
      }
      return null;
    }

    // ── Parse setiap baris ──────────────────────────────────────────────────
    final List<Ticket> tickets = [];
    final isWr = emailLower.contains('wr') || nameLower.contains('wr');
    final isKd = emailLower.contains('kd') || nameLower.contains('kd');

    for (final row in rows) {
      if (row.isEmpty) continue;

      // Ambil nilai STATUS_RP dan PENGADUAN_RP menggunakan header yang fleksibel
      final statusVal = pickStr(row, ['status_rp', 'status']);
      final pengaduanVal = pickStr(row, ['pengaduan_rp', 'pengaduan', 'kendala']).toLowerCase();

      // Filter: STATUS_RP harus 'OPEN'
      if (statusVal.toLowerCase() != 'open') continue;

      // Filter: PENGADUAN_RP tidak boleh berisi 'buka segel' atau 'pasang kembali'
      if (pengaduanVal.contains('buka segel') || pengaduanVal.contains('pasang kembali')) {
        continue;
      }

      final ticketNo = pickStr(row, [
        'ticket_rp', 'ticket', 'no tiket', 'tiket', 'no. tiket', 'no_tiket',
        'nomor tiket', 'kode tiket',
      ]);
      if (ticketNo.isEmpty) continue;

      final ticketNoLower = ticketNo.toLowerCase();
      if (isWr && !ticketNoLower.contains('wr')) continue;
      if (isKd && !ticketNoLower.contains('kd')) continue;

      final telepon = _normalizePhone(pickStr(row, [
        'telfon_rp', 'telepon', 'no telepon', 'telp', 'no telp',
        'hp', 'no hp', 'handphone', 'wa', 'whatsapp', 'no wa',
        'phone', 'mobile', 'telfon', 'nohp',
      ]));

      final kendala = pickStr(row, [
        'pengaduan_rp', 'kendala', 'keluhan', 'pengaduan', 'masalah',
        'deskripsi', 'keterangan', 'jenis pengaduan',
      ]);

      tickets.add(Ticket(
        ticket: ticketNo,
        nama: pickStr(row, ['nama_rp', 'nama', 'nama pelanggan', 'name']),
        status: statusVal.isEmpty ? AppConfig.statusOpen : statusVal.toUpperCase(),
        area: pickStr(row, ['area', 'wilayah', 'zona', 'rayon']),
        idPelanggan: pickStr(row, [
          'idpel_rp', 'idpel', 'id pelanggan', 'id_pelanggan',
          'no pelanggan', 'id pel',
        ]),
        alamat: pickStr(row, ['alamat_rp', 'alamat', 'address']),
        telepon: telepon,
        kendala: kendala,
        lat: pickDouble(row, ['latitude_rp', 'lat', 'latitude', 'lintang'], isLat: true),
        lng: pickDouble(row, ['longitude_rp', 'lng', 'lon', 'longitude', 'bujur'], isLat: false),
      ));
    }

    return tickets;
  }

  /// Parser CSV yang aman mendukung field ber-tanda kutip (quoted commas)
  List<String> _parseCsvRow(String line) {
    final List<String> result = [];
    final StringBuffer current = StringBuffer();
    bool inQuotes = false;
    for (int i = 0; i < line.length; i++) {
      final char = line[i];
      if (char == '"') {
        inQuotes = !inQuotes;
      } else if (char == ',' && !inQuotes) {
        result.add(current.toString());
        current.clear();
      } else {
        current.write(char);
      }
    }
    result.add(current.toString());
    return result;
  }

  /// Normalisasi nomor telepon agar selalu diawali 0 jika angka murni
  String _normalizePhone(String raw) {
    if (raw.isEmpty) return raw;
    final digitsOnly = raw.replaceAll(RegExp(r'[^0-9]'), '');
    if (digitsOnly == raw && digitsOnly.isNotEmpty) {
      if (digitsOnly.startsWith('8') && digitsOnly.length >= 9) {
        return '0$digitsOnly';
      }
    }
    return raw;
  }
}
