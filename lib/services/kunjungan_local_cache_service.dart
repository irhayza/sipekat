import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'kunjungan_api_service.dart';

class KunjunganLocalCacheService {
  KunjunganLocalCacheService._();
  static final KunjunganLocalCacheService instance = KunjunganLocalCacheService._();

  static const String _keyVisitList = 'cached_visit_list';
  static const String _keyReopenList = 'cached_reopen_list';
  static const String _keyPendingSubmissions = 'pending_submissions';

  Future<void> saveList(String key, List<VisitCustomer> list) async {
    final prefs = await SharedPreferences.getInstance();
    final jsonList = list.map((c) => c.toJson()).toList();
    await prefs.setString(key, jsonEncode(jsonList));
  }

  Future<void> saveVisitList(List<VisitCustomer> list) => saveList(_keyVisitList, list);
  Future<void> saveReopenList(List<VisitCustomer> list) => saveList(_keyReopenList, list);

  Future<List<VisitCustomer>> getList(String key) async {
    final prefs = await SharedPreferences.getInstance();
    final jsonStr = prefs.getString(key);
    if (jsonStr == null || jsonStr.isEmpty) return [];

    try {
      final List<dynamic> decoded = jsonDecode(jsonStr);
      return decoded.map((item) => VisitCustomer.fromJson(Map<String, dynamic>.from(item as Map))).toList();
    } catch (e) {
      return [];
    }
  }

  Future<List<VisitCustomer>> getCachedVisitList() => getList(_keyVisitList);
  Future<List<VisitCustomer>> getCachedReopenList() => getList(_keyReopenList);

  Future<void> removeCustomerFromCache(String idpel) async {
    final visits = await getCachedVisitList();
    final updatedVisits = visits.where((c) => c.idpel != idpel).toList();
    await saveVisitList(updatedVisits);

    final reopens = await getCachedReopenList();
    final updatedReopens = reopens.where((c) => c.idpel != idpel).toList();
    await saveReopenList(updatedReopens);
  }

  Future<void> savePendingSubmission(Map<String, dynamic> submission) async {
    final prefs = await SharedPreferences.getInstance();
    final list = await getPendingSubmissions();
    
    final idpel = submission['idpel']?.toString();
    final filtered = list.where((item) => item['idpel']?.toString() != idpel).toList();
    filtered.add(submission);

    await prefs.setString(_keyPendingSubmissions, jsonEncode(filtered));
  }

  Future<List<Map<String, dynamic>>> getPendingSubmissions() async {
    final prefs = await SharedPreferences.getInstance();
    final jsonStr = prefs.getString(_keyPendingSubmissions);
    if (jsonStr == null || jsonStr.isEmpty) return [];

    try {
      final List<dynamic> decoded = jsonDecode(jsonStr);
      return decoded.map((item) => Map<String, dynamic>.from(item as Map)).toList();
    } catch (e) {
      return [];
    }
  }

  Future<void> removePendingSubmission(String idpel) async {
    final prefs = await SharedPreferences.getInstance();
    final list = await getPendingSubmissions();
    final updated = list.where((item) => item['idpel']?.toString() != idpel).toList();
    await prefs.setString(_keyPendingSubmissions, jsonEncode(updated));
  }

  Future<Map<String, dynamic>> syncPendingSubmissions() async {
    final pending = await getPendingSubmissions();
    if (pending.isEmpty) {
      return {'status': 'success', 'message': 'Tidak ada data pending yang perlu dikirim.'};
    }

    int successCount = 0;
    int failCount = 0;
    String lastError = '';

    for (final item in pending) {
      try {
        final idpel = item['idpel']?.toString() ?? '';
        final actionType = item['actionType']?.toString() ?? '';
        final fotoSebBase64 = item['fotoSebBase64']?.toString() ?? '';
        final fotoSesBase64 = item['fotoSesBase64']?.toString() ?? '';
        final nomgrtBru = item['nomgrtBru']?.toString();
        final stMgrt = item['stMgrt']?.toString();
        final metadata = item['metadata']?.toString() ?? '';
        final lat = item['lat']?.toString();
        final lng = item['lng']?.toString();
        final timeBefore = item['timeBefore']?.toString();
        final timeAfter = item['timeAfter']?.toString();

        final res = await KunjunganApiService.instance.submitAction(
          idpel: idpel,
          actionType: actionType,
          fotoSebBase64: fotoSebBase64,
          fotoSesBase64: fotoSesBase64,
          nomgrtBru: nomgrtBru,
          stMgrt: stMgrt,
          metadata: metadata,
          lat: lat,
          lng: lng,
          timeBefore: timeBefore,
          timeAfter: timeAfter,
        );

        if (res['status'] == 'success' || res['status'] == 'ok') {
          successCount++;
          await removePendingSubmission(idpel);
          await removeCustomerFromCache(idpel);
        } else {
          failCount++;
          lastError = res['message']?.toString() ?? 'Gagal menyimpan ke server';
        }
      } catch (e) {
        failCount++;
        lastError = e.toString();
      }
    }

    if (failCount == 0) {
      return {
        'status': 'success',
        'message': 'Berhasil mengirim $successCount data kunjungan ke server.'
      };
    } else {
      return {
        'status': 'partial',
        'message': 'Berhasil mengirim $successCount data. $failCount data gagal dikirim. Error terakhir: $lastError'
      };
    }
  }

  static const String _keyHistorySubmissions = 'history_submissions';

  Future<void> saveHistorySubmission(Map<String, dynamic> submission) async {
    final prefs = await SharedPreferences.getInstance();
    final list = await getHistorySubmissions();
    
    final idpel = submission['idpel']?.toString();
    final filtered = list.where((item) => item['idpel']?.toString() != idpel).toList();
    
    if (!submission.containsKey('timestamp')) {
      submission['timestamp'] = DateTime.now().toIso8601String();
    }
    
    filtered.add(submission);
    await prefs.setString(_keyHistorySubmissions, jsonEncode(filtered));
  }

  Future<List<Map<String, dynamic>>> getHistorySubmissions() async {
    final prefs = await SharedPreferences.getInstance();
    final jsonStr = prefs.getString(_keyHistorySubmissions);
    if (jsonStr == null || jsonStr.isEmpty) return [];

    try {
      final List<dynamic> decoded = jsonDecode(jsonStr);
      final list = decoded.map((item) => Map<String, dynamic>.from(item as Map)).toList();
      list.sort((a, b) {
        final tA = a['timestamp']?.toString() ?? '';
        final tB = b['timestamp']?.toString() ?? '';
        return tB.compareTo(tA);
      });
      return list;
    } catch (e) {
      return [];
    }
  }

  Future<void> removeHistorySubmission(String idpel) async {
    final prefs = await SharedPreferences.getInstance();
    final list = await getHistorySubmissions();
    final updated = list.where((item) => item['idpel']?.toString() != idpel).toList();
    await prefs.setString(_keyHistorySubmissions, jsonEncode(updated));
  }

  Future<void> updateHistorySubmissionStatus(String idpel, String status) async {
    final list = await getHistorySubmissions();
    for (final item in list) {
      if (item['idpel']?.toString() == idpel) {
        item['status'] = status;
        await saveHistorySubmission(item);
        break;
      }
    }
  }
}
