import 'dart:io';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter_image_compress/flutter_image_compress.dart';
import 'package:image/image.dart' as img;

/// Kompresi gambar menggunakan native codec (flutter_image_compress).
/// 10-20x lebih cepat dari pure-Dart. Ada fallback ke pure-Dart jika gagal.
class KunjunganImageCompressService {
  KunjunganImageCompressService._();

  static const int _maxDimension    = 800;
  static const int _quality         = 55;
  static const int _targetMaxBytes  = 250 * 1024; // 250 KB

  /// Kompres [File] dan kembalikan base64 string siap pakai.
  static Future<String> compressFileToBase64(File file) async {
    try {
      final compressed = await FlutterImageCompress.compressWithFile(
        file.absolute.path,
        minWidth: _maxDimension,
        minHeight: 0,
        quality: _quality,
        format: CompressFormat.jpeg,
      );
      if (compressed != null && compressed.isNotEmpty) {
        return 'data:image/jpeg;base64,${base64Encode(compressed)}';
      }
    } catch (_) {
      // fallback ke pure-Dart
    }
    final raw = await file.readAsBytes();
    final fallback = await compress(raw);
    return 'data:image/jpeg;base64,${base64Encode(fallback)}';
  }

  /// Kompres [Uint8List] — digunakan jika sudah punya bytes.
  static Future<Uint8List> compress(
    Uint8List original, {
    int maxDimension = _maxDimension,
    int targetMaxBytes = _targetMaxBytes,
    int initialQuality = _quality,
    int minQuality = 30,
  }) {
    return compute(
      _compressIsolate,
      _CompressArgs(
        bytes: original,
        maxDimension: maxDimension,
        targetMaxBytes: targetMaxBytes,
        initialQuality: initialQuality,
        minQuality: minQuality,
      ),
    );
  }
}

class _CompressArgs {
  final Uint8List bytes;
  final int maxDimension;
  final int targetMaxBytes;
  final int initialQuality;
  final int minQuality;

  const _CompressArgs({
    required this.bytes,
    required this.maxDimension,
    required this.targetMaxBytes,
    required this.initialQuality,
    required this.minQuality,
  });
}

Uint8List _compressIsolate(_CompressArgs args) {
  final decoded = img.decodeImage(args.bytes);
  if (decoded == null) return args.bytes;

  var image = img.bakeOrientation(decoded);

  final longestSide = image.width > image.height ? image.width : image.height;
  if (longestSide > args.maxDimension) {
    image = image.width >= image.height
        ? img.copyResize(image, width: args.maxDimension)
        : img.copyResize(image, height: args.maxDimension);
  }

  // Turunkan quality secara adaptif sampai ukuran target terpenuhi
  int quality = args.initialQuality;
  List<int> out = img.encodeJpg(image, quality: quality);
  while (out.length > args.targetMaxBytes && quality > args.minQuality) {
    quality -= 5;
    out = img.encodeJpg(image, quality: quality);
  }
  return Uint8List.fromList(out);
}
