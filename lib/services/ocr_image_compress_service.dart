import 'package:flutter/foundation.dart';
import 'package:image/image.dart' as img;

class OcrImageCompressService {
  OcrImageCompressService._();

  static Future<Uint8List> compress(
    Uint8List original, {
    int maxDimension = 1280,
    int targetMaxBytes = 350 * 1024,
    int initialQuality = 85,
    int minQuality = 40,
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
  if (decoded == null) {
    return args.bytes;
  }

  var image = img.bakeOrientation(decoded);

  final longestSide = image.width > image.height ? image.width : image.height;
  if (longestSide > args.maxDimension) {
    image = image.width >= image.height
        ? img.copyResize(image, width: args.maxDimension)
        : img.copyResize(image, height: args.maxDimension);
  }

  int quality = args.initialQuality;
  Uint8List encoded = img.encodeJpg(image, quality: quality);

  while (encoded.length > args.targetMaxBytes && quality > args.minQuality) {
    quality = (quality - 10).clamp(args.minQuality, args.initialQuality);
    encoded = img.encodeJpg(image, quality: quality);
  }

  return encoded;
}

typedef ImageCompressService = OcrImageCompressService;
