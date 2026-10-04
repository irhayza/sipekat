import 'dart:io';
import 'package:exif/exif.dart';

/// Menentukan waktu pengambilan foto (dipakai untuk kolom "waktu foto" di
/// sheet, dan untuk penamaan file upload) dengan urutan prioritas:
///
///   1. EXIF asli dari file foto (DateTimeOriginal / DateTimeDigitized /
///      DateTime) -- paling akurat, berlaku baik untuk foto kamera HP
///      maupun foto lama dari galeri yang EXIF-nya masih ada.
///   2. Pola nama file WhatsApp (WhatsApp Image yyyy-MM-dd at HH.mm.ss,
///      IMG-yyyymmdd-WAxxxx) -- fallback kalau EXIF sudah hilang, umum
///      terjadi pada foto yang diteruskan lewat WhatsApp.
///   3. Waktu modifikasi file di penyimpanan HP -- fallback terakhir kalau
///      dua cara di atas tidak menghasilkan apa-apa.
///
/// Catatan: image_picker dipanggil dengan `imageQuality`/`maxWidth` di
/// beberapa layar, yang membuat plugin memampatkan ulang foto di level
/// native SEBELUM Dart menerimanya. Sebagian perangkat/OS ikut membuang
/// EXIF saat proses itu -- kalau itu terjadi, resolver ini otomatis lanjut
/// ke langkah 2/3 tanpa error, jadi tidak ada perilaku yang rusak.
class PhotoMetadataResolver {
  PhotoMetadataResolver._();

  static Future<DateTime> resolveCapturedTime({
    required File file,
    required String filename,
  }) async {
    final exifTime = await _readExifTime(file);
    if (exifTime != null) return exifTime;

    // Use current time as fallback for parseFromFilename
    final nameTime = _parseFromFilename(filename, DateTime.now());
    if (nameTime != null) return nameTime;

    return DateTime.now();
  }

  /// Format nama file upload sesuai permintaan: yymmddhhmm (10 digit,
  /// tanpa pemisah). Contoh: 21 Jul 2026 14:35 -> "2607211435".
  static String toFilenameStamp(DateTime dt) {
    String two(int v) => v.toString().padLeft(2, '0');
    final yy = two(dt.year % 100);
    final mm = two(dt.month);
    final dd = two(dt.day);
    final hh = two(dt.hour);
    final mi = two(dt.minute);
    return '$yy$mm$dd$hh$mi';
  }

  // ── EXIF ────────────────────────────────────────────────────────────────

  static Future<DateTime?> _readExifTime(File file) async {
    try {
      final bytes = await file.readAsBytes();
      final tags = await readExifFromBytes(bytes);
      if (tags.isEmpty) return null;

      // Urutan prioritas tag: DateTimeOriginal = saat rana ditekan (paling
      // akurat), DateTimeDigitized = saat file digital dibuat, Image
      // DateTime = waktu terakhir file diubah menurut kamera/editor.
      const candidateKeys = [
        'EXIF DateTimeOriginal',
        'EXIF DateTimeDigitized',
        'Image DateTime',
      ];

      for (final key in candidateKeys) {
        final tag = tags[key];
        if (tag == null) continue;
        final parsed = _parseExifDateTimeString(tag.toString());
        if (parsed != null) return parsed;
      }
    } catch (_) {
      // Foto tanpa EXIF, format tidak didukung, atau data EXIF korup --
      // anggap saja tidak ada, biar lanjut ke fallback berikutnya.
    }
    return null;
  }

  /// Format standar EXIF: "yyyy:MM:dd HH:mm:ss".
  static DateTime? _parseExifDateTimeString(String raw) {
    final m = RegExp(r'^(\d{4}):(\d{2}):(\d{2})\s+(\d{2}):(\d{2}):(\d{2})')
        .firstMatch(raw.trim());
    if (m == null) return null;
    try {
      return DateTime(
        int.parse(m.group(1)!),
        int.parse(m.group(2)!),
        int.parse(m.group(3)!),
        int.parse(m.group(4)!),
        int.parse(m.group(5)!),
        int.parse(m.group(6)!),
      );
    } catch (_) {
      return null;
    }
  }

  // ── Nama file (pola WhatsApp) ──────────────────────────────────────────
  // Logika ini persis logika lama yang sebelumnya ada langsung di
  // reopen_detail_screen.dart / visit_detail_screen.dart, hanya dipindah
  // ke sini agar dipakai bersama tanpa duplikasi.

  static DateTime? _parseFromFilename(String filename, DateTime fallbackTime) {
    final normalized = filename.trim().toLowerCase();
    if (normalized.isEmpty) return null;

    // 1. Pola lengkap (Tahun-Bulan-Tanggal Jam-Menit-Detik)
    // Mencakup format Screenshot, kamera bawaan, WhatsApp lengkap.
    final fullPatterns = <RegExp>[
      RegExp(r'(\d{4})(\d{2})(\d{2})[_-](\d{2})(\d{2})(\d{2})'),
      RegExp(r'(\d{4})-(\d{2})-(\d{2})[ _-](\d{2})[.\-:](\d{2})[.\-:](\d{2})'),
      RegExp(r'(\d{4})_(\d{2})_(\d{2})[ _-](\d{2})_(\d{2})_(\d{2})'),
      RegExp(r'whatsapp\s+image\s+(\d{4})-(\d{2})-(\d{2})\s+at\s+(\d{2})[.\-:](\d{2})[.\-:](\d{2})'),
    ];

    for (final pattern in fullPatterns) {
      final match = pattern.firstMatch(normalized);
      if (match != null) {
        try {
          return DateTime(
            int.parse(match.group(1)!),
            int.parse(match.group(2)!),
            int.parse(match.group(3)!),
            int.parse(match.group(4)!),
            int.parse(match.group(5)!),
            int.parse(match.group(6)!),
          );
        } catch (_) {}
      }
    }

    // 2. Pola tanpa detik (Tahun-Bulan-Tanggal Jam-Menit)
    final waReg1b = RegExp(
        r'whatsapp\s+image\s+(\d{4})-(\d{2})-(\d{2})\s+at\s+(\d{2})[.\-:](\d{2})');
    final match1b = waReg1b.firstMatch(normalized);
    if (match1b != null) {
      try {
        return DateTime(
          int.parse(match1b.group(1)!),
          int.parse(match1b.group(2)!),
          int.parse(match1b.group(3)!),
          int.parse(match1b.group(4)!),
          int.parse(match1b.group(5)!),
          0,
        );
      } catch (_) {}
    }

    // 3. Pola tanggal saja (IMG-20240722-WA0001)
    final dateOnlyPatterns = <RegExp>[
      RegExp(r'img-(\d{4})(\d{2})(\d{2})-wa\d*'),
    ];
    for (final pattern in dateOnlyPatterns) {
      final match = pattern.firstMatch(normalized);
      if (match != null) {
        try {
          return DateTime(
            int.parse(match.group(1)!),
            int.parse(match.group(2)!),
            int.parse(match.group(3)!),
            fallbackTime.hour,
            fallbackTime.minute,
            fallbackTime.second,
          );
        } catch (_) {}
      }
    }

    return null;
  }
}
