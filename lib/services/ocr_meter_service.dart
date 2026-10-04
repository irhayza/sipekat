import 'dart:io';
import 'dart:ui';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'package:image/image.dart' as img;
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import '../config/app_config.dart';

class MeterOcrResult {
  const MeterOcrResult({
    required this.digits,
    required this.rawText,
    required this.candidates,
    required this.isConfident,
    required this.score,
  });

  const MeterOcrResult.empty()
      : digits = '',
        rawText = '',
        candidates = const <String>[],
        isConfident = false,
        score = 0;

  final String digits;
  final String rawText;
  final List<String> candidates;
  final bool isConfident;
  final double score;

  bool get hasDigits => digits.isNotEmpty;
}

class MeterOcrService {
  MeterOcrService({TextRecognizer? recognizer})
      : _recognizer =
            recognizer ?? TextRecognizer(script: TextRecognitionScript.latin);

  final TextRecognizer _recognizer;

  Future<MeterOcrResult> scanImage(
    File imageFile, {
    int? previousStand,
  }) async {
    final variants = await _buildVariants(imageFile);

    try {
      final results = await Future.wait(
        variants.map((variant) async {
          try {
            final inputImage = InputImage.fromFile(variant.file);
            final recognizedText = await _recognizer.processImage(inputImage);
            return parseRecognizedText(
              recognizedText,
              previousStand: previousStand,
              scoreBias: variant.scoreBias,
            );
          } catch (_) {
            return const MeterOcrResult.empty();
          }
        }),
      );

      var best = const MeterOcrResult.empty();
      for (final result in results) {
        if (!best.hasDigits || result.score > best.score) {
          best = result;
        }
      }

      return best;
    } finally {
      for (final variant in variants) {
        if (!variant.shouldDelete) continue;
        try {
          if (await variant.file.exists()) {
            await variant.file.delete();
          }
        } catch (_) {}
      }
    }
  }

  MeterOcrResult parseRecognizedText(
    RecognizedText recognizedText, {
    int? previousStand,
    double scoreBias = 0,
  }) {
    final candidates = <_MeterCandidate>[];

    double maxLineHeight = 0;
    double maxElementHeight = 0;

    for (final block in recognizedText.blocks) {
      for (final line in block.lines) {
        if (RegExp(r'\d').hasMatch(line.text)) {
          if (line.boundingBox.height > maxLineHeight) {
            maxLineHeight = line.boundingBox.height;
          }
          for (final element in line.elements) {
            if (RegExp(r'\d').hasMatch(element.text)) {
              if (element.boundingBox.height > maxElementHeight) {
                maxElementHeight = element.boundingBox.height;
              }
            }
          }
        }
      }
    }

    _collectCandidates(
      candidates,
      recognizedText.text,
      confidence: 0.35,
      scoreBias: scoreBias - 6,
      visualWeight: 0,
    );

    for (final block in recognizedText.blocks) {
      final blockWeight = _visualWeight(block.boundingBox);
      _collectCandidates(
        candidates,
        block.text,
        confidence: 0.48,
        scoreBias: scoreBias + (blockWeight * 0.35),
        visualWeight: blockWeight,
      );

      for (final line in block.lines) {
        final lineWeight = _visualWeight(line.boundingBox);
        double lineSizeBoost = 0;
        if (maxLineHeight > 0 && RegExp(r'\d').hasMatch(line.text)) {
          final ratio = line.boundingBox.height / maxLineHeight;
          if (ratio >= 0.8) {
            lineSizeBoost = 40.0;
          } else if (ratio >= 0.5) {
            lineSizeBoost = 20.0;
          }
        }

        _collectCandidates(
          candidates,
          line.text,
          confidence: line.confidence ?? 0.66,
          scoreBias: scoreBias + (lineWeight * 0.65) + lineSizeBoost,
          visualWeight: lineWeight,
        );

        for (final element in line.elements) {
          final elementWeight = _visualWeight(element.boundingBox);
          double elementSizeBoost = 0;
          if (maxElementHeight > 0 && RegExp(r'\d').hasMatch(element.text)) {
            final ratio = element.boundingBox.height / maxElementHeight;
            if (ratio >= 0.8) {
              elementSizeBoost = 40.0;
            } else if (ratio >= 0.5) {
              elementSizeBoost = 20.0;
            }
          }

          _collectCandidates(
            candidates,
            element.text,
            confidence: element.confidence ?? 0.82,
            scoreBias: scoreBias + elementWeight + elementSizeBoost,
            visualWeight: elementWeight,
          );
        }
      }
    }

    return _buildResult(
      candidates,
      rawText: recognizedText.text,
      previousStand: previousStand,
    );
  }

  static MeterOcrResult parseRawText(
    String rawText, {
    int? previousStand,
  }) {
    final candidates = <_MeterCandidate>[];
    _collectCandidates(
      candidates,
      rawText,
      confidence: 0.6,
      scoreBias: 0,
      visualWeight: 0,
    );
    return _buildResult(
      candidates,
      rawText: rawText,
      previousStand: previousStand,
    );
  }

  Future<void> close() => _recognizer.close();

  static MeterOcrResult _buildResult(
    List<_MeterCandidate> candidates, {
    required String rawText,
    int? previousStand,
  }) {
    if (candidates.isEmpty) {
      return MeterOcrResult(
        digits: '',
        rawText: rawText.trim(),
        candidates: const <String>[],
        isConfident: false,
        score: 0,
      );
    }

    final byDigits = <String, _MeterCandidate>{};
    for (final candidate in candidates) {
      final existing = byDigits[candidate.digits];
      if (existing == null ||
          candidate.rank(previousStand) > existing.rank(previousStand)) {
        byDigits[candidate.digits] = candidate;
      }
    }

    final ranked = byDigits.values.toList()
      ..sort(
        (left, right) => right
            .rank(previousStand)
            .compareTo(left.rank(previousStand)),
      );

    final best = ranked.first;
    final parsedValue = int.tryParse(best.digits);
    final matchesHistory =
        previousStand == null || (parsedValue != null && parsedValue >= previousStand);
    final confident = best.confidence >= 0.58 &&
        best.digits.length >= AppConfig.meterDigitCount - 1 &&
        best.digits.length <= AppConfig.meterDigitCount &&
        matchesHistory;

    return MeterOcrResult(
      digits: best.digits,
      rawText: rawText.trim(),
      candidates: ranked.map((candidate) => candidate.digits).take(5).toList(),
      isConfident: confident,
      score: best.rank(previousStand),
    );
  }

  static void _collectCandidates(
    List<_MeterCandidate> target,
    String source, {
    required double confidence,
    required double scoreBias,
    required double visualWeight,
  }) {
    final normalized = _normalizeMeterText(source);
    if (normalized.isEmpty) return;

    final parts = normalized
        .split(RegExp(r'\s+'))
        .where((part) => part.isNotEmpty)
        .toList();

    final sourceDigits = source.replaceAll(RegExp(r'[^0-9]'), '');

    void addCandidate(String digits, {required int segmentCount, double extraBias = 0}) {
      if (!_isValidLength(digits)) {
        return;
      }

      final lcs = _lcsLength(digits, sourceDigits);
      final substitutedCount = digits.length - lcs;
      final purityPenalty = substitutedCount * 15.0;

      target.add(
        _MeterCandidate(
          digits: digits,
          confidence: confidence,
          visualWeight: visualWeight,
          scoreBias: scoreBias + extraBias - purityPenalty,
          segmentCount: segmentCount,
        ),
      );
    }

    for (final part in parts) {
      addCandidate(part, segmentCount: 1);
    }

    final joined = parts.join();
    if (parts.length >= 2) {
      addCandidate(joined, segmentCount: parts.length, extraBias: 10);
    }

    if (parts.length >= AppConfig.meterDigitCount - 1) {
      for (var start = 0; start < parts.length; start++) {
        for (final length in [AppConfig.meterDigitCount - 1, AppConfig.meterDigitCount]) {
          final end = start + length;
          if (end > parts.length) continue;
          final slice = parts.sublist(start, end);
          final digits = slice.join();
          addCandidate(
            digits,
            segmentCount: slice.length,
            extraBias: 18 - (start * 2),
          );
        }
      }
    }
  }

  static bool _isValidLength(String value) {
    return value.length >= 4 && value.length <= 6;
  }

  static int _lcsLength(String s1, String s2) {
    final m = s1.length;
    final n = s2.length;
    if (m == 0 || n == 0) return 0;
    final dp = List.generate(m + 1, (_) => List<int>.filled(n + 1, 0));
    for (var i = 1; i <= m; i++) {
      for (var j = 1; j <= n; j++) {
        if (s1.codeUnitAt(i - 1) == s2.codeUnitAt(j - 1)) {
          dp[i][j] = dp[i - 1][j - 1] + 1;
        } else {
          dp[i][j] = dp[i - 1][j] > dp[i][j - 1] ? dp[i - 1][j] : dp[i][j - 1];
        }
      }
    }
    return dp[m][n];
  }

  static String _normalizeMeterText(String value) {
    if (value.trim().isEmpty) return '';

    const substitutions = <String, String>{
      'O': '0',
      'Q': '0',
      'D': '0',
      'I': '1',
      'L': '1',
      '|': '1',
      'Z': '2',
      'S': '5',
      'G': '6',
      'B': '8',
    };

    final buffer = StringBuffer();
    for (final rune in value.toUpperCase().runes) {
      final character = String.fromCharCode(rune);
      if (RegExp(r'\d').hasMatch(character)) {
        buffer.write(character);
      } else if (substitutions.containsKey(character)) {
        buffer.write(substitutions[character]);
      } else {
        buffer.write(' ');
      }
    }

    return buffer.toString().replaceAll(RegExp(r'\s+'), ' ').trim();
  }

  double _visualWeight(Rect rect) {
    final heightScore = (rect.height / 12).clamp(0, 22).toDouble();
    final widthScore = (rect.width / 30).clamp(0, 12).toDouble();
    return heightScore + widthScore;
  }

  Future<List<_OcrImageVariant>> _buildVariants(File sourceFile) async {
    final variants = <_OcrImageVariant>[
      _OcrImageVariant(
        file: sourceFile,
        scoreBias: 0,
        shouldDelete: false,
      ),
    ];

    try {
      final bytes = await sourceFile.readAsBytes();
      final decoded = img.decodeImage(bytes);
      if (decoded == null) {
        return variants;
      }

      var base = img.bakeOrientation(decoded);
      if (base.width > 1800) {
        base = img.copyResize(base, width: 1800);
      }

      final tempDir = await getTemporaryDirectory();
      final prefix = 'ocr-${DateTime.now().microsecondsSinceEpoch}';

      Future<void> addVariant(
        img.Image image,
        String name,
        double scoreBias,
      ) async {
        final file = File(p.join(tempDir.path, '$prefix-$name.jpg'));
        await file.writeAsBytes(img.encodeJpg(image, quality: 92));
        variants.add(
          _OcrImageVariant(
            file: file,
            scoreBias: scoreBias,
            shouldDelete: true,
          ),
        );
      }

      final meterWindow = _cropByFraction(
        base,
        left: 0.12,
        top: 0.18,
        width: 0.76,
        height: 0.60,
      );
      final upscaledWindow = _upscaleForOcr(meterWindow);
      await addVariant(upscaledWindow, 'window', 18);

      final contrastWindow = _cloneImage(upscaledWindow);
      img.grayscale(contrastWindow);
      img.adjustColor(
        contrastWindow,
        contrast: 1.65,
        brightness: 1.12,
        saturation: 0,
      );
      await addVariant(contrastWindow, 'window-contrast', 22);

      final thresholdWindow = _cloneImage(upscaledWindow);
      img.grayscale(thresholdWindow);
      img.contrast(thresholdWindow, contrast: 180);
      img.luminanceThreshold(thresholdWindow, threshold: 0.42);
      await addVariant(thresholdWindow, 'window-threshold-42', 25);

      final invThresholdWindow = _cloneImage(upscaledWindow);
      img.grayscale(invThresholdWindow);
      img.invert(invThresholdWindow);
      img.contrast(invThresholdWindow, contrast: 180);
      img.luminanceThreshold(invThresholdWindow, threshold: 0.58);
      await addVariant(invThresholdWindow, 'window-inv-threshold-58', 28);

      final invThresholdWindow2 = _cloneImage(upscaledWindow);
      img.grayscale(invThresholdWindow2);
      img.invert(invThresholdWindow2);
      img.contrast(invThresholdWindow2, contrast: 180);
      img.luminanceThreshold(invThresholdWindow2, threshold: 0.70);
      await addVariant(invThresholdWindow2, 'window-inv-threshold-70', 30);

      final blackDigits = _cropByFraction(
        base,
        left: 0.15,
        top: 0.28,
        width: 0.60,
        height: 0.34,
      );
      final upscaledDigits = _upscaleForOcr(blackDigits);
      await addVariant(upscaledDigits, 'digits', 32);

      final contrastDigits = _cloneImage(upscaledDigits);
      img.grayscale(contrastDigits);
      img.adjustColor(
        contrastDigits,
        contrast: 1.65,
        brightness: 1.12,
        saturation: 0,
      );
      await addVariant(contrastDigits, 'digits-contrast', 38);

      final thresholdDigits = _cloneImage(upscaledDigits);
      img.grayscale(thresholdDigits);
      img.contrast(thresholdDigits, contrast: 180);
      img.luminanceThreshold(thresholdDigits, threshold: 0.42);
      await addVariant(thresholdDigits, 'digits-threshold-42', 42);

      final invThresholdDigits = _cloneImage(upscaledDigits);
      img.grayscale(invThresholdDigits);
      img.invert(invThresholdDigits);
      img.contrast(invThresholdDigits, contrast: 180);
      img.luminanceThreshold(invThresholdDigits, threshold: 0.58);
      await addVariant(invThresholdDigits, 'digits-inv-threshold-58', 46);

      final invThresholdDigits2 = _cloneImage(upscaledDigits);
      img.grayscale(invThresholdDigits2);
      img.invert(invThresholdDigits2);
      img.contrast(invThresholdDigits2, contrast: 180);
      img.luminanceThreshold(invThresholdDigits2, threshold: 0.70);
      await addVariant(invThresholdDigits2, 'digits-inv-threshold-70', 48);

    } catch (_) {}

    return variants;
  }

  img.Image _cropByFraction(
    img.Image source, {
    required double left,
    required double top,
    required double width,
    required double height,
  }) {
    final x = (source.width * left).round().clamp(0, source.width - 1);
    final y = (source.height * top).round().clamp(0, source.height - 1);
    final w = (source.width * width).round().clamp(1, source.width - x);
    final h = (source.height * height).round().clamp(1, source.height - y);
    return img.copyCrop(source, x: x, y: y, width: w, height: h);
  }

  img.Image _upscaleForOcr(img.Image source) {
    if (source.width >= 1200) {
      return source;
    }
    return img.copyResize(source, width: 1200);
  }

  img.Image _cloneImage(img.Image source) {
    return img.copyCrop(
      source,
      x: 0,
      y: 0,
      width: source.width,
      height: source.height,
    );
  }
}

class _MeterCandidate {
  const _MeterCandidate({
    required this.digits,
    required this.confidence,
    required this.visualWeight,
    required this.scoreBias,
    required this.segmentCount,
  });

  final String digits;
  final double confidence;
  final double visualWeight;
  final double scoreBias;
  final int segmentCount;

  double rank(int? previousStand) {
    var score = confidence * 100;
    score += visualWeight;
    score += scoreBias;

    if (digits.length == AppConfig.meterDigitCount) {
      score += 50;
    } else if (digits.length == AppConfig.meterDigitCount - 1) {
      score += 45;
    } else if (digits.length == AppConfig.meterDigitCount + 1) {
      score += 10;
    } else {
      score -= 30;
    }

    if (segmentCount >= AppConfig.meterDigitCount - 1 && segmentCount <= AppConfig.meterDigitCount) {
      score += 18;
    } else if (segmentCount >= 2) {
      score += 8;
    }

    if (digits.startsWith('0') && digits.length == AppConfig.meterDigitCount) {
      score += 10;
    }

    final value = int.tryParse(digits);
    if (value == null) {
      return score - 120;
    }

    if (previousStand != null) {
      if (value >= previousStand) {
        final difference = value - previousStand;
        score += 38;
        if (difference <= 250) {
          score += 24;
        } else if (difference <= 1500) {
          score += 16;
        } else if (difference <= AppConfig.meterMaxPlausibleUsage) {
          score += 10;
        } else {
          score -= (difference / 1500).clamp(0, 30);
        }
      } else {
        score -= 40 + ((previousStand - value) / 100).clamp(0, 60);
      }

      if (digits.length == AppConfig.meterDigitCount + 1 && previousStand < 10000) {
        score -= 22;
      }
    }

    return score;
  }
}

class _OcrImageVariant {
  const _OcrImageVariant({
    required this.file,
    required this.scoreBias,
    required this.shouldDelete,
  });

  final File file;
  final double scoreBias;
  final bool shouldDelete;
}
