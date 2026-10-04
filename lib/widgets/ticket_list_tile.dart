import 'package:flutter/material.dart';
import '../models/perbaikan/ticket.dart';
import '../theme/app_theme.dart';
import '../utils/contact_launcher.dart';

/// Kartu tiket perbaikan bergaya premium untuk daftar Laporan Perbaikan.
///
/// Menampilkan: No. Tiket, IDPEL, Nama, Alamat, Pengaduan/Kendala,
/// Telepon (dengan aksi WA langsung), dan Lokasi Map jika tersedia.
/// Seluruh kartu dapat diklik untuk membuka form tindakan & dokumentasi.
class TicketListTile extends StatelessWidget {
  final Ticket ticket;
  final VoidCallback onTap;

  const TicketListTile({
    super.key,
    required this.ticket,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final hasPhone = ContactLauncher.hasValidPhone(ticket.telepon);
    final hasLocation = ticket.lat != null && ticket.lng != null;
    final statusColor = _statusColor(ticket.status);

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: const BorderSide(color: AppColors.line),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // ── Header tiket ─────────────────────────────────────────────
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: AppColors.ink.withValues(alpha: 0.05),
                borderRadius: const BorderRadius.only(
                  topLeft: Radius.circular(16),
                  topRight: Radius.circular(16),
                ),
                border: const Border(
                  bottom: BorderSide(color: AppColors.line),
                ),
              ),
              child: Row(
                children: [
                  // Icon tiket
                  Container(
                    width: 38,
                    height: 38,
                    decoration: BoxDecoration(
                      color: AppColors.ink.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Icon(
                      Icons.assignment_rounded,
                      color: AppColors.ink,
                      size: 20,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          ticket.ticket,
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w800,
                            color: AppColors.ink,
                            letterSpacing: 0.3,
                          ),
                        ),
                        if (ticket.idPelanggan.isNotEmpty)
                          Text(
                            'IDPEL: ${ticket.idPelanggan}',
                            style: const TextStyle(
                              fontSize: 11.5,
                              color: AppColors.muted,
                            ),
                          ),
                      ],
                    ),
                  ),
                  // Status badge
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
                    decoration: BoxDecoration(
                      color: statusColor.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: statusColor.withValues(alpha: 0.4)),
                    ),
                    child: Text(
                      ticket.status,
                      style: TextStyle(
                        fontSize: 10.5,
                        fontWeight: FontWeight.bold,
                        color: statusColor,
                        letterSpacing: 0.3,
                      ),
                    ),
                  ),
                ],
              ),
            ),

            // ── Detail pelanggan ─────────────────────────────────────────
            Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Nama
                  Text(
                    ticket.nama.isNotEmpty ? ticket.nama : 'Nama tidak tersedia',
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.bold,
                      color: AppColors.ink,
                    ),
                  ),
                  const SizedBox(height: 8),

                  // Alamat
                  if (ticket.alamat.isNotEmpty)
                    _infoRow(
                      icon: Icons.location_on_outlined,
                      text: ticket.alamat,
                      color: AppColors.muted,
                    ),

                  // Pengaduan / Kendala
                  if (ticket.kendala.isNotEmpty) ...[
                    const SizedBox(height: 6),
                    _infoRow(
                      icon: Icons.report_problem_outlined,
                      text: ticket.kendala,
                      color: AppColors.proses,
                      maxLines: 2,
                    ),
                  ],

                  // Telepon — tampilkan sebagai teks + bisa diklik WA
                  if (ticket.telepon.isNotEmpty) ...[
                    const SizedBox(height: 6),
                    GestureDetector(
                      onTap: () => ContactLauncher.openWhatsApp(ticket.telepon),
                      child: _infoRow(
                        icon: Icons.phone_rounded,
                        text: ticket.telepon,
                        color: AppColors.ink,
                        isClickable: true,
                      ),
                    ),
                  ],

                  // ── Tombol Aksi Cepat ──────────────────────────────────
                  if (hasPhone || hasLocation) ...[
                    const SizedBox(height: 14),
                    const Divider(height: 1, color: AppColors.line),
                    const SizedBox(height: 12),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        // Telepon langsung
                        if (hasPhone)
                          _QuickChip(
                            icon: Icons.call_rounded,
                            label: 'Telepon',
                            color: AppColors.ink,
                            onTap: () async {
                              final ok = await ContactLauncher.callPhone(ticket.telepon);
                              if (!ok && context.mounted) {
                                ContactLauncher.showLaunchFailure(context, 'aplikasi telepon');
                              }
                            },
                          ),
                        // WhatsApp
                        if (hasPhone)
                          _QuickChip(
                            icon: Icons.chat_rounded,
                            label: 'WhatsApp',
                            color: const Color(0xFF25D366),
                            onTap: () async {
                              final ok = await ContactLauncher.openWhatsApp(
                                ticket.telepon,
                                message:
                                    'Halo ${ticket.nama}, kami dari petugas Jargas ingin menindaklanjuti laporan Anda (No. Tiket: ${ticket.ticket}).',
                              );
                              if (!ok && context.mounted) {
                                ContactLauncher.showLaunchFailure(context, 'WhatsApp');
                              }
                            },
                          ),

                        // Buka Lokasi Map
                        if (hasLocation)
                          _QuickChip(
                            icon: Icons.map_rounded,
                            label: 'Lihat Lokasi',
                            color: AppColors.proses,
                            onTap: () async {
                              final ok = await ContactLauncher.openMap(
                                ticket.lat!,
                                ticket.lng!,
                                label: ticket.nama,
                              );
                              if (!ok && context.mounted) {
                                ContactLauncher.showLaunchFailure(context, 'aplikasi peta');
                              }
                            },
                          ),
                      ],
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _infoRow({
    required IconData icon,
    required String text,
    required Color color,
    int maxLines = 1,
    bool isClickable = false,
  }) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 1),
          child: Icon(icon, size: 14, color: color),
        ),
        const SizedBox(width: 6),
        Expanded(
          child: Text(
            text,
            maxLines: maxLines,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: 12.5,
              color: isClickable
                  ? color
                  : (color == AppColors.proses ? AppColors.proses : AppColors.body),
              height: 1.35,
              fontWeight: isClickable ? FontWeight.w600 : FontWeight.normal,
              decoration: isClickable ? TextDecoration.underline : TextDecoration.none,
              decorationColor: color,
            ),
          ),
        ),
      ],
    );
  }

  Color _statusColor(String status) {
    switch (status.toUpperCase()) {
      case 'OPEN':
        return AppColors.open;
      case 'PROSES':
        return AppColors.proses;
      case 'SELESAI':
        return AppColors.selesai;
      default:
        return AppColors.muted;
    }
  }
}

class _QuickChip extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;

  const _QuickChip({
    required this.icon,
    required this.label,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: color.withValues(alpha: 0.10),
      borderRadius: BorderRadius.circular(8),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 8),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 14, color: color),
              const SizedBox(width: 5),
              Text(
                label,
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                  color: color,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
