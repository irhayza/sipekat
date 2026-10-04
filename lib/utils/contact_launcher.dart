import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

/// Kumpulan helper untuk aksi kontak & lokasi yang dipakai di seluruh modul
/// SiPEKAT (Perbaikan, Pengaduan, Kunjungan, Pencatatan Meter): menelepon,
/// membuka WhatsApp, membuka lokasi di peta, dan membagikan titik lokasi.
///
/// Disatukan di sini supaya setiap modul memakai perilaku & format nomor
/// yang identik — tidak ada lagi implementasi WA/telepon yang berbeda-beda
/// antar modul.
class ContactLauncher {
  ContactLauncher._();

  /// Menormalkan nomor telepon Indonesia ke format internasional (62...)
  /// tanpa tanda '+', spasi, tanda kurung, atau strip.
  /// Contoh: "0812-3456-7890" -> "6281234567890"
  static String? normalizePhone(String? raw) {
    if (raw == null) return null;
    var digits = raw.replaceAll(RegExp(r'[^0-9]'), '');
    if (digits.isEmpty) return null;

    if (digits.startsWith('0')) {
      digits = '62${digits.substring(1)}';
    } else if (digits.startsWith('8')) {
      // Nomor tanpa awalan 0 (kadang tersimpan begitu di sheet)
      digits = '62$digits';
    }
    // Sudah diawali 62 -> biarkan apa adanya.

    // Nomor Indonesia yang valid minimal ~10 digit termasuk kode negara.
    if (digits.length < 9) return null;
    return digits;
  }

  static bool hasValidPhone(String? raw) => normalizePhone(raw) != null;

  /// Membuka aplikasi telepon dengan nomor yang sudah diisi.
  static Future<bool> callPhone(String? raw) async {
    final normalized = normalizePhone(raw);
    if (normalized == null) return false;
    final uri = Uri(scheme: 'tel', path: normalized);
    return launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  /// Membuka WhatsApp langsung ke chat nomor tersebut, dengan pesan
  /// pembuka opsional.
  static Future<bool> openWhatsApp(String? raw, {String? message}) async {
    final normalized = normalizePhone(raw);
    if (normalized == null) return false;
    final uri = Uri.parse(
      'https://wa.me/$normalized'
      '${message != null && message.isNotEmpty ? '?text=${Uri.encodeComponent(message)}' : ''}',
    );
    return launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  /// Membuka titik koordinat di aplikasi peta (Google Maps atau default).
  static Future<bool> openMap(double lat, double lng, {String? label}) async {
    final uri = Uri.parse(
      'https://www.google.com/maps/search/?api=1&query=$lat,$lng',
    );
    return launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  /// Membagikan titik lokasi lewat WhatsApp (mis. ke pelanggan/rekan tim).
  static Future<bool> shareLocationViaWhatsApp(
    double lat,
    double lng, {
    String? toPhone,
    String? label,
  }) async {
    final mapsUrl = 'https://www.google.com/maps/search/?api=1&query=$lat,$lng';
    final text = label != null && label.isNotEmpty
        ? 'Lokasi $label: $mapsUrl'
        : 'Lokasi: $mapsUrl';

    if (toPhone != null && hasValidPhone(toPhone)) {
      return openWhatsApp(toPhone, message: text);
    }
    // Tidak ada nomor tujuan -> buka WA dengan teks siap kirim ke kontak manapun.
    final uri = Uri.parse('https://wa.me/?text=${Uri.encodeComponent(text)}');
    return launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  /// Menampilkan snackbar ringan jika sebuah aksi gagal dibuka
  /// (mis. tidak ada aplikasi yang menangani intent-nya).
  static void showLaunchFailure(BuildContext context, String what) {
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Tidak bisa membuka $what di perangkat ini.')),
    );
  }
}
