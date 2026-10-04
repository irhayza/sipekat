import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';

class PesanTab extends StatelessWidget {
  const PesanTab({super.key});

  @override
  Widget build(BuildContext context) {
    final messages = [
      {
        'title': 'Penugasan Baru Tersedia',
        'desc': 'Data penugasan sektor baru telah dimuat. Silakan periksa daftar penugasan Anda.',
        'time': 'Baru saja',
        'icon': Icons.assignment_turned_in_rounded,
      },
      {
        'title': 'Sinkronisasi Berhasil',
        'desc': 'Data pembacaan stand meter berhasil dikirim ke server google sheet.',
        'time': '1 jam yang lalu',
        'icon': Icons.check_circle_rounded,
      },
      {
        'title': 'Tips Membaca OCR',
        'desc': 'Posisikan kamera sejajar dengan angka meter dan pastikan cahaya cukup untuk hasil maksimal.',
        'time': 'Kemarin',
        'icon': Icons.tips_and_updates_rounded,
      },
    ];

    return Scaffold(
      backgroundColor: AppColors.canvas,
      appBar: AppBar(
        title: const Text('Pesan & Notifikasi'),
        backgroundColor: AppColors.brandDark,
        foregroundColor: Colors.white,
        automaticallyImplyLeading: false,
      ),
      body: ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: messages.length,
        separatorBuilder: (_, __) => const SizedBox(height: 12),
        itemBuilder: (context, index) {
          final m = messages[index];
          return Card(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: ListTile(
              contentPadding: const EdgeInsets.all(16),
              leading: CircleAvatar(
                backgroundColor: AppColors.meter.withValues(alpha: 0.1),
                child: Icon(m['icon'] as IconData, color: AppColors.meter),
              ),
              title: Text(
                m['title'] as String,
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
              ),
              subtitle: Padding(
                padding: const EdgeInsets.only(top: 8.0),
                child: Text(
                  m['desc'] as String,
                  style: const TextStyle(fontSize: 12, height: 1.4),
                ),
              ),
              trailing: Text(
                m['time'] as String,
                style: const TextStyle(color: AppColors.muted, fontSize: 10),
              ),
            ),
          );
        },
      ),
    );
  }
}
