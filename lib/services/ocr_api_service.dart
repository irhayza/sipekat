import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../config/app_config.dart';
import '../models/ocr/customer.dart';
import '../models/ocr/meter_reading.dart';
import 'app_session_cache.dart';

class OcrApiService {
  OcrApiService._();
  static final OcrApiService instance = OcrApiService._();

  Uri get _backendOcrUri => Uri.parse('${AppConfig.apiUrl}/readings/ocr');
  Uri get _sheetOcrDapelUri => _sheetUri(AppConfig.ocrSheetName);

  // Menggunakan GAS endpoint filter-safe sebagai pengganti gviz/tq.
  // getValues() di GAS membaca semua baris termasuk yang difilter/disembunyikan.
  Uri _sheetUri(String sheetName) {
    return Uri.parse(
      '${AppConfig.gasReadSheetUrl}?action=read_sheet&sheet=${Uri.encodeComponent(sheetName)}',
    );
  }

  Uri get _ocrUri {
    final dedicatedUrl = AppConfig.ocrServiceUrl.trim();
    if (dedicatedUrl.isNotEmpty) {
      return Uri.parse(dedicatedUrl);
    }
    return _backendOcrUri;
  }

  bool get _usesDedicatedOcrService => AppConfig.ocrServiceUrl.trim().isNotEmpty;
  bool get canSyncPendingReadings => AppConfig.ocrBridgeUrl.trim().isNotEmpty;

  Future<String?> _token() async {
    final p = await SharedPreferences.getInstance();
    return p.getString('session_token') ?? p.getString('auth_token');
  }

  Map<String, String> _headers(String? token) => {
    'Content-Type': 'application/json',
    if (token != null && token.isNotEmpty) 'Authorization': 'Bearer $token',
  };

  Duration get _timeout => const Duration(seconds: AppConfig.connectionTimeoutSec);
  Duration get _uploadTimeout => const Duration(seconds: AppConfig.uploadTimeoutSec);

  void _check(http.Response res, [String? msg]) {
    final bodyTrim = res.body.trim();
    final isHtml = bodyTrim.startsWith('<html') || 
                   bodyTrim.startsWith('<HTML') || 
                   bodyTrim.startsWith('<!DOCTYPE html') || 
                   bodyTrim.startsWith('<!doctype html');

    if (res.statusCode >= 200 && res.statusCode < 300) {
      if (isHtml) {
        throw Exception(
          '${msg ?? 'Request failed'}: Server mengembalikan halaman HTML (Status ${res.statusCode}). '
          'Pastikan URL Google Apps Script / Backend Bridge sudah benar dan dapat diakses tanpa login.',
        );
      }
      return;
    }

    if (isHtml) {
      throw Exception(
        '${msg ?? 'Request failed'}: Server error dengan halaman HTML (Status ${res.statusCode}). '
        'Mohon periksa konfigurasi Apps Script / Backend server Anda.',
      );
    }

    dynamic decoded;
    if (res.body.isNotEmpty) {
      try {
        decoded = jsonDecode(res.body);
      } catch (_) {}
    }

    final body = decoded is Map<String, dynamic>
        ? decoded
        : decoded is Map
            ? decoded.cast<String, dynamic>()
            : <String, dynamic>{};

    throw Exception(body['error'] ?? msg ?? 'Error ${res.statusCode}');
  }

  Map<String, dynamic> _normalizeOcrResponse(Map<String, dynamic> payload) {
    final source = payload['data'] is Map
        ? (payload['data'] as Map).cast<String, dynamic>()
        : payload;

    final rawStand = source['stand'] ?? source['angka'] ?? source['text'] ?? '';
    final standText = rawStand.toString();
    final digitsOnly = standText.replaceAll(RegExp(r'[^0-9]'), '');
    final confidenceValue = source['confidence'] ?? source['conf'] ?? source['score'];
    final confidence = confidenceValue is num
        ? confidenceValue.toDouble()
        : double.tryParse(confidenceValue?.toString() ?? '');
    final detected = source['terbaca'] is bool
        ? source['terbaca'] as bool
        : confidence != null
            ? digitsOnly.isNotEmpty && confidence >= 0.7
            : digitsOnly.isNotEmpty;

    return {
      'angka': digitsOnly,
      'terbaca': detected,
      if (confidence != null) 'confidence': confidence,
      if (standText.isNotEmpty) 'raw_text': standText,
      if (source['error'] != null) 'error': source['error'].toString(),
    };
  }

  String _cleanError(Object error) {
    return error.toString().replaceFirst('Exception: ', '').trim();
  }

  Map<String, dynamic> _decodeGvizResponse(String body) {
    final trimmed = body.trim();
    if (trimmed.startsWith('<html') || 
        trimmed.startsWith('<HTML') || 
        trimmed.startsWith('<!DOCTYPE html') || 
        trimmed.startsWith('<!doctype html')) {
      throw Exception(
        'Gagal membaca data spreadsheet: Server mengembalikan halaman HTML. '
        'Pastikan Spreadsheet Google Anda dipublikasikan ke web dan dapat diakses publik.',
      );
    }
    final start = body.indexOf('{');
    final end = body.lastIndexOf('}');
    if (start == -1 || end == -1 || end <= start) {
      throw Exception('Format data spreadsheet tidak dikenali. Pastikan sheet diatur dengan benar.');
    }
    try {
      return jsonDecode(body.substring(start, end + 1)) as Map<String, dynamic>;
    } catch (_) {
      throw Exception('Format JSON pada respon spreadsheet tidak valid.');
    }
  }

  dynamic _decodeJson(String body, [String? context]) {
    try {
      return jsonDecode(body);
    } catch (_) {
      throw Exception(
        '${context ?? 'Gagal memproses data'}: Respon dari server bukan format data JSON yang valid.',
      );
    }
  }

  int? _findColumnIndex(List<dynamic> columns, List<String> candidates) {
    final normalized = candidates.map((value) => value.toUpperCase()).toList();
    for (var index = 0; index < columns.length; index++) {
      final column = columns[index];
      if (column is! Map) continue;
      final label = column['label']?.toString().trim().toUpperCase() ?? '';
      final id = column['id']?.toString().trim().toUpperCase() ?? '';
      if (normalized.contains(label) || normalized.contains(id)) {
        return index;
      }
    }
    return null;
  }

  String _cellText(List<dynamic> cells, int index) {
    if (index < 0 || index >= cells.length) return '';
    final cell = cells[index];
    if (cell is! Map) return '';

    final formatted = cell['f']?.toString().trim();
    if (formatted != null && formatted.isNotEmpty) {
      return formatted;
    }

    final value = cell['v'];
    if (value == null) return '';
    if (value is num && value == value.roundToDouble()) {
      return value.toInt().toString();
    }
    return value.toString().trim();
  }

  String _normalizeComparable(String value) {
    return value.trim().toLowerCase().replaceAll(RegExp(r'\s+'), ' ');
  }

  int _compareText(String left, String right) {
    final leftNum = int.tryParse(left);
    final rightNum = int.tryParse(right);
    if (leftNum != null && rightNum != null) {
      return leftNum.compareTo(rightNum);
    }
    return left.compareTo(right);
  }

  Future<Map<String, dynamic>> _fetchPublicSheet(Uri uri, String label) async {
    final res = await http.get(uri).timeout(_timeout);
    if (res.statusCode < 200 || res.statusCode >= 300) {
      throw Exception('Sheet $label tidak bisa dibaca');
    }

    final body = res.body;
    // Cek apakah response adalah error JSON dari GAS
    if (body.trim().startsWith('{') && body.contains('"status":"error"')) {
      throw Exception('Sheet $label tidak ditemukan atau tidak bisa dibaca');
    }

    final lines = body
        .replaceAll('\r\n', '\n')
        .replaceAll('\r', '\n')
        .split('\n');

    if (lines.isEmpty) {
      throw Exception('Sheet $label kosong');
    }

    final header = _parseCsvRow(lines.first);
    // Format cols seperti gviz untuk kompatibilitas dengan kode yang ada
    final cols = header
        .map((h) => <String, dynamic>{'label': h.trim(), 'id': h.trim()})
        .toList();

    final rows = <Map<String, dynamic>>[];
    for (int i = 1; i < lines.length; i++) {
      final line = lines[i];
      if (line.trim().isEmpty) continue;
      final cells = _parseCsvRow(line);
      // Format cells seperti gviz untuk kompatibilitas dengan kode yang ada
      final c = cells.map((cell) => <String, dynamic>{'v': cell, 'f': cell}).toList();
      rows.add({'c': c});
    }

    return {'cols': cols, 'rows': rows};
  }

  /// Parse satu baris CSV dengan benar (menangani field yang dikutip)
  List<String> _parseCsvRow(String line) {
    final List<String> result = [];
    final StringBuffer current = StringBuffer();
    bool inQuotes = false;
    for (int i = 0; i < line.length; i++) {
      final char = line[i];
      if (char == '"') {
        if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
          current.write('"');
          i++;
        } else {
          inQuotes = !inQuotes;
        }
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

  Future<Map<String, dynamic>> _ocrViaDedicatedService(File imageFile) async {
    final bytes = await imageFile.readAsBytes();
    final req = http.MultipartRequest('POST', _ocrUri)
      ..files.add(
        http.MultipartFile.fromBytes(
          'file',
          bytes,
          filename: imageFile.uri.pathSegments.isNotEmpty
              ? imageFile.uri.pathSegments.last
              : 'meter.jpg',
        ),
      );

    final streamed = await req.send().timeout(_uploadTimeout);
    final res = await http.Response.fromStream(streamed);
    _check(res, 'OCR gagal');

    final payload = _decodeJson(res.body, 'OCR Service') as Map<String, dynamic>;
    final normalized = _normalizeOcrResponse(payload);
    if ((normalized['angka'] as String).isEmpty && normalized['error'] != null) {
      throw Exception(normalized['error']);
    }

    return {
      ...normalized,
      'source': 'dedicated',
    };
  }

  Future<Map<String, dynamic>> _ocrViaBackend(File imageFile) async {
    final t = await _token();
    final bytes = await imageFile.readAsBytes();
    final b64 = base64Encode(bytes);
    final res = await http.post(
      _backendOcrUri,
      headers: _headers(t),
      body: jsonEncode({'image_base64': b64, 'mime_type': 'image/jpeg'}),
    ).timeout(_uploadTimeout);
    _check(res, 'OCR gagal');

    return {
      ..._normalizeOcrResponse(_decodeJson(res.body, 'OCR Backend') as Map<String, dynamic>),
      'source': 'backend',
    };
  }

  Future<List<Customer>> getAssignedCustomers(String petugasName) async {
    final normalizedPetugas = _normalizeComparable(petugasName);
    if (normalizedPetugas.isEmpty) {
      return const <Customer>[];
    }

    final email = AppSessionCache.instance.officerEmail.trim().toLowerCase();
    final isAdmin = email.startsWith('admin') || email.startsWith('super');

    final table = await _fetchPublicSheet(
      _sheetOcrDapelUri,
      AppConfig.ocrSheetName,
    );
    final columns = (table['cols'] as List?) ?? const [];
    final rows = (table['rows'] as List?) ?? const [];

    final idpelIndex = _findColumnIndex(columns, const ['IDPEL', 'ID PEL', 'NO PELANGGAN']);
    final namaIndex = _findColumnIndex(columns, const ['NAMA']);
    final alamatIndex = _findColumnIndex(columns, const ['ALAMAT']);
    final tarifIndex = _findColumnIndex(columns, const ['TARIP', 'TARIF']);
    final meterIndex = _findColumnIndex(columns, const ['NOMGRT_LAMA', 'NOMETER', 'NO METER', 'METER']);
    final aktifIndex = _findColumnIndex(columns, const ['STATUSCEK', 'STATUS', 'AKTIF']);
    final sektorIndex = _findColumnIndex(columns, const ['SEKTOR']);
    final stLaluIndex = _findColumnIndex(columns, const ['RUPIAH', 'BLN']);
    final petugasIndex = _findColumnIndex(columns, const ['PERSONIL', 'PETUGAS']);
    final linkIndex = _findColumnIndex(columns, const ['Hasil_OCR', 'LINK_OCR', 'LINK']);

    if (idpelIndex == null) {
      throw Exception('Kolom IDPEL pada OCRDAPEL belum terbaca');
    }

    final deduped = <String, Customer>{};
    for (var rowIndex = 0; rowIndex < rows.length; rowIndex++) {
      final row = rows[rowIndex];
      if (row is! Map) continue;
      final cells = (row['c'] as List?) ?? const [];
      final idpel = _cellText(cells, idpelIndex);
      if (idpel.isEmpty) continue;

      final aktifVal = aktifIndex != null ? _cellText(cells, aktifIndex).trim() : '';
      if (aktifVal.toUpperCase() != 'Y') {
        continue;
      }

      // Filter: petugas harus cocok dengan nama akun
      if (!isAdmin) {
        final petugasVal = petugasIndex != null ? _cellText(cells, petugasIndex).trim() : '';
        if (_normalizeComparable(petugasVal) != normalizedPetugas) {
          continue;
        }
      }

      final linkFoto = linkIndex != null ? _cellText(cells, linkIndex) : '';
      if (linkFoto.trim().isNotEmpty) {
        continue;
      }

      final petugas = petugasIndex != null ? _cellText(cells, petugasIndex) : '';
      final standText = stLaluIndex != null ? _cellText(cells, stLaluIndex) : '';
      final parsedStand = int.tryParse(standText);
      deduped[idpel] = Customer(
        id: idpel,
        noPelanggan: idpel,
        noMeter: meterIndex != null ? _cellText(cells, meterIndex) : null,
        nama: namaIndex != null ? _cellText(cells, namaIndex) : idpel,
        alamat: alamatIndex != null ? _cellText(cells, alamatIndex) : null,
        standAwal: parsedStand ?? 0,
        standBulanLalu: parsedStand,
        bulanLalu: 'St Lalu',
        tarif: tarifIndex != null ? _cellText(cells, tarifIndex) : null,
        aktif: aktifIndex != null ? _cellText(cells, aktifIndex) : null,
        sektor: sektorIndex != null ? _cellText(cells, sektorIndex) : null,
        petugas: petugas,
        linkFoto: linkIndex != null ? _cellText(cells, linkIndex) : null,
        sheetRowNumber: rowIndex + 2,
      );
    }

    final customers = deduped.values.toList()
      ..sort((left, right) {
        final bySektor = _compareText(left.sektorLabel, right.sektorLabel);
        if (bySektor != 0) return bySektor;
        return _compareText(left.noPelanggan, right.noPelanggan);
      });

    return customers;
  }

  Future<List<Customer>> getAllDapellCustomers() async {
    final table = await _fetchPublicSheet(
      _sheetUri('dbase'),
      'dbase',
    );
    final columns = (table['cols'] as List?) ?? const [];
    final rows = (table['rows'] as List?) ?? const [];

    final idpelIndex = _findColumnIndex(columns, const ['IDPEL', 'ID PEL', 'NO PELANGGAN']);
    final namaIndex = _findColumnIndex(columns, const ['NAMA']);
    final alamatIndex = _findColumnIndex(columns, const ['ALAMAT']);
    final tarifIndex = _findColumnIndex(columns, const ['TARIP', 'TARIF']);
    final meterIndex = _findColumnIndex(columns, const ['NOMGRT_LAMA', 'NOMETER', 'NO METER', 'METER']);
    final aktifIndex = _findColumnIndex(columns, const ['STATUSCEK', 'STATUS', 'AKTIF']);
    final sektorIndex = _findColumnIndex(columns, const ['SEKTOR']);
    final stLaluIndex = _findColumnIndex(columns, const ['RUPIAH', 'BLN']);
    final petugasIndex = _findColumnIndex(columns, const ['PERSONIL', 'PTGS_KJG', 'PETUGAS', 'ptgs']);
    final linkIndex = _findColumnIndex(columns, const ['Hasil_OCR', 'LINK_OCR', 'LINK']);

    if (idpelIndex == null) return const <Customer>[];

    final List<Customer> result = [];
    for (var rowIndex = 0; rowIndex < rows.length; rowIndex++) {
      final row = rows[rowIndex];
      if (row is! Map) continue;
      final cells = (row['c'] as List?) ?? const [];
      final idpel = _cellText(cells, idpelIndex);
      if (idpel.isEmpty) continue;

      final standText = stLaluIndex != null ? _cellText(cells, stLaluIndex) : '';
      final parsedStand = int.tryParse(standText);
      final petugas = petugasIndex != null ? _cellText(cells, petugasIndex) : '';
      result.add(Customer(
        id: idpel,
        noPelanggan: idpel,
        noMeter: meterIndex != null ? _cellText(cells, meterIndex) : null,
        nama: namaIndex != null ? _cellText(cells, namaIndex) : idpel,
        alamat: alamatIndex != null ? _cellText(cells, alamatIndex) : null,
        standAwal: parsedStand ?? 0,
        standBulanLalu: parsedStand,
        bulanLalu: 'St Lalu',
        tarif: tarifIndex != null ? _cellText(cells, tarifIndex) : null,
        aktif: aktifIndex != null ? _cellText(cells, aktifIndex) : null,
        sektor: sektorIndex != null ? _cellText(cells, sektorIndex) : null,
        petugas: petugas,
        linkFoto: linkIndex != null ? _cellText(cells, linkIndex) : null,
        sheetRowNumber: rowIndex + 2,
      ));
    }
    return result;
  }

  Future<Customer?> findCustomerInDapel(String targetIdpel) async {
    final targetId = targetIdpel.trim().toLowerCase();
    if (targetId.isEmpty) return null;

    final table = await _fetchPublicSheet(
      _sheetUri('dbase'),
      'dbase',
    );
    final columns = (table['cols'] as List?) ?? const [];
    final rows = (table['rows'] as List?) ?? const [];

    final idpelIndex = _findColumnIndex(columns, const ['IDPEL', 'ID PEL', 'NO PELANGGAN']);
    final namaIndex = _findColumnIndex(columns, const ['NAMA']);
    final alamatIndex = _findColumnIndex(columns, const ['ALAMAT']);
    final tarifIndex = _findColumnIndex(columns, const ['TARIP', 'TARIF']);
    final meterIndex = _findColumnIndex(columns, const ['NOMGRT_LAMA', 'NOMETER', 'NO METER', 'METER']);
    final aktifIndex = _findColumnIndex(columns, const ['STATUSCEK', 'STATUS', 'AKTIF']);
    final sektorIndex = _findColumnIndex(columns, const ['SEKTOR']);
    final stLaluIndex = _findColumnIndex(columns, const ['RUPIAH', 'BLN']);
    final petugasIndex = _findColumnIndex(columns, const ['PERSONIL', 'PTGS_KJG', 'PETUGAS', 'ptgs']);
    final linkIndex = _findColumnIndex(columns, const ['Hasil_OCR', 'LINK_OCR', 'LINK']);

    if (idpelIndex == null) return null;

    for (var rowIndex = 0; rowIndex < rows.length; rowIndex++) {
      final row = rows[rowIndex];
      if (row is! Map) continue;
      final cells = (row['c'] as List?) ?? const [];
      final idpel = _cellText(cells, idpelIndex);
      if (idpel.toLowerCase() == targetId) {
        final standText = stLaluIndex != null ? _cellText(cells, stLaluIndex) : '';
        final parsedStand = int.tryParse(standText);
        final petugas = petugasIndex != null ? _cellText(cells, petugasIndex) : '';
        return Customer(
          id: idpel,
          noPelanggan: idpel,
          noMeter: meterIndex != null ? _cellText(cells, meterIndex) : null,
          nama: namaIndex != null ? _cellText(cells, namaIndex) : idpel,
          alamat: alamatIndex != null ? _cellText(cells, alamatIndex) : null,
          standAwal: parsedStand ?? 0,
          standBulanLalu: parsedStand,
          bulanLalu: 'St Lalu',
          tarif: tarifIndex != null ? _cellText(cells, tarifIndex) : null,
          aktif: aktifIndex != null ? _cellText(cells, aktifIndex) : null,
          sektor: sektorIndex != null ? _cellText(cells, sektorIndex) : null,
          petugas: petugas,
          linkFoto: linkIndex != null ? _cellText(cells, linkIndex) : null,
          sheetRowNumber: rowIndex + 2,
        );
      }
    }
    return null;
  }


  Future<List<Customer>> searchCustomers(String q) async {
    final t = await _token();
    final uri = Uri.parse('${AppConfig.apiUrl}/pelanggan/search').replace(
      queryParameters: {'q': q},
    );
    final res = await http.get(uri, headers: _headers(t)).timeout(_timeout);
    _check(res, 'Pencarian gagal');
    final list = (_decodeJson(res.body, 'Pencarian Pelanggan') as Map)['data'] as List;
    return list.map((e) => Customer.fromJson(e)).toList();
  }

  Future<Customer> getCustomer(String id) async {
    final t = await _token();
    final res = await http
        .get(Uri.parse('${AppConfig.apiUrl}/pelanggan/$id'), headers: _headers(t))
        .timeout(_timeout);
    _check(res, 'Pelanggan tidak ditemukan');
    return Customer.fromJson((_decodeJson(res.body, 'Ambil Data Pelanggan') as Map)['data']);
  }

  Future<List<MeterReading>> getReadingHistory(String pelangganId) async {
    final t = await _token();
    final res = await http
        .get(
          Uri.parse('${AppConfig.apiUrl}/readings/pelanggan/$pelangganId'),
          headers: _headers(t),
        )
        .timeout(_timeout);
    _check(res, 'Gagal ambil riwayat');
    final list = (_decodeJson(res.body, 'Ambil Riwayat') as Map)['data'] as List;
    return list.map((e) => MeterReading.fromJson(e)).toList();
  }

  Future<Map<String, dynamic>> ocrMeter(File imageFile) async {
    if (_usesDedicatedOcrService) {
      try {
        return await _ocrViaDedicatedService(imageFile);
      } catch (dedicatedError) {
        try {
          final fallback = await _ocrViaBackend(imageFile);
          return {
            ...fallback,
            'fallback': true,
            'fallback_reason': _cleanError(dedicatedError),
          };
        } catch (backendError) {
          throw Exception(
            'OCR tunnel gagal: ${_cleanError(dedicatedError)}. '
            'Backend utama juga gagal: ${_cleanError(backendError)}',
          );
        }
      }
    }

    return _ocrViaBackend(imageFile);
  }

  Future<Map<String, String>> syncOcrDapelReading({
    required MeterReading reading,
    required String petugasName,
  }) async {
    if (!canSyncPendingReadings) {
      throw Exception(
        'Sinkronisasi Google belum aktif. Isi AppConfig.googleBridgeUrl '
        'dengan URL Apps Script atau backend bridge.',
      );
    }

    String? imageBase64;
    String? fileName;
    final photoPath = reading.fotoPath?.trim();
    if (photoPath != null && photoPath.isNotEmpty) {
      final file = File(photoPath);
      if (await file.exists()) {
        final bytes = await file.readAsBytes();
        imageBase64 = base64Encode(bytes);
        fileName = file.uri.pathSegments.isNotEmpty
            ? file.uri.pathSegments.last
            : 'meter-${reading.pelangganId}.jpg';
      }
    }

    final body = <String, dynamic>{
      'action': 'sync_ocr_dapel',
      'spreadsheet_id': AppConfig.loginSheetId,
      'sheet_name': AppConfig.ocrSheetName,
      'drive_folder_id': AppConfig.ocrDriveFolderId,
      'idpel': reading.pelangganId,
      'st_lalu': reading.standAngka.toString(),
      'hasil_ocr': reading.standAngka.toString(),
      'stand_column': 'RUPIAH',
      'hasil_ocr_column': 'Hasil_OCR',
      'link_column': 'Link_OCR',
      'koordinat_column': 'LATITUDE_OCR',
      'koordinat_lng_column': 'LONGITUDE_OCR',
      'keterangan_column': 'CATATAN',
      'exif_column': 'EXIF_OCR',
      'keterangan': reading.catatan ?? '',
      'petugas': petugasName,
      'timestamp': reading.createdAt.toIso8601String(),
      if (reading.lat != null) 'latitude': reading.lat,
      if (reading.lng != null) 'longitude': reading.lng,
      if (reading.lat != null && reading.lng != null)
        'koordinat': '${reading.lat}, ${reading.lng}',
      if (reading.catatan != null && reading.catatan!.trim().isNotEmpty)
        'catatan': reading.catatan!.trim(),
      if (imageBase64 != null) 'image_base64': imageBase64,
      if (fileName != null) 'file_name': fileName,
      if (imageBase64 != null) 'content_type': 'image/jpeg',
    };

    final client = http.Client();
    http.Response res;
    try {
      final request = http.Request('POST', Uri.parse(AppConfig.ocrBridgeUrl))
        ..headers.addAll({'Content-Type': 'application/json'})
        ..body = jsonEncode(body)
        ..followRedirects = false;

      final streamedRes = await client.send(request).timeout(_uploadTimeout);
      res = await http.Response.fromStream(streamedRes);

      if (res.statusCode == 302 || res.statusCode == 307 || res.statusCode == 308 || res.statusCode == 301) {
        final redirectUrl = res.headers['location'] ?? res.headers['Location'];
        if (redirectUrl != null) {
          final redirectRequest = http.Request('GET', Uri.parse(redirectUrl))
            ..followRedirects = true;
          final redirectStreamedRes = await client.send(redirectRequest).timeout(_uploadTimeout);
          res = await http.Response.fromStream(redirectStreamedRes);
        }
      }
    } finally {
      client.close();
    }
    _check(res, 'Sinkronisasi OCRDAPEL gagal');

    final bodyTrim = res.body.trim();
    if (bodyTrim.startsWith('<html') || 
        bodyTrim.startsWith('<HTML') || 
        bodyTrim.startsWith('<!DOCTYPE html') || 
        bodyTrim.startsWith('<!doctype html')) {
      throw Exception(
        'Sinkronisasi OCRDAPEL gagal: Server mengembalikan halaman HTML (Status ${res.statusCode}). '
        'Pastikan URL Apps Script sudah benar, di-deploy sebagai Web App, dan diatur aksesnya untuk "Anyone" (siapa saja).',
      );
    }

    final payload = res.body.isNotEmpty
        ? _decodeJson(res.body, 'Sinkronisasi OCRDAPEL') as Map<String, dynamic>
        : <String, dynamic>{};
    final data = payload['data'] is Map
        ? (payload['data'] as Map).cast<String, dynamic>()
        : payload;
    final status = payload['status']?.toString().toLowerCase();
    if (status != null && status.isNotEmpty && status != 'ok' && status != 'success') {
      throw Exception(payload['error']?.toString() ?? 'Sinkronisasi OCRDAPEL gagal');
    }

    return {
      'foto_url': data['foto_url']?.toString() ??
          data['url']?.toString() ??
          data['link']?.toString() ??
          '',
      'foto_gdrive_id': data['foto_gdrive_id']?.toString() ??
          data['file_id']?.toString() ??
          data['drive_id']?.toString() ??
          '',
    };
  }

  Future<MeterReading> syncPendingReading(
    MeterReading reading, {
    String? petugasName,
  }) async {
    final syncedData = await syncOcrDapelReading(
      reading: reading,
      petugasName: petugasName ?? reading.petugasId,
    );

    return MeterReading(
      id: reading.id,
      pelangganId: reading.pelangganId,
      petugasId: reading.petugasId,
      standAngka: reading.standAngka,
      fotoPath: null,
      fotoUrl: syncedData['foto_url']?.isNotEmpty == true
          ? syncedData['foto_url']
          : reading.fotoUrl,
      fotoDriveId: syncedData['foto_gdrive_id']?.isNotEmpty == true
          ? syncedData['foto_gdrive_id']
          : reading.fotoDriveId,
      lat: reading.lat,
      lng: reading.lng,
      catatan: reading.catatan,
      bulan: reading.bulan,
      createdAt: reading.createdAt,
      isSynced: true,
      minus: reading.minus,
    );
  }

  Future<Map<String, String>> uploadPhoto(File imageFile, String pelangganId) async {
    final t = await _token();
    final bytes = await imageFile.readAsBytes();
    final b64 = base64Encode(bytes);
    final res = await http.post(
      Uri.parse('${AppConfig.apiUrl}/readings/upload-photo'),
      headers: _headers(t),
      body: jsonEncode({'image_base64': b64, 'pelanggan_id': pelangganId}),
    ).timeout(_uploadTimeout);
    _check(res, 'Upload foto gagal');
    final data = _decodeJson(res.body, 'Upload Foto') as Map<String, dynamic>;
    return {
      'foto_url': data['foto_url']?.toString() ?? '',
      'foto_gdrive_id': data['foto_gdrive_id']?.toString() ?? '',
    };
  }

  Future<MeterReading> submitReading(MeterReading r) async {
    final t = await _token();
    final res = await http.post(
      Uri.parse('${AppConfig.apiUrl}/readings'),
      headers: _headers(t),
      body: jsonEncode(r.toApiJson()),
    ).timeout(_timeout);
    _check(res, 'Gagal simpan data');
    return MeterReading.fromJson((_decodeJson(res.body, 'Simpan Bacaan') as Map)['data']);
  }

  Future<List<Map<String, dynamic>>> syncOfflineReadings(
    List<MeterReading> readings,
  ) async {
    if (canSyncPendingReadings) {
      final results = <Map<String, dynamic>>[];
      for (final reading in readings) {
        try {
          final synced = await syncPendingReading(reading);
          results.add({
            'id': synced.id,
            'status': 'ok',
            'foto_url': synced.fotoUrl,
            'foto_gdrive_id': synced.fotoDriveId,
          });
        } catch (error) {
          results.add({
            'id': reading.id,
            'status': 'error',
            'error': _cleanError(error),
          });
        }
      }
      return results;
    }

    final t = await _token();
    final res = await http.post(
      Uri.parse('${AppConfig.apiUrl}/readings/sync'),
      headers: _headers(t),
      body: jsonEncode({'readings': readings.map((r) => r.toApiJson()).toList()}),
    ).timeout(_uploadTimeout);
    _check(res, 'Sinkronisasi gagal');
    final results = (_decodeJson(res.body, 'Sinkronisasi Offline') as Map)['results'] as List;
    return results.cast<Map<String, dynamic>>();
  }
}


