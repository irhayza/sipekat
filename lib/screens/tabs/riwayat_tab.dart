import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../services/ocr_local_db_service.dart';
import '../../models/ocr/meter_reading.dart';
import '../../services/kunjungan_local_cache_service.dart';

class RiwayatTab extends StatelessWidget {
  const RiwayatTab({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 4,
      child: Scaffold(
        backgroundColor: AppColors.canvas,
        appBar: AppBar(
          title: const Text('Riwayat Pekerjaan'),
          backgroundColor: AppColors.brandDark,
          foregroundColor: Colors.white,
          automaticallyImplyLeading: false,
          bottom: const TabBar(
            isScrollable: true,
            labelColor: Colors.white,
            unselectedLabelColor: Colors.white70,
            indicatorColor: AppColors.selesai,
            tabs: [
              Tab(text: 'Catat Meter'),
              Tab(text: 'Pengaduan'),
              Tab(text: 'Kunjungan'),
              Tab(text: 'Perbaikan'),
            ],
          ),
        ),
        body: TabBarView(
          children: [
            _buildOcrHistory(),
            _buildPengaduanHistory(),
            _buildKunjunganHistory(),
            _buildPerbaikanHistory(),
          ],
        ),
      ),
    );
  }

  Widget _buildOcrHistory() {
    return FutureBuilder<List<MeterReading>>(
      future: OcrLocalDbService.instance.getAllReadings(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return Center(child: Text('Gagal memuat riwayat: ${snapshot.error}'));
        }
        final list = snapshot.data ?? [];
        if (list.isEmpty) {
          return const _EmptyHistory(title: 'Belum ada riwayat catat meter.');
        }
        return ListView.separated(
          padding: const EdgeInsets.all(16),
          itemCount: list.length,
          separatorBuilder: (_, __) => const SizedBox(height: 12),
          itemBuilder: (context, index) {
            final r = list[index];
            return Card(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              child: ListTile(
                contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                leading: const CircleAvatar(
                  backgroundColor: AppColors.meter,
                  child: Icon(Icons.receipt_long, color: Colors.white),
                ),
                title: Text(
                  r.pelangganId,
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
                subtitle: Text(
                  'Stand: ${r.standAngka}\n${r.createdAt.toString().substring(0, 16)}',
                  style: const TextStyle(height: 1.4, fontSize: 12),
                ),
                trailing: _StatusBadge(isSynced: r.isSynced),
              ),
            );
          },
        );
      },
    );
  }

  Widget _buildKunjunganHistory() {
    return FutureBuilder<List<Map<String, dynamic>>>(
      future: KunjunganLocalCacheService.instance.getHistorySubmissions(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        final list = snapshot.data ?? [];
        if (list.isEmpty) {
          return const _EmptyHistory(title: 'Belum ada riwayat kunjungan.');
        }
        return ListView.separated(
          padding: const EdgeInsets.all(16),
          itemCount: list.length,
          separatorBuilder: (_, __) => const SizedBox(height: 12),
          itemBuilder: (context, index) {
            final r = list[index];
            final status = r['status'] ?? 'pending';
            final isSynced = status == 'synced';
            return Card(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              child: ListTile(
                contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                leading: const CircleAvatar(
                  backgroundColor: AppColors.kunjungan,
                  child: Icon(Icons.transfer_within_a_station_rounded, color: Colors.white),
                ),
                title: Text(
                  r['idpel'] ?? '-',
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
                subtitle: Text(
                  'Catatan: ${r['keterangan'] ?? '-'}\n${(r['timestamp'] ?? '').toString().split('.').first}',
                  style: const TextStyle(height: 1.4, fontSize: 12),
                ),
                trailing: _StatusBadge(isSynced: isSynced),
              ),
            );
          },
        );
      },
    );
  }

  Widget _buildPengaduanHistory() {
    // TODO: Implement actual database logic for Pengaduan history
    return const _EmptyHistory(title: 'Belum ada riwayat pengaduan.');
  }

  Widget _buildPerbaikanHistory() {
    // TODO: Implement actual database logic for Perbaikan history
    return const _EmptyHistory(title: 'Belum ada riwayat laporan perbaikan.');
  }
}

class _StatusBadge extends StatelessWidget {
  final bool isSynced;
  const _StatusBadge({required this.isSynced});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: isSynced ? Colors.green.withValues(alpha: 0.1) : Colors.orange.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        isSynced ? 'Terkirim' : 'Pending',
        style: TextStyle(
          fontSize: 10,
          fontWeight: FontWeight.bold,
          color: isSynced ? Colors.green : Colors.orange,
        ),
      ),
    );
  }
}

class _EmptyHistory extends StatelessWidget {
  final String title;
  const _EmptyHistory({required this.title});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.history_rounded, size: 64, color: AppColors.muted),
          const SizedBox(height: 12),
          Text(title, style: const TextStyle(color: AppColors.muted)),
        ],
      ),
    );
  }
}
