import 'dart:io';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/foundation.dart';
import 'ocr_api_service.dart';
import 'ocr_local_db_service.dart';

class OcrSyncService {
  OcrSyncService._();
  static final OcrSyncService instance = OcrSyncService._();

  bool _isSyncing = false;

  bool _hasConnection(List<ConnectivityResult> results) {
    return results.any((result) => result != ConnectivityResult.none);
  }

  Future<int> syncAll() async {
    if (_isSyncing) return 0;

    final connectivity = await Connectivity().checkConnectivity();
    if (!_hasConnection(connectivity)) return 0;

    _isSyncing = true;
    int synced = 0;

    try {
      final unsynced = await OcrLocalDbService.instance.getUnsynced();
      if (unsynced.isEmpty) return 0;

      if (OcrApiService.instance.canSyncPendingReadings) {
        for (final reading in unsynced) {
          try {
            final syncedReading = await OcrApiService.instance.syncPendingReading(reading);
            await OcrLocalDbService.instance.saveReading(syncedReading);

            final oldPhotoPath = reading.fotoPath?.trim();
            if (oldPhotoPath != null && oldPhotoPath.isNotEmpty) {
              final tempFile = File(oldPhotoPath);
              if (await tempFile.exists()) {
                await tempFile.delete();
              }
            }

            synced++;
          } catch (e) {
            if (kDebugMode) debugPrint('OcrSyncService: gagal sync reading IDPEL ${reading.pelangganId}: $e');
          }
        }
        return synced;
      }

      final results = await OcrApiService.instance.syncOfflineReadings(unsynced);
      for (final r in results) {
        if (r['status'] == 'ok') {
          await OcrLocalDbService.instance.markSynced(r['id'].toString());
          synced++;
        } else {
          if (kDebugMode) debugPrint('OcrSyncService: syncOfflineReadings gagal untuk id ${r['id']}: ${r['error']}');
        }
      }
    } catch (e) {
      if (kDebugMode) debugPrint('OcrSyncService: syncAll error: $e');
    } finally {
      _isSyncing = false;
    }

    return synced;
  }

  Stream<int> get autoSync {
    return Connectivity().onConnectivityChanged.where(_hasConnection).asyncMap((_) => syncAll());
  }
}
