import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:image/image.dart' as img;
import '../config/app_config.dart';
import 'ocr_meter_service.dart' show MeterOcrResult;

class GeminiOcrService {
  GeminiOcrService({
    String? apiKey,
    String? model,
    Duration timeout = const Duration(seconds: 20),
  })  : _apiKey = apiKey ?? AppConfig.geminiApiKey,
        _model = model ?? AppConfig.geminiModel,
        _timeout = timeout;

  final String _apiKey;
  final String _model;
  final Duration _timeout;

  static const String _promptText = '''
Anda adalah sistem pembaca stand meter gas dari foto yang diambil petugas di lapangan.

Pada foto ini, fokus HANYA pada satu deret angka mekanik (roda counter) yang:
- Berwarna PUTIH/TERANG di atas LATAR BELAKANG HITAM/GELAP
- Berjumlah 5 digit, dengan jarak antar digit agak renggang (masing-masing digit berada di kotak/roda sendiri)
- Adalah kelompok digit UTAMA/TERBESAR pada jendela angka meteran, dan merupakan angka cetak/roda mekanik ASLI — BUKAN tulisan tangan, coretan, atau stiker di badan meteran

ABAIKAN SEPENUHNYA, jangan pernah dimasukkan ke hasil:
- Digit dengan latar belakang MERAH atau yang dikelilingi kotak/garis MERAH (itu sub-meter desimal/dm3, BUKAN bagian dari pembacaan ini)
- Tulisan tangan, coretan, stiker, atau angka apa pun yang ditulis manual di badan meteran
- Barcode, nomor seri, merek, tahun produksi, satuan (m3/dm3), atau teks lain di luar jendela angka utama

Tentukan sendiri secara otomatis di mana letak jendela angka utama tersebut di dalam foto — posisinya bisa di mana saja dalam foto, jangan berasumsi selalu di tengah.

Jika gambar buram, terpotong, tertutup pantulan cahaya/silau, atau angka tidak bisa dipastikan dengan yakin, set "terbaca" menjadi false dan kosongkan "angka".

Jawab HANYA dengan JSON sesuai skema yang diberikan, tanpa penjelasan tambahan.''';

  static const Map<String, Object> _responseSchema = {
    'type': 'OBJECT',
    'properties': {
      'angka': {
        'type': 'STRING',
        'description':
            'Deret digit 0-9 dari angka putih di atas latar hitam, tanpa spasi/simbol. Kosongkan jika tidak yakin.',
      },
      'jumlah_digit': {
        'type': 'INTEGER',
        'description': 'Jumlah digit pada field angka.',
      },
      'terbaca': {
        'type': 'BOOLEAN',
        'description':
            'true jika yakin terbaca dengan jelas, false jika ragu/buram.',
      },
    },
    'required': ['angka', 'jumlah_digit', 'terbaca'],
  };

  Future<MeterOcrResult> scanImage(File imageFile) async {
    if (_apiKey.trim().isEmpty) {
      throw Exception('Gemini API key belum diatur di AppConfig.geminiApiKey');
    }

    final base64Image = await _prepareImage(imageFile);

    final url = Uri.parse(
      'https://generativelanguage.googleapis.com/v1beta/models/$_model:generateContent',
    );

    http.Response response;
    try {
      response = await http
          .post(
            url,
            headers: {
              'Content-Type': 'application/json',
              'x-goog-api-key': _apiKey,
            },
            body: jsonEncode({
              'contents': [
                {
                  'parts': [
                    {
                      'inlineData': {
                        'mimeType': 'image/jpeg',
                        'data': base64Image,
                      },
                    },
                    {'text': _promptText},
                  ],
                },
              ],
              'generationConfig': {
                'temperature': 0,
                'responseMimeType': 'application/json',
                'responseSchema': _responseSchema,
              },
            }),
          )
          .timeout(_timeout);
    } on TimeoutException {
      throw Exception('Waktu tunggu Gemini AI habis, periksa koneksi internet.');
    } catch (_) {
      throw Exception('Gagal menghubungi Gemini API. Periksa koneksi internet.');
    }

    if (response.statusCode != 200) {
      final detail = _extractErrorDetail(response.body);
      if (response.statusCode == 400 || response.statusCode == 403) {
        throw Exception('Gemini API key tidak valid atau tidak punya akses. $detail');
      }
      if (response.statusCode == 404) {
        throw Exception('Model "$_model" tidak ditemukan di Gemini API.');
      }
      if (response.statusCode == 429) {
        throw Exception('Kuota Gemini API habis sementara, coba lagi nanti.');
      }
      throw Exception('Gemini API error (${response.statusCode}). $detail');
    }

    return _parseResponse(response.body);
  }

  Future<String> _prepareImage(File imageFile) async {
    final originalBytes = await imageFile.readAsBytes();
    var decoded = img.decodeImage(originalBytes);
    if (decoded == null) {
      return base64Encode(originalBytes);
    }

    decoded = img.bakeOrientation(decoded);
    if (decoded.width > 1024) {
      decoded = img.copyResize(decoded, width: 1024);
    }

    final jpg = img.encodeJpg(decoded, quality: 85);
    return base64Encode(jpg);
  }

  MeterOcrResult _parseResponse(String body) {
    Map<String, dynamic> decodedBody;
    try {
      decodedBody = jsonDecode(body) as Map<String, dynamic>;
    } catch (_) {
      throw Exception('Gagal mem-parse respons Gemini.');
    }

    final candidates = decodedBody['candidates'] as List<dynamic>?;
    if (candidates == null || candidates.isEmpty) {
      throw Exception('Respons Gemini kosong (mungkin diblokir filter keamanan).');
    }

    final content = candidates.first is Map ? candidates.first['content'] : null;
    final parts = content is Map ? content['parts'] as List<dynamic>? : null;
    final textPart = parts?.firstWhere(
      (part) => part is Map && part['text'] is String,
      orElse: () => null,
    );

    final text = textPart is Map ? textPart['text'] as String? : null;
    if (text == null || text.trim().isEmpty) {
      throw Exception('Respons Gemini tidak berisi hasil yang bisa dibaca.');
    }

    Map<String, dynamic> parsed;
    try {
      parsed = jsonDecode(text) as Map<String, dynamic>;
    } catch (_) {
      throw Exception('Gagal mem-parse hasil JSON dari Gemini.');
    }

    final rawAngka = (parsed['angka'] as String?) ?? '';
    final digits = rawAngka.replaceAll(RegExp(r'[^0-9]'), '');
    final terbaca = parsed['terbaca'] == true;

    final validLength = digits.length == AppConfig.meterDigitCount ||
        digits.length == AppConfig.meterDigitCount - 1;

    final note = 'Gemini AI: angka=${rawAngka.isEmpty ? '-' : rawAngka} • '
        'jumlah_digit=${parsed['jumlah_digit'] ?? '-'} • '
        'terbaca=${terbaca ? 'ya' : 'tidak'}';

    return MeterOcrResult(
      digits: digits,
      rawText: note,
      candidates: digits.isEmpty ? const <String>[] : <String>[digits],
      isConfident: terbaca && validLength,
      score: terbaca && validLength ? 95 : (digits.isNotEmpty ? 55 : 0),
    );
  }

  String _extractErrorDetail(String body) {
    try {
      final decoded = jsonDecode(body) as Map<String, dynamic>;
      final error = decoded['error'];
      if (error is Map && error['message'] is String) {
        return error['message'] as String;
      }
    } catch (_) {}
    return '';
  }
}
