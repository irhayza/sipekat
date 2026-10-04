import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import '../config/app_config.dart';
import 'app_session_cache.dart';

class ApiException implements Exception {
  final String message;
  ApiException(this.message);

  @override
  String toString() => message;
}

class ServerException implements Exception {
  final String message;
  ServerException(this.message);

  @override
  String toString() => message;
}

/// Hasil dari [KunjunganApiService.findRow]: `row` (Map by nama header,
/// dipakai untuk sheet yang nama headernya diketahui pasti, mis.
/// "Pengaduan") dan `rowValues` (List posisi asli kolom 0-based, dipakai
/// untuk sheet yang nama headernya TIDAK diketahui pasti, mis. "CABUT" —
/// supaya baca/tulis tetap sesuai posisi kolom yang sudah terbukti benar
/// dari kode lama, tanpa perlu menebak nama header).
class KunjunganRowResult {
  final Map<String, dynamic> row;
  final List<dynamic> rowValues;
  const KunjunganRowResult({required this.row, required this.rowValues});

  /// Ambil nilai pada kolom ke-[index] (0-based) sebagai String, aman
  /// kalau index di luar jangkauan.
  String valueAt(int index) {
    if (index < 0 || index >= rowValues.length) return '';
    return rowValues[index]?.toString() ?? '';
  }
}

class VisitCustomer {
  final String idpel;
  final String nama;
  final String alamat;
  final String nomgrt;
  final int bln;
  final double rupiah;
  final String status;

  /// Nomor telepon/WA pelanggan (kolom TELEPON/TELP/HP pada sheet CABUT
  /// bila tersedia). Kosong jika sheet belum punya kolom ini.
  final String telepon;

  /// Ringkasan kendala/kondisi di lapangan (mis. dari kolom KONDISI/
  /// KETERANGAN), ditampilkan di kartu info pelanggan.
  final String kendala;

  final double? lat;
  final double? lng;

  VisitCustomer({
    required this.idpel,
    required this.nama,
    required this.alamat,
    required this.nomgrt,
    required this.bln,
    required this.rupiah,
    required this.status,
    this.telepon = '',
    this.kendala = '',
    this.lat,
    this.lng,
  });

  factory VisitCustomer.fromJson(Map<String, dynamic> json) {
    return VisitCustomer(
      idpel: json['IDPEL']?.toString() ?? '',
      nama: json['NAMA']?.toString() ?? '',
      alamat: json['ALAMAT']?.toString() ?? '',
      nomgrt: json['NOMGRT']?.toString() ?? '',
      bln: int.tryParse(json['BLN']?.toString() ?? '0') ?? 0,
      rupiah: double.tryParse(json['RUPIAH']?.toString() ?? '0') ?? 0.0,
      status: json['STATUS']?.toString() ?? 'Y',
      telepon: json['TELEPON']?.toString() ?? '',
      kendala: json['KONDISI']?.toString() ?? '',
      lat: json['LAT'] != null ? double.tryParse(json['LAT'].toString()) : null,
      lng: json['LNG'] != null ? double.tryParse(json['LNG'].toString()) : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'IDPEL': idpel,
      'NAMA': nama,
      'ALAMAT': alamat,
      'NOMGRT': nomgrt,
      'BLN': bln,
      'RUPIAH': rupiah,
      'STATUS': status,
      'TELEPON': telepon,
      'KONDISI': kendala,
      if (lat != null) 'LAT': lat,
      if (lng != null) 'LNG': lng,
    };
  }
}

class KunjunganApiService {
  KunjunganApiService._();
  static final KunjunganApiService instance = KunjunganApiService._();

  bool get _isPlaceholderUrl =>
      AppConfig.visitBridgeUrl.contains('AKfycbx_placeholder_deployment_id');

  Uri get _apiUri => Uri.parse(AppConfig.visitBridgeUrl);

  List<VisitCustomer> _mockGetList(String nama, String role, String menu) {
    if (menu == 'kunjungan') {
      final all = [
        VisitCustomer(
          idpel: '1100223301',
          nama: 'Budi Santoso',
          alamat: 'Jl. Raya Kediri No. 12',
          nomgrt: 'M-98765',
          bln: 4,
          rupiah: 150000,
          status: 'Y',
          telepon: '081234567801',
        ),
        VisitCustomer(
          idpel: '1100223302',
          nama: 'Siti Aminah',
          alamat: 'Jl. Melati No. 5 Kediri',
          nomgrt: 'M-98766',
          bln: 5,
          rupiah: 220000,
          status: 'Y',
          telepon: '081234567802',
        ),
        VisitCustomer(
          idpel: '1100223399',
          nama: 'Sudirman (Status T - Mock)',
          alamat: 'Jl. Pemuda No. 15 Kediri',
          nomgrt: 'M-98770',
          bln: 3,
          rupiah: 180000,
          status: 'T',
        ),
        if (role == 'admin' || nama.toLowerCase() == 'ahmad')
          VisitCustomer(
            idpel: '1100223303',
            nama: 'Joko Widodo',
            alamat: 'Jl. Diponegoro No. 88 Waru',
            nomgrt: 'M-88771',
            bln: 3,
            rupiah: 85000,
            status: 'Y',
          ),
        if (role == 'admin' || nama.toLowerCase() == 'reza')
          VisitCustomer(
            idpel: '1100223304',
            nama: 'Heri Setiawan',
            alamat: 'Perum Waru Permai B-4',
            nomgrt: 'M-88772',
            bln: 6,
            rupiah: 310000,
            status: 'Y',
          ),
      ];
      return all.where((c) => c.status.toUpperCase() == 'Y').toList();
    } else {
      return [
        VisitCustomer(
          idpel: '2200334401',
          nama: 'Diana Lestari',
          alamat: 'Jl. Mawar No. 10 Kediri',
          nomgrt: 'M-11223',
          bln: 2,
          rupiah: 120000,
          status: 'Y',
        ),
        VisitCustomer(
          idpel: '2200334402',
          nama: 'Rudi Hartono',
          alamat: 'Jl. Kenanga No. 23 Waru',
          nomgrt: 'M-44556',
          bln: 3,
          rupiah: 95000,
          status: 'Y',
        ),
      ];
    }
  }

  Future<Map<String, dynamic>> _post(Map<String, dynamic> payload,
      {required Duration timeout}) async {
    late final http.Response res;
    final client = http.Client();
    try {
      final request = http.Request('POST', _apiUri)
        ..headers['Content-Type'] = 'application/json'
        ..body = jsonEncode(payload)
        ..followRedirects = false;

      final streamedRes = await client.send(request).timeout(timeout);
      var tempRes = await http.Response.fromStream(streamedRes);

      if (tempRes.statusCode == 302 ||
          tempRes.statusCode == 307 ||
          tempRes.statusCode == 308 ||
          tempRes.statusCode == 301) {
        final redirectUrl = tempRes.headers['location'] ?? tempRes.headers['Location'];
        if (redirectUrl != null) {
          final redirectRequest = http.Request('GET', Uri.parse(redirectUrl))
            ..followRedirects = true;
          final redirectStreamedRes = await client.send(redirectRequest).timeout(timeout);
          res = await http.Response.fromStream(redirectStreamedRes);
        } else {
          res = tempRes;
        }
      } else {
        res = tempRes;
      }
    } on TimeoutException {
      throw ApiException(
        'Koneksi ke server timeout (${timeout.inSeconds} detik). '
        'Sinyal internet lambat atau server Google Apps Script sedang '
        'lambat merespons (cold start). Coba lagi.',
      );
    } on SocketException catch (e) {
      throw ApiException(
        'Tidak ada koneksi internet atau server tidak dapat dijangkau: ${e.message}',
      );
    } on http.ClientException catch (e) {
      throw ApiException('Gagal terhubung ke server: $e');
    } finally {
      client.close();
    }

    if (res.statusCode != 200) {
      throw ApiException(
        'Server merespons dengan status ${res.statusCode}. '
        'Periksa kembali deployment Google Apps Script Anda.',
      );
    }

    final body = res.body.trim();
    if (body.startsWith('<')) {
      throw ApiException(
        'Server mengembalikan halaman HTML, bukan JSON. Kemungkinan besar '
        'deployment Web App Google Apps Script belum diset akses ke '
        '"Anyone", atau URL di app_config.dart sudah kedaluwarsa. '
        'Cek menu Deploy > Manage deployments di Apps Script.',
      );
    }

    dynamic decoded;
    try {
      decoded = jsonDecode(body);
    } on FormatException {
      throw ApiException('Gagal membaca respons server (format JSON tidak valid).');
    }

    if (decoded is! Map) {
      throw ApiException('Format respons server tidak sesuai yang diharapkan.');
    }
    return decoded.cast<String, dynamic>();
  }

  String _normalizeComparable(String value) {
    return value.trim().toLowerCase().replaceAll(RegExp(r'\s+'), ' ');
  }

  Future<List<VisitCustomer>> getList(String nama, String role, String menu) async {
    if (_isPlaceholderUrl) {
      await Future.delayed(const Duration(milliseconds: 800));
      return _mockGetList(nama, role, menu);
    }

    final email = AppSessionCache.instance.officerEmail.trim().toLowerCase();
    final isAdmin = email.startsWith('admin') || email.startsWith('super');

    final sheetName = (menu == 'kunjungan') ? AppConfig.ocrSheetName : AppConfig.pengaduanSheetName;
    // Menggunakan GAS endpoint filter-safe sebagai pengganti gviz/tq.
    // getValues() di GAS membaca semua baris termasuk yang difilter/disembunyikan.
    final csvUri = Uri.parse(
      '${AppConfig.gasReadSheetUrl}?action=read_sheet&sheet=${Uri.encodeComponent(sheetName)}',
    );

    late final String csvText;
    try {
      final res = await http.get(csvUri).timeout(const Duration(seconds: AppConfig.connectionTimeoutSec));
      if (res.statusCode != 200) {
        throw ApiException('Server merespon dengan status ${res.statusCode}');
      }
      csvText = res.body;
    } catch (e) {
      throw ApiException('Gagal memuat data pelanggan ($sheetName): $e');
    }

    if (csvText.trim().isEmpty) return const <VisitCustomer>[];

    final lines = csvText.replaceAll('\r\n', '\n').replaceAll('\r', '\n').split('\n');
    final List<List<String>> allRows = [];
    for (final line in lines) {
      if (line.trim().isEmpty) continue;
      allRows.add(_parseCsvRow(line));
    }

    if (allRows.isEmpty) return const <VisitCustomer>[];

    final columns = allRows.first;
    final rows = allRows.sublist(1);

    final idpelIndex = _findCsvColumnIndex(columns, const ['IDPEL', 'ID PEL', 'NO PELANGGAN', 'IDPEL_RP']);
    final namaIndex = _findCsvColumnIndex(columns, const ['NAMA', 'NAMA PELANGGAN', 'Nama_RP']);
    final alamatIndex = _findCsvColumnIndex(columns, const [
      'ALAMAT', 'ALAMAT PELANGGAN', 'ALAMATPELANGGAN', 'ALMT', 'ALM',
      'ALAMAT LOKASI', 'ALAMAT PASANG', 'ALAMAT_PELANGGAN', 'LOKASI', 'ADDRESS',
      'ALAMAT LENGKAP', 'JALAN', 'Alamat_RP'
    ]);
    final nomgrtIndex = _findCsvColumnIndex(columns, const ['NOMETER', 'NO METER', 'NOMGRT', 'NOMGRT_LAMA', 'NoMGRT_RP']);
    final blnIndex = _findCsvColumnIndex(columns, const ['BLN', 'BULAN']);
    final rupiahIndex = _findCsvColumnIndex(columns, const ['RUPIAH', 'TAGIHAN']);
    final statusIndex = _findCsvColumnIndex(columns, const ['STATUS', 'STATUS_RP']);
    final petugasIndex = _findCsvColumnIndex(columns, const ['PTGS_KJG', 'PETUGAS', 'PETUGAS ORDER', 'NAMA PETUGAS', 'Petugas_RP', 'ptgs']);
    final kondisiIndex = _findCsvColumnIndex(columns, const ['KONDISI', 'KETERANGAN', 'PENGADUAN', 'PENGADUAN_RP', 'KONDISI_PR']);
    final teleponIndex = _findCsvColumnIndex(columns, const ['TELEPON', 'TELP', 'TELFON', 'NO HP', 'NOHP', 'HP', 'WHATSAPP', 'WA', 'TELFON_RP']);
    final latIndex = _findCsvColumnIndex(columns, const ['LAT', 'LATITUDE', 'Latitude_RP']);
    final lngIndex = _findCsvColumnIndex(columns, const ['LNG', 'LON', 'LONGITUDE', 'Longitude_RP']);
    final ticketIndex = _findCsvColumnIndex(columns, const ['TICKET', 'NO TICKET', 'TIKET', 'NO TIKET', 'Ticket_RP']);
    final mrsIndex = _findCsvColumnIndex(columns, const ['MRS']);
    final orderIndex = _findCsvColumnIndex(columns, const ['ORDER']);

    if (idpelIndex == null) {
      throw ApiException('Kolom IDPEL pada sheet $sheetName tidak ditemukan');
    }

    final normalizedPetugas = _normalizeComparable(nama);
    final List<VisitCustomer> result = [];

    // Map untuk CG (Dari sheet dbase)
    final Map<String, String> cgCabutMapping = {};
    if (menu == 'pembukaan' && !isAdmin) {
      final emailLower = AppSessionCache.instance.officerEmail.trim().toLowerCase();
      final nameLower = nama.trim().toLowerCase();
      final isCg = emailLower.contains('cg') || nameLower.contains('cg');
      
      if (isCg) {
        // Menggunakan GAS endpoint filter-safe untuk mapping CG.
        // getValues() membaca semua baris termasuk yang difilter/disembunyikan.
        final dbaseUri = Uri.parse(
          '${AppConfig.gasReadSheetUrl}?action=read_sheet&sheet=${Uri.encodeComponent(AppConfig.ocrSheetName)}',
        );
        try {
          final dbaseRes = await http.get(dbaseUri).timeout(const Duration(seconds: AppConfig.connectionTimeoutSec));
          if (dbaseRes.statusCode == 200) {
            final dbLines = dbaseRes.body.replaceAll('\r\n', '\n').replaceAll('\r', '\n').split('\n');
            if (dbLines.isNotEmpty) {
              final dbCols = _parseCsvRow(dbLines.first);
              final idxIdpel = _findCsvColumnIndex(dbCols, const ['IDPEL', 'ID PEL', 'NO PELANGGAN']);
              final idxPtgs = _findCsvColumnIndex(dbCols, const ['PERSONIL', 'ptgs_kjg', 'ptgs', 'PETUGAS', 'NAMA PETUGAS', 'PETUGAS ORDER']);
              if (idxIdpel != null && idxPtgs != null) {
                for (var i = 1; i < dbLines.length; i++) {
                  if (dbLines[i].trim().isEmpty) continue;
                  final row = _parseCsvRow(dbLines[i]);
                  if (idxIdpel < row.length && idxPtgs < row.length) {
                    final id = row[idxIdpel].trim().toLowerCase();
                    if (id.isNotEmpty) {
                      cgCabutMapping[id] = _normalizeComparable(row[idxPtgs]);
                    }
                  }
                }
              }
            }
          }
        } catch (e) {
          // ignore error
        }
      }
    }

    for (var rowIndex = 0; rowIndex < rows.length; rowIndex++) {
      final cells = rows[rowIndex];
      if (idpelIndex >= cells.length) continue;
      final idpel = cells[idpelIndex].trim();
      if (idpel.isEmpty) continue;

      while (cells.length < columns.length) {
        cells.add('');
      }

      if (menu == 'kunjungan') {
        // Kolom ORDER harus 1
        if (orderIndex != null) {
          final orderVal = int.tryParse(cells[orderIndex].trim()) ?? 0;
          if (orderVal != 1) continue;
        }

        // Kolom STATUS harus 'y'
        if (statusIndex != null) {
          final statusStr = cells[statusIndex].trim().toLowerCase();
          if (statusStr != 'y') continue;
        }

        if (!isAdmin) {
          if (petugasIndex != null) {
            if (_normalizeComparable(cells[petugasIndex]) != normalizedPetugas) continue;
          }
        }

        final blnStr = (blnIndex != null && blnIndex < cells.length) ? cells[blnIndex].trim() : '';
        final blnVal = int.tryParse(blnStr) ?? 0;
        final rupiahStr = (rupiahIndex != null && rupiahIndex < cells.length) ? cells[rupiahIndex].trim() : '0';
        final rupiahVal = double.tryParse(rupiahStr.replaceAll(RegExp(r'[^0-9.]'), '')) ?? 0.0;
        final statusVal = (statusIndex != null && statusIndex < cells.length) ? cells[statusIndex].trim() : '';

        result.add(VisitCustomer(
          idpel: idpel,
          nama: (namaIndex != null && namaIndex < cells.length) ? cells[namaIndex].trim() : '',
          alamat: (alamatIndex != null && alamatIndex < cells.length) ? cells[alamatIndex].trim() : '',
          nomgrt: (nomgrtIndex != null && nomgrtIndex < cells.length) ? cells[nomgrtIndex].trim() : '',
          bln: blnVal,
          rupiah: rupiahVal,
          status: statusVal.isNotEmpty ? statusVal : 'Y',
          telepon: (teleponIndex != null && teleponIndex < cells.length) ? cells[teleponIndex].trim() : '',
          kendala: (kondisiIndex != null && kondisiIndex < cells.length) ? cells[kondisiIndex].trim() : '',
          lat: (latIndex != null && latIndex < cells.length) ? double.tryParse(cells[latIndex].trim()) : null,
          lng: (lngIndex != null && lngIndex < cells.length) ? double.tryParse(cells[lngIndex].trim()) : null,
        ));

      } else if (menu == 'pembukaan') {
        // filter kolom PENGADUAN_RP terdapat kata buka segel atau pasang kembali dan kolom STATUS_RP open
        if (kondisiIndex != null && statusIndex != null) {
          final colFValue = cells[kondisiIndex].trim().toLowerCase(); 
          final colHValue = cells[statusIndex].trim().toLowerCase(); 
          
          if (colHValue != 'open') continue;
          if (!colFValue.contains('buka segel') && !colFValue.contains('pasang kembali')) continue;
        } else {
          continue;
        }

        final emailLower = AppSessionCache.instance.officerEmail.trim().toLowerCase();
        final nameLower = nama.trim().toLowerCase();
        final isWr = emailLower.contains('wr') || nameLower.contains('wr');
        final isKd = emailLower.contains('kd') || nameLower.contains('kd');
        final isCg = emailLower.contains('cg') || nameLower.contains('cg');

        final ticketVal = (ticketIndex != null && ticketIndex < cells.length)
            ? cells[ticketIndex].trim().toLowerCase()
            : '';

        if (isWr) {
          if (!ticketVal.contains('wr')) continue;
        } else if (isKd) {
          if (!ticketVal.contains('kd')) continue;
        } else if (isCg) {
          if (!isAdmin) {
            // Filter ketat: hanya tampilkan IDPEL yang kolom PERSONIL-nya
            // di sheet dbase cocok dengan nama petugas yang sedang login.
            // Jika IDPEL tidak ditemukan di mapping atau petugas tidak sesuai → skip.
            final pengaduanIdpel = idpel.toLowerCase();
            final assignedPetugas = cgCabutMapping[pengaduanIdpel];
            if (assignedPetugas == null || assignedPetugas != normalizedPetugas) {
              continue;
            }
          }
        } else {
          if (!isAdmin && petugasIndex != null && petugasIndex < cells.length) {
            final petugas = _normalizeComparable(cells[petugasIndex]);
            if (petugas != normalizedPetugas) continue;
          }
        }

        final blnStr = (blnIndex != null && blnIndex < cells.length) ? cells[blnIndex].trim() : '';
        final blnVal = int.tryParse(blnStr) ?? 0;
        final rupiahStr = (rupiahIndex != null && rupiahIndex < cells.length) ? cells[rupiahIndex].trim() : '0';
        final rupiahVal = double.tryParse(rupiahStr.replaceAll(RegExp(r'[^0-9.]'), '')) ?? 0.0;
        final statusVal = (statusIndex != null && statusIndex < cells.length) ? cells[statusIndex].trim() : '';
        final kondisi = (kondisiIndex != null && kondisiIndex < cells.length) ? cells[kondisiIndex].trim() : '';

        result.add(VisitCustomer(
          idpel: idpel,
          nama: (namaIndex != null && namaIndex < cells.length) ? cells[namaIndex].trim() : '',
          alamat: (alamatIndex != null && alamatIndex < cells.length) ? cells[alamatIndex].trim() : (cells.length > 2 ? cells[2].trim() : ''),
          nomgrt: (nomgrtIndex != null && nomgrtIndex < cells.length) ? cells[nomgrtIndex].trim() : '',
          bln: blnVal,
          rupiah: rupiahVal,
          status: statusVal.isNotEmpty ? statusVal : 'Y',
          telepon: (teleponIndex != null && teleponIndex < cells.length) ? cells[teleponIndex].trim() : '',
          kendala: kondisi,
          lat: (latIndex != null && latIndex < cells.length) ? double.tryParse(cells[latIndex].trim()) : null,
          lng: (lngIndex != null && lngIndex < cells.length) ? double.tryParse(cells[lngIndex].trim()) : null,
        ));
      }
    }
    return result;
  }

  int? _findCsvColumnIndex(List<String> columns, List<String> candidates) {
    final normalized = candidates.map((value) => value.toUpperCase()).toList();
    for (var index = 0; index < columns.length; index++) {
      final col = columns[index].trim().toUpperCase();
      if (normalized.contains(col)) {
        return index;
      }
    }
    return null;
  }

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

  Future<Map<String, dynamic>> changePassword({
    required String user,
    required String oldPassword,
    required String newPassword,
  }) async {
    final payload = <String, dynamic>{
      'action': 'changePassword',
      'user': user,
      'oldPassword': oldPassword,
      'newPassword': newPassword,
    };

    if (_isPlaceholderUrl) {
      await Future.delayed(const Duration(seconds: 1));
      return {'status': 'success', 'message': 'Password berhasil diubah (Mock)'};
    }

    final data = await _post(
      payload,
      timeout: const Duration(seconds: 15),
    );

    if (data['status'] != 'success') {
      throw ApiException(
        data['message']?.toString() ?? 'Gagal mengubah password.',
      );
    }
    return data;
  }

  // ═══════════════════════════════════════════════════════════════════
  // [API BARU] Endpoint generik — GAS di sisi server HANYA mencari baris
  // dan menulis sel yang diminta, tanpa aturan bisnis. Semua keputusan
  // (status apa yang valid, kolom mana diisi apa) ada di sini, di Flutter.
  // ═══════════════════════════════════════════════════════════════════

  /// Mencari satu baris di [sheetName] berdasarkan [idpel] (default: kolom
  /// "ID Pelanggan"/"IDPEL") atau kolom lain lewat [keyColumn] (mis. "Ticket").
  /// Mengembalikan baik `row` (Map by nama header) maupun `rowValues` (List
  /// posisi asli kolom) — pakai `rowValues` untuk sheet yang nama headernya
  /// tidak diketahui pasti (mis. CABUT).
  Future<KunjunganRowResult> findRow({
    required String sheetName,
    required String idpel,
    String? keyColumn,
    String? filterColumn,
    String? filterValue,
  }) async {
    final payload = <String, dynamic>{
      'action': 'find_row',
      'sheetName': sheetName,
      'idpel': idpel,
      if (keyColumn != null) 'keyColumn': keyColumn,
      if (filterColumn != null) 'filterColumn': filterColumn,
      if (filterValue != null) 'filterValue': filterValue,
    };

    final data = await _post(payload, timeout: const Duration(seconds: AppConfig.connectionTimeoutSec));

    if (data['status'] != 'success') {
      throw ServerException(data['message']?.toString() ?? 'Data tidak ditemukan.');
    }
    return KunjunganRowResult(
      row: (data['row'] as Map?)?.cast<String, dynamic>() ?? {},
      rowValues: (data['rowValues'] as List?) ?? const [],
    );
  }

  /// Menulis [updates] (key = nama kolom PERSIS seperti header sheet) dan/atau
  /// [updatesByIndex] (key = nomor kolom 1-based sebagai String, dipakai kalau
  /// nama header tidak diketahui pasti) ke baris yang cocok dengan [idpel] di
  /// [sheetName]. GAS mencari ulang baris tersebut di server (bukan pakai
  /// index lama) supaya aman dari race condition.
  Future<void> updateRowCells({
    required String sheetName,
    required String idpel,
    String? keyColumn,
    String? filterColumn,
    String? filterValue,
    Map<String, dynamic>? updates,
    Map<String, dynamic>? updatesByIndex,
  }) async {
    final payload = <String, dynamic>{
      'action': 'update_row_cells',
      'sheetName': sheetName,
      'idpel': idpel,
      if (keyColumn != null) 'keyColumn': keyColumn,
      if (filterColumn != null) 'filterColumn': filterColumn,
      if (filterValue != null) 'filterValue': filterValue,
      if (updates != null) 'updates': updates,
      if (updatesByIndex != null) 'updatesByIndex': updatesByIndex,
    };

    final data = await _post(payload, timeout: const Duration(seconds: AppConfig.uploadTimeoutSec));

    if (data['status'] != 'success') {
      throw ServerException(data['message']?.toString() ?? 'Gagal menyimpan perubahan ke server.');
    }
  }

  /// Upload satu foto base64 ke Drive dan kembalikan URL-nya. Dipisah dari
  /// [updateRowCells] supaya kegagalan upload (izin folder Drive, dsb)
  /// bisa dibedakan dengan jelas dari kegagalan menulis sheet.
  Future<String> uploadPhoto({
    required String base64,
    required String filename,
    String? target,
  }) async {
    final payload = <String, dynamic>{
      'action': 'upload_photo',
      'base64': base64,
      'filename': filename,
      if (target != null) 'target': target,
    };

    final data = await _post(payload, timeout: const Duration(seconds: AppConfig.uploadTimeoutSec));

    if (data['status'] != 'success' || data['url'] == null) {
      throw ServerException(data['message']?.toString() ?? 'Gagal upload foto ke server.');
    }
    return data['url'].toString();
  }

  Future<Map<String, dynamic>> submitAction({
    required String idpel,
    required String actionType,
    required String fotoSebBase64,
    required String fotoSesBase64,
    String? nomgrtBru,
    String? stMgrt,
    required String metadata,
    String? lat,
    String? lng,
    String? timeBefore,
    String? timeAfter,
  }) async {
    final payload = <String, dynamic>{
      'action': 'submitAction',
      'IDPEL': idpel,
      'actionType': actionType,
      'fotoSebBase64': fotoSebBase64,
      'fotoSesBase64': fotoSesBase64,
      'nomgrt_bru': nomgrtBru ?? '',
      'st_mgrt': stMgrt ?? '',
      'metadata': metadata,
      'lat': lat ?? '',
      'lng': lng ?? '',
      'timeBefore': timeBefore ?? '',
      'timeAfter': timeAfter ?? '',
    };

    if (_isPlaceholderUrl) {
      await Future.delayed(const Duration(seconds: 2));
      return {'status': 'success', 'message': 'Data updated (Mock)'};
    }

    final data = await _post(
      payload,
      timeout: const Duration(seconds: AppConfig.uploadTimeoutSec),
    );

    if (data['status'] != 'success') {
      throw ServerException(
        data['message']?.toString() ?? 'Terjadi kesalahan pada server saat menyimpan.',
      );
    }
    return data;
  }
}
