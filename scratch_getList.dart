import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:sipekat_v3/config/app_config.dart';
import 'package:sipekat_v3/models/visit_customer.dart';
import 'package:sipekat_v3/services/app_session_cache.dart';

class ApiException implements Exception {
  final String message;
  ApiException(this.message);
}

class Test {
  bool _isPlaceholderUrl = false;
  Future<List<VisitCustomer>> _mockGetList(String nama, String role, String menu) async => [];
  
  String _normalizeComparable(String value) {
    return value.trim().toLowerCase().replaceAll(RegExp(r'\s+'), ' ');
  }

  int? _findCsvColumnIndex(List<String> columns, List<String> possibleNames) {
    for (int i = 0; i < columns.length; i++) {
      final colName = _normalizeComparable(columns[i]);
      for (final possibleName in possibleNames) {
        if (colName == _normalizeComparable(possibleName)) {
          return i;
        }
      }
    }
    return null;
  }

  List<String> _parseCsvRow(String line) => [];

  // ------------------------- GET LIST -----------------------------
  Future<List<VisitCustomer>> getList(String nama, String role, String menu) async {
    if (_isPlaceholderUrl) {
      await Future.delayed(const Duration(milliseconds: 800));
      return _mockGetList(nama, role, menu);
    }

    final email = AppSessionCache.instance.officerEmail.trim().toLowerCase();
    final isAdmin = email.startsWith('admin') || email.startsWith('super');

    final sheetName = (menu == 'kunjungan') ? AppConfig.ocrSheetName : AppConfig.pengaduanSheetName;
    final String sheetParam = 'sheet=\';
    final csvUri = Uri.parse(
      'https://docs.google.com/spreadsheets/d/\/export?format=csv&\',
    );

    late final String csvText;
    try {
      final res = await http.get(csvUri).timeout(const Duration(seconds: AppConfig.connectionTimeoutSec));
      if (res.statusCode != 200) {
        throw ApiException('Server merespon dengan status \');
      }
      csvText = res.body;
    } catch (e) {
      throw ApiException('Gagal memuat data pelanggan (\): \');
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
    final petugasIndex = _findCsvColumnIndex(columns, const ['PETUGAS', 'PETUGAS ORDER', 'NAMA PETUGAS', 'Petugas_RP', 'ptgs']);
    final kondisiIndex = _findCsvColumnIndex(columns, const ['KONDISI', 'KETERANGAN', 'PENGADUAN', 'PENGADUAN_RP']);
    final teleponIndex = _findCsvColumnIndex(columns, const ['TELEPON', 'TELP', 'TELFON', 'NO HP', 'NOHP', 'HP', 'WHATSAPP', 'WA', 'TELFON_RP']);
    final latIndex = _findCsvColumnIndex(columns, const ['LAT', 'LATITUDE', 'Latitude_RP']);
    final lngIndex = _findCsvColumnIndex(columns, const ['LNG', 'LON', 'LONGITUDE', 'Longitude_RP']);
    final ticketIndex = _findCsvColumnIndex(columns, const ['TICKET', 'NO TICKET', 'TIKET', 'NO TIKET', 'Ticket_RP']);
    final mrsIndex = _findCsvColumnIndex(columns, const ['MRS']);
    final orderIndex = _findCsvColumnIndex(columns, const ['ORDER']);

    if (idpelIndex == null) {
      throw ApiException('Kolom IDPEL pada sheet \ tidak ditemukan');
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
        final dbaseUri = Uri.parse(
          'https://docs.google.com/spreadsheets/d/\/export?format=csv&sheet=\',
        );
        try {
          final dbaseRes = await http.get(dbaseUri).timeout(const Duration(seconds: AppConfig.connectionTimeoutSec));
          if (dbaseRes.statusCode == 200) {
            final dbLines = dbaseRes.body.replaceAll('\r\n', '\n').replaceAll('\r', '\n').split('\n');
            if (dbLines.isNotEmpty) {
              final dbCols = _parseCsvRow(dbLines.first);
              final idxIdpel = _findCsvColumnIndex(dbCols, const ['IDPEL', 'ID PEL', 'NO PELANGGAN']);
              final idxPtgs = _findCsvColumnIndex(dbCols, const ['ptgs', 'PETUGAS']);
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

        if (!isAdmin) {
          final emailLower = AppSessionCache.instance.officerEmail.trim().toLowerCase();
          final nameLower = nama.trim().toLowerCase();
          final isKd = emailLower.contains('kd') || nameLower.contains('kd');
          final isWr = emailLower.contains('wr') || nameLower.contains('wr');
          final isCg = emailLower.contains('cg') || nameLower.contains('cg');

          if (isKd || isWr) {
             if (mrsIndex != null) {
               final cellMrs = cells[mrsIndex].trim().toLowerCase();
               if (isKd && !cellMrs.contains('kd')) continue;
               if (isWr && !cellMrs.contains('wr')) continue;
             }
          } else if (isCg) {
             if (petugasIndex != null) {
               if (_normalizeComparable(cells[petugasIndex]) != normalizedPetugas) continue;
             }
          } else {
             if (petugasIndex != null) {
               if (_normalizeComparable(cells[petugasIndex]) != normalizedPetugas) continue;
             }
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
            final pengaduanIdpel = idpel.toLowerCase();
            final assignedPetugas = cgCabutMapping[pengaduanIdpel];
            if (assignedPetugas != normalizedPetugas) {
              continue; 
            }
          }
        } else {
          if (!isAdmin && petugasIndex != null && petugasIndex < cells.length) {
            final petugas = cells[petugasIndex];
            if (_normalizeComparable(petugas) != normalizedPetugas) continue;
          }
        }

        result.add(VisitCustomer(
          idpel: idpel,
          nama: (namaIndex != null && namaIndex < cells.length) ? cells[namaIndex].trim() : '',
          alamat: (alamatIndex != null && alamatIndex < cells.length) ? cells[alamatIndex].trim() : '',
          nomgrt: (nomgrtIndex != null && nomgrtIndex < cells.length) ? cells[nomgrtIndex].trim() : '',
          bln: 0,
          rupiah: 0.0,
          status: 'OPEN',
          telepon: (teleponIndex != null && teleponIndex < cells.length) ? cells[teleponIndex].trim() : '',
          kendala: (kondisiIndex != null && kondisiIndex < cells.length) ? cells[kondisiIndex].trim() : '',
          lat: (latIndex != null && latIndex < cells.length) ? double.tryParse(cells[latIndex].trim()) : null,
          lng: (lngIndex != null && lngIndex < cells.length) ? double.tryParse(cells[lngIndex].trim()) : null,
        ));
      }
    }

    return result;
  }
}
