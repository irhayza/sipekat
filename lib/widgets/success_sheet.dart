import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import 'status_badge.dart';

/// Lembar konfirmasi sukses terpadu — dipakai di semua modul (Perbaikan,
/// Pengaduan, Kunjungan) agar tampilan "laporan terkirim" konsisten.
///
/// [status] kosongkan ('') untuk menyembunyikan badge status (mis. pada
/// modul Pengaduan yang tidak punya konsep status SELESAI/PROSES/OPEN).
/// [onContinue] & [continueLabel] opsional — bila null, tombol kedua
/// disembunyikan dan hanya tombol [finishLabel] yang tampil.
Future<void> showSuccessSheet({
  required BuildContext context,
  required String ticket,
  String status = '',
  String title = 'Laporan tersimpan',
  String message = 'Data penanganan sudah dicatat di server.',
  String ticketLabel = 'TIKET',
  required VoidCallback onFinish,
  String finishLabel = 'Selesai',
  VoidCallback? onContinue,
  String continueLabel = 'Lanjut Update',
}) {
  return showModalBottomSheet<void>(
    context: context,
    isDismissible: false,
    enableDrag: false,
    backgroundColor: Colors.transparent,
    builder: (ctx) {
      return Container(
        padding: EdgeInsets.fromLTRB(24, 28, 24, 24 + MediaQuery.of(ctx).viewInsets.bottom),
        decoration: const BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: const BoxDecoration(color: AppColors.selesaiBg, shape: BoxShape.circle),
              child: const Icon(Icons.check_rounded, color: AppColors.selesai, size: 26),
            ),
            const SizedBox(height: 14),
            Text(
              title,
              style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700, color: AppColors.ink),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 4),
            Text(
              message,
              style: const TextStyle(fontSize: 13, color: AppColors.muted),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 18),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppColors.canvas,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: AppColors.line),
              ),
              child: Column(
                children: [
                  Text(
                    ticketLabel,
                    style: const TextStyle(
                      fontSize: 10,
                      fontWeight: FontWeight.w700,
                      color: AppColors.muted,
                      letterSpacing: 0.6,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    ticket,
                    style: const TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                      fontFamily: 'monospace',
                      color: AppColors.ink,
                    ),
                  ),
                  if (status.isNotEmpty) ...[
                    const SizedBox(height: 8),
                    StatusBadge(status, fontSize: 12),
                  ],
                ],
              ),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () {
                      Navigator.pop(ctx);
                      onFinish();
                    },
                    child: Text(finishLabel),
                  ),
                ),
                if (onContinue != null) ...[
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () {
                        Navigator.pop(ctx);
                        onContinue();
                      },
                      child: Text(continueLabel),
                    ),
                  ),
                ],
              ],
            ),
          ],
        ),
      );
    },
  );
}
