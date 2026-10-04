import 'package:flutter/material.dart';
import '../../models/ocr/customer.dart';
import '../../models/ocr/meter_reading.dart';
import '../../services/ocr_api_service.dart';
import '../../services/ocr_local_db_service.dart';
import '../../theme/app_theme.dart';

class HistoryScreen extends StatefulWidget {
  final Customer customer;

  const HistoryScreen({super.key, required this.customer});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  late Future<List<MeterReading>> _future;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() {
    setState(() {
      _future = _loadHistory();
    });
  }

  Future<List<MeterReading>> _loadHistory() async {
    try {
      return await OcrApiService.instance.getReadingHistory(widget.customer.id);
    } catch (_) {
      return OcrLocalDbService.instance.getByPelanggan(widget.customer.id);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: AppColors.meter,
        foregroundColor: Colors.white,
        title: Text(
          'Riwayat - ${widget.customer.nama}',
          style: const TextStyle(fontSize: 16),
        ),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _load),
        ],
      ),
      body: FutureBuilder<List<MeterReading>>(
        future: _future,
        builder: (context, snap) {
          if (snap.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }

          if (snap.hasError) {
            return Center(child: Text('Gagal memuat: ${snap.error}'));
          }

          final list = snap.data ?? [];
          if (list.isEmpty) {
            return const Center(
              child: Text(
                'Belum ada riwayat bacaan',
                style: TextStyle(color: AppColors.muted),
              ),
            );
          }

          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: list.length,
            itemBuilder: (_, i) {
              final reading = list[i];
              final previous = i < list.length - 1 ? list[i + 1] : null;
              final pemakaian = previous != null
                  ? reading.standAngka - previous.standAngka
                  : null;

              return Card(
                margin: const EdgeInsets.only(bottom: 10),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Row(
                    children: [
                      if (reading.fotoUrl != null)
                        ClipRRect(
                          borderRadius: BorderRadius.circular(6),
                          child: Image.network(
                            reading.fotoUrl!,
                            width: 64,
                            height: 64,
                            fit: BoxFit.cover,
                            errorBuilder: (_, __, ___) => _noPhoto(),
                          ),
                        )
                      else
                        _noPhoto(),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(
                                  reading.bulan,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                    color: AppColors.muted,
                                  ),
                                ),
                                if (!reading.isSynced)
                                  const Chip(
                                    label: Text(
                                      'Offline',
                                      style: TextStyle(fontSize: 10),
                                    ),
                                    backgroundColor: AppColors.proses,
                                    padding: EdgeInsets.zero,
                                    visualDensity: VisualDensity.compact,
                                  ),
                              ],
                            ),
                            Text(
                              'Stand: ${reading.standAngka}',
                              style: const TextStyle(
                                fontSize: 22,
                                fontWeight: FontWeight.bold,
                                color: AppColors.meter,
                                fontFamily: 'monospace',
                                letterSpacing: 2,
                              ),
                            ),
                            if (pemakaian != null)
                              Text(
                                'Pemakaian: $pemakaian m3',
                                style: TextStyle(
                                  color: pemakaian < 0 ? AppColors.danger : AppColors.selesai,
                                  fontSize: 13,
                                ),
                              ),
                            if (reading.catatan != null)
                              Text(
                                'Catatan: ${reading.catatan}',
                                style: const TextStyle(
                                  color: AppColors.muted,
                                  fontSize: 12,
                                ),
                              ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      IconButton(
                        icon: const Icon(Icons.delete_outline, color: AppColors.danger),
                        onPressed: () async {
                          final confirm = await showDialog<bool>(
                            context: context,
                            builder: (ctx) => AlertDialog(
                              title: const Text('Hapus Riwayat'),
                              content: const Text('Apakah Anda yakin ingin menghapus data bacaan ini dari riwayat lokal?'),
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
                            await OcrLocalDbService.instance.deleteReading(reading.id);
                            _load();
                          }
                        },
                      ),
                    ],
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }

  Widget _noPhoto() => Container(
        width: 64,
        height: 64,
        decoration: BoxDecoration(
          color: AppColors.line,
          borderRadius: BorderRadius.circular(6),
        ),
        child: const Icon(Icons.image_not_supported, color: AppColors.muted),
      );
}
