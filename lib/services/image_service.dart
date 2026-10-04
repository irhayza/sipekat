import 'dart:convert';
import 'dart:io';
import 'package:flutter_image_compress/flutter_image_compress.dart';
import 'package:image/image.dart' as img;
import '../config/app_config.dart';

class ImageService {
  ImageService._();
  static final ImageService instance = ImageService._();

  /// Kompres foto menggunakan native codec (flutter_image_compress).
  /// 10–20x lebih cepat dibanding decoding pure-Dart.
  /// Fallback ke pure-Dart jika native gagal.
  Future<String> compressToBase64(File file) async {
    final quality = (AppConfig.photoQuality * 100).round().clamp(1, 100);
    try {
      final compressed = await FlutterImageCompress.compressWithFile(
        file.absolute.path,
        minWidth: AppConfig.photoMaxWidth,
        minHeight: 0,
        quality: quality,
        format: CompressFormat.jpeg,
      );
      if (compressed != null && compressed.isNotEmpty) {
        return 'data:image/jpeg;base64,${base64Encode(compressed)}';
      }
    } catch (_) {
      // Native gagal → fallback ke pure-Dart
    }
    return _compressPureDart(file, quality);
  }

  Future<String> _compressPureDart(File file, int quality) async {
    final bytes = await file.readAsBytes();
    final decoded = img.decodeImage(bytes);
    if (decoded == null) {
      throw Exception('Berkas foto tidak bisa dibaca. Coba ambil ulang.');
    }
    var target = decoded;
    if (target.width > AppConfig.photoMaxWidth) {
      target = img.copyResize(target, width: AppConfig.photoMaxWidth);
    }
    final jpg = img.encodeJpg(target, quality: quality);
    return 'data:image/jpeg;base64,${base64Encode(jpg)}';
  }
}
