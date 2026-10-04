import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../services/kunjungan_local_cache_service.dart';

class PendingSyncScreen extends StatefulWidget {
  const PendingSyncScreen({super.key});

  @override
  State<PendingSyncScreen> createState() => _PendingSyncScreenState();
}

class _PendingSyncScreenState extends State<PendingSyncScreen> {
  List<Map<String, dynamic>> _pendingList = [];
  bool _loading = true;
  bool _syncing = false;

  @override
  void initState() {
    super.initState();
    _loadPendingList();
  }

  Future<void> _loadPendingList() async {
    setState(() => _loading = true);
    final list = await KunjunganLocalCacheService.instance.getPendingSubmissions();
    setState(() {
      _pendingList = list;
      _loading = false;
    });
  }

  Future<void> _handleDelete(String idpel, String actionType) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text('Hapus Laporan?'),
        content: Text('Apakah Anda yakin ingin menghapus laporan pending untuk pelanggan $idpel ($actionType) dari memori lokal?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Batal'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Hapus', style: TextStyle(color: AppColors.danger)),
          ),
        ],
      ),
    );

    if (confirm == true) {
      await KunjunganLocalCacheService.instance.removePendingSubmission(idpel);
      _showSnackbar('Laporan pending berhasil dihapus.', isError: false);
      _loadPendingList();
    }
  }

  Future<void> _handleSyncAll() async {
    if (_pendingList.isEmpty) return;

    setState(() => _syncing = true);

    try {
      final res = await KunjunganLocalCacheService.instance.syncPendingSubmissions();
      setState(() => _syncing = false);

      if (mounted) {
        showDialog<void>(
          context: context,
          barrierDismissible: false,
          builder: (ctx) => AlertDialog(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            title: const Text('Sinkronisasi Selesai'),
            content: Text(res['message']?.toString() ?? 'Proses sinkronisasi selesai.'),
            actions: [
              TextButton(
                onPressed: () {
                  Navigator.pop(ctx);
                  _loadPendingList();
                },
                child: const Text('OK'),
              ),
            ],
          ),
        );
      }
    } catch (e) {
      setState(() => _syncing = false);
      _showSnackbar('Sinkronisasi gagal: $e', isError: true);
    }
  }

  void _showSnackbar(String message, {bool isError = false}) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError ? AppColors.danger : AppColors.selesai,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.canvas,
      appBar: AppBar(
        title: const Text(
          'Riwayat Pending (Offline)',
          style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadPendingList,
            tooltip: 'Segarkan',
          ),
        ],
      ),
      body: _syncing
          ? const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(color: AppColors.kunjungan),
                  SizedBox(height: 20),
                  Text(
                    'Mengirimkan data ke Google Sheets...',
                    style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppColors.muted),
                  ),
                ],
              ),
            )
          : _loading
              ? const Center(child: CircularProgressIndicator())
              : _pendingList.isEmpty
                  ? const Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.cloud_done_rounded, size: 64, color: AppColors.selesai),
                          SizedBox(height: 16),
                          Text(
                            'Semua data telah disinkronkan!',
                            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.muted),
                          ),
                          SizedBox(height: 4),
                          Text(
                            'Tidak ada laporan tertunda di memori lokal.',
                            style: TextStyle(fontSize: 12, color: AppColors.muted),
                          ),
                        ],
                      ),
                    )
                  : Column(
                      children: [
                        Expanded(
                          child: ListView.builder(
                            padding: const EdgeInsets.all(12),
                            itemCount: _pendingList.length,
                            itemBuilder: (ctx, index) {
                              final item = _pendingList[index];
                              final idpel = item['idpel']?.toString() ?? '';
                              final actionType = (item['actionType']?.toString() ?? '').toUpperCase();
                              final metadata = item['metadata']?.toString() ?? '';

                              return Card(
                                margin: const EdgeInsets.only(bottom: 12),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                                child: Padding(
                                  padding: const EdgeInsets.all(16),
                                  child: Row(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            Row(
                                              children: [
                                                Text(
                                                  idpel,
                                                  style: const TextStyle(
                                                    fontWeight: FontWeight.bold,
                                                    fontSize: 16,
                                                    color: AppColors.muted,
                                                  ),
                                                ),
                                                const SizedBox(width: 8),
                                                Container(
                                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                                  decoration: BoxDecoration(
                                                    color: actionType.contains('BUKA') || actionType.contains('PASANG')
                                                        ? AppColors.selesaiBg
                                                        : AppColors.dangerBg,
                                                    borderRadius: BorderRadius.circular(8),
                                                    border: Border.all(
                                                      color: actionType.contains('BUKA') || actionType.contains('PASANG')
                                                          ? AppColors.selesai
                                                          : AppColors.danger,
                                                    ),
                                                  ),
                                                  child: Text(
                                                    actionType,
                                                    style: TextStyle(
                                                      fontSize: 10,
                                                      fontWeight: FontWeight.bold,
                                                      color: actionType.contains('BUKA') || actionType.contains('PASANG')
                                                          ? AppColors.selesai
                                                          : AppColors.danger,
                                                    ),
                                                  ),
                                                ),
                                              ],
                                            ),
                                            const SizedBox(height: 8),
                                            Text(
                                              metadata,
                                              style: const TextStyle(fontSize: 12, color: AppColors.muted, height: 1.3),
                                            ),
                                          ],
                                        ),
                                      ),
                                      const SizedBox(width: 8),
                                      IconButton(
                                        icon: const Icon(Icons.delete_outline, color: AppColors.danger),
                                        onPressed: () => _handleDelete(idpel, actionType),
                                        tooltip: 'Hapus Laporan',
                                      ),
                                    ],
                                  ),
                                ),
                              );
                            },
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.all(16),
                          decoration: const BoxDecoration(
                            color: Colors.white,
                            border: Border(top: BorderSide(color: Colors.black12)),
                          ),
                          child: SafeArea(
                            child: SizedBox(
                              width: double.infinity,
                              height: 50,
                              child: ElevatedButton.icon(
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: AppColors.kunjungan,
                                  foregroundColor: Colors.white,
                                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                                ),
                                icon: const Icon(Icons.cloud_upload_rounded),
                                label: const Text(
                                  'Sinkronkan Semua Sekarang',
                                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                                ),
                                onPressed: _handleSyncAll,
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
    );
  }
}