import 'dart:io';
import 'package:image/image.dart' as img;
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import '../utils/photo_metadata_resolver.dart';

class PhotoTimestampResolution {
  const PhotoTimestampResolution({
    required this.value,
    required this.sourceLabel,
  });

  final DateTime value;
  final String sourceLabel;
}

class PreparedMeterPhoto {
  const PreparedMeterPhoto({
    required this.file,
    required this.capturedAt,
    required this.overlayLines,
  });

  final File file;
  final DateTime capturedAt;
  final List<String> overlayLines;
}

class PhotoProofService {
  Future<File> createWorkingCopy(
    File sourceFile, {
    required String idpel,
  }) async {
    final tempDir = await getTemporaryDirectory();
    final extension = _normalizedExtension(sourceFile.path);
    final fileName = 'meter-work-${_safeId(idpel)}-${DateTime.now().millisecondsSinceEpoch}$extension';
    final target = File(p.join(tempDir.path, fileName));
    return sourceFile.copy(target.path);
  }

  Future<PhotoTimestampResolution> resolveCapturedAt(
    File sourceFile, {
    String? originalName,
  }) async {
    final fileName = (originalName?.trim().isNotEmpty == true)
        ? originalName!.trim()
        : p.basename(sourceFile.path);

    final capturedTime = await PhotoMetadataResolver.resolveCapturedTime(
      file: sourceFile,
      filename: fileName,
    );

    return PhotoTimestampResolution(
      value: capturedTime,
      sourceLabel: 'waktu terdeteksi',
    );
  }

  Future<PreparedMeterPhoto> prepareForUpload({
    required File sourceFile,
    required String idpel,
    double? latitude,
    double? longitude,
    DateTime? capturedAt,
  }) async {
    final bytes = await sourceFile.readAsBytes();
    final decoded = img.decodeImage(bytes);
    if (decoded == null) {
      throw Exception('Foto tidak bisa diproses');
    }

    var processed = img.bakeOrientation(decoded);
    if (processed.width > 1600) {
      processed = img.copyResize(processed, width: 1600);
    }

    final captured = capturedAt ?? DateTime.now();
    final overlayLines = <String>[
      'Waktu: ${_formatTimestamp(captured)}',
      if (latitude != null && longitude != null)
        'Lat: ${latitude.toStringAsFixed(6)} | Lng: ${longitude.toStringAsFixed(6)}'
      else
        'Lat/Lng: belum tersedia',
      'IDPEL: $idpel',
    ];

    final font = img.arial24;
    final lineHeight = font.lineHeight + 6;
    final blockHeight = (overlayLines.length * lineHeight) + 20;
    final startY = (processed.height - blockHeight).clamp(0, processed.height - 1);

    img.fillRect(
      processed,
      x1: 0,
      y1: startY,
      x2: processed.width - 1,
      y2: processed.height - 1,
      color: img.ColorRgba8(0, 0, 0, 180),
    );

    var currentY = startY + 10;
    for (final line in overlayLines) {
      img.drawString(
        processed,
        line,
        font: font,
        x: 12,
        y: currentY,
        color: img.ColorRgb8(255, 255, 255),
      );
      currentY += lineHeight;
    }

    final tempDir = await getTemporaryDirectory();
    final uploadName =
        'meter-upload-${_safeId(idpel)}-${captured.millisecondsSinceEpoch}.jpg';
    final uploadFile = File(p.join(tempDir.path, uploadName));
    await uploadFile.writeAsBytes(img.encodeJpg(processed, quality: 82));

    return PreparedMeterPhoto(
      file: uploadFile,
      capturedAt: captured,
      overlayLines: overlayLines,
    );
  }

  Future<void> deleteFile(File? file) async {
    if (file == null) return;
    try {
      if (await file.exists()) {
        await file.delete();
      }
    } catch (_) {}
  }

  String _normalizedExtension(String path) {
    final extension = p.extension(path).trim();
    if (extension.isEmpty) return '.jpg';
    return extension.toLowerCase();
  }

  String _safeId(String value) {
    final normalized = value.trim().replaceAll(RegExp(r'[^A-Za-z0-9_-]'), '_');
    return normalized.isEmpty ? 'meter' : normalized;
  }

  String _formatTimestamp(DateTime value) {
    final local = value.toLocal();
    final year = local.year.toString().padLeft(4, '0');
    final month = local.month.toString().padLeft(2, '0');
    final day = local.day.toString().padLeft(2, '0');
    final hour = local.hour.toString().padLeft(2, '0');
    final minute = local.minute.toString().padLeft(2, '0');
    final second = local.second.toString().padLeft(2, '0');
    return '$year-$month-$day $hour:$minute:$second';
  }

}
