import 'dart:async';
import 'dart:io';
import 'package:http/http.dart' as http;
import '../config/app_config.dart';

class AuthException implements Exception {
  final String message;
  const AuthException(this.message);
  @override
  String toString() => message;
}

/// Shared authentication service for all SiPEKAT modules.
/// Validates credentials against the LOGIN sheet in Google Sheets.
/// Menggunakan GAS endpoint (filter-safe) sebagai pengganti gviz/tq.
class AuthService {
  AuthService._();
  static final AuthService instance = AuthService._();

  Future<Map<String, dynamic>> login(String email, String password) async {
    // Menggunakan GAS endpoint filter-safe: getValues() membaca semua baris
    // termasuk baris yang disembunyikan/difilter di UI Google Sheets.
    final url =
        '${AppConfig.gasReadSheetUrl}?action=read_sheet&sheet=${Uri.encodeComponent(AppConfig.loginSheetName)}';
    final client = http.Client();
    try {
      final res = await client
          .get(Uri.parse(url))
          .timeout(const Duration(seconds: AppConfig.connectionTimeoutSec));
      if (res.statusCode != 200) {
        throw AuthException(
            'Gagal memuat data login dari server (Status ${res.statusCode}).');
      }

      final body = res.body;
      final lines = body
          .replaceAll('\r\n', '\n')
          .replaceAll('\r', '\n')
          .split('\n');

      if (lines.isEmpty) {
        throw const AuthException('Format data login tidak valid.');
      }

      // Lewati baris header (baris ke-0), mulai dari baris ke-1
      for (int i = 1; i < lines.length; i++) {
        final line = lines[i];
        if (line.trim().isEmpty) continue;
        final cells = _parseCsvRow(line);
        final rowEmail = cells.isNotEmpty ? cells[0].toLowerCase().trim() : '';
        final rowPassword = cells.length > 1 ? cells[1].trim() : '';
        final rowNama = cells.length > 2 ? cells[2].trim() : '';

        if (rowEmail == email.toLowerCase().trim() &&
            rowPassword == password.trim()) {
          return {
            'status': 'success',
            'nama': rowNama,
            'email': rowEmail,
          };
        }
      }

      throw const AuthException('Email atau password salah.');
    } on TimeoutException {
      throw const AuthException(
          'Koneksi timeout. Periksa koneksi internet Anda.');
    } on SocketException {
      throw const AuthException(
          'Tidak ada koneksi internet. Sambungkan ke internet lalu coba lagi.');
    } finally {
      client.close();
    }
  }

  /// Parse satu baris CSV dengan benar (menangani field yang dikutip)
  static List<String> _parseCsvRow(String line) {
    final List<String> result = [];
    final StringBuffer current = StringBuffer();
    bool inQuotes = false;
    for (int i = 0; i < line.length; i++) {
      final char = line[i];
      if (char == '"') {
        if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
          // Escaped quote ("") → tambahkan satu kutip
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
}
