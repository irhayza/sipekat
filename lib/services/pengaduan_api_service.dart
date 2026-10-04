import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/app_config.dart';

class PengaduanApiService {
  // [FIX v9.7] Sebelumnya hanya mengenali SATU ID contoh yang di-hardcode
  // ("6120990001") — bukan pencarian sungguhan. Endpoint sisi server untuk
  // ini memang belum ada sebelumnya (DapellService.getById() cuma bisa
  // dipanggil dari dalam GAS, tidak lewat HTTP). Sekarang sudah ada route
  // 'get_customer_by_id' di Code.gs, jadi ini bisa memanggil data asli.
  static Future<Map<String, dynamic>?> checkCustomerId(String id) async {
    final client = http.Client();
    try {
      final request = http.Request('POST', Uri.parse(AppConfig.pengaduanBridgeUrl))
        ..headers['Content-Type'] = 'application/json'
        ..body = jsonEncode({'action': 'get_customer_by_id', 'idpel': id})
        ..followRedirects = false;

      final streamedRes = await client
          .send(request)
          .timeout(const Duration(seconds: AppConfig.connectionTimeoutSec));
      final response = await http.Response.fromStream(streamedRes);

      if (response.statusCode != 200) return null;

      final decoded = jsonDecode(response.body);
      if (decoded is Map && decoded['status'] == 'success') {
        return {
          'nama': decoded['nama']?.toString() ?? '',
          'alamat': decoded['alamat']?.toString() ?? '',
        };
      }
      return null;
    } catch (_) {
      return null;
    } finally {
      client.close();
    }
  }

  // Submit complaint with redirect handling
  static Future<Map<String, dynamic>> submitComplaint(Map<String, dynamic> data) async {
    final client = http.Client();
    try {
      final request = http.Request('POST', Uri.parse(AppConfig.pengaduanBridgeUrl))
        ..headers['Content-Type'] = 'application/json'
        // [FIX v9.7 — PENTING] Sebelumnya body ini TIDAK punya field
        // 'action' sama sekali. doPost() di Code.gs merutekan semua request
        // berdasarkan payload.action; tanpa field ini requestnya jatuh ke
        // jalur webhook WAHA/Telegram dan dibalas 'Unauthorized' atau 'OK'
        // polos (bukan JSON) — pengaduan TIDAK PERNAH benar-benar tersimpan.
        // Sekarang ada action eksplisit yang dikenali route baru 'submit_pengaduan'.
        ..body = jsonEncode({'action': 'submit_pengaduan', 'data': data})
        ..followRedirects = false;

      final streamedRes = await client
          .send(request)
          .timeout(const Duration(seconds: AppConfig.connectionTimeoutSec));
      var response = await http.Response.fromStream(streamedRes);

      // Follow redirect if 3xx status code
      if (response.statusCode == 302 ||
          response.statusCode == 307 ||
          response.statusCode == 308 ||
          response.statusCode == 301 ||
          response.statusCode == 303) {
        final redirectUrl = response.headers['location'] ?? response.headers['Location'];
        if (redirectUrl != null) {
          final redirectRequest = http.Request('GET', Uri.parse(redirectUrl))
            ..followRedirects = true;
          final redirectStreamedRes = await client
              .send(redirectRequest)
              .timeout(const Duration(seconds: AppConfig.connectionTimeoutSec));
          response = await http.Response.fromStream(redirectStreamedRes);
        }
      }

      if (response.statusCode == 200) {
        try {
          return jsonDecode(response.body);
        } catch (e) {
          final trimmed = response.body.trim();
          if (trimmed == 'Unauthorized') {
            return {
              "ok": false,
              "message": "Akses Ditolak (Unauthorized). Pastikan Web App dideploy sebagai 'Anyone' (Siapa saja) atau token script diatur dengan benar.",
            };
          }
          return {
            "ok": false,
            "message": "Response server tidak valid (bukan JSON). Hubungi Admin.\nResponse: "
                "${trimmed.length > 150 ? '${trimmed.substring(0, 150)}...' : trimmed}",
          };
        }
      } else {
        return {
          "ok": false,
          "message": "Gagal terhubung ke server (Status ${response.statusCode})."
        };
      }
    } catch (e) {
      if (e is FormatException) {
        return {
          "ok": false,
          "message": "Gagal membaca data dari server: $e",
        };
      }
      return {
        "ok": false,
        "message": "Tidak bisa terhubung ke server. Periksa koneksi internet lalu coba kirim lagi. ($e)",
      };
    } finally {
      client.close();
    }
  }
}
