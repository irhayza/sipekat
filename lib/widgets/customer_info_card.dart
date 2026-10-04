import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../utils/contact_launcher.dart';

/// Kartu informasi pelanggan terpadu — dipakai di modul Perbaikan,
/// Pengaduan, dan Kunjungan supaya tampilan data pelanggan konsisten
/// di seluruh aplikasi.
///
/// Menampilkan (jika tersedia): ID Pelanggan, Nama, Alamat, Telepon
/// (dengan tombol Telepon & WhatsApp langsung), Kendala/Keluhan, dan
/// tombol Lihat/Bagikan Lokasi bila koordinat GPS tersedia.
class CustomerInfoCard extends StatelessWidget {
  final String? idPelanggan;
  final String? nama;
  final String? alamat;
  final String? telepon;
  final String? kendala;
  final double? lat;
  final double? lng;

  /// Baris tambahan spesifik modul (mis. No MGRT, Tarif, Tunggakan)
  /// ditampilkan setelah field standar di atas.
  final List<MapEntry<String, String>> extraRows;

  /// Widget kecil di pojok kanan atas (mis. StatusBadge).
  final Widget? trailing;

  const CustomerInfoCard({
    super.key,
    this.idPelanggan,
    this.nama,
    this.alamat,
    this.telepon,
    this.kendala,
    this.lat,
    this.lng,
    this.extraRows = const [],
    this.trailing,
  });

  bool get _hasLocation => lat != null && lng != null;

  @override
  Widget build(BuildContext context) {
    final hasPhone = ContactLauncher.hasValidPhone(telepon);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (nama != null && nama!.trim().isNotEmpty)
                      Text(
                        nama!,
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.bold,
                          color: AppColors.ink,
                        ),
                      ),
                    if (idPelanggan != null && idPelanggan!.trim().isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 2),
                        child: Text(
                          'ID Pelanggan: $idPelanggan',
                          style: const TextStyle(fontSize: 12, color: AppColors.muted),
                        ),
                      ),
                  ],
                ),
              ),
              if (trailing != null) trailing!,
            ],
          ),
          if (alamat != null && alamat!.trim().isNotEmpty) ...[
            const SizedBox(height: 10),
            _iconRow(Icons.location_on_outlined, alamat!),
          ],
          if (telepon != null && telepon!.trim().isNotEmpty) ...[
            const SizedBox(height: 8),
            _iconRow(Icons.phone_outlined, telepon!),
          ],
          if (kendala != null && kendala!.trim().isNotEmpty) ...[
            const SizedBox(height: 8),
            _iconRow(Icons.report_problem_outlined, kendala!),
          ],
          for (final row in extraRows) ...[
            const SizedBox(height: 8),
            _labelValueRow(row.key, row.value),
          ],
          if (hasPhone || _hasLocation) ...[
            const SizedBox(height: 14),
            const Divider(height: 1, color: AppColors.line),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                if (hasPhone)
                  _ActionChip(
                    icon: Icons.call_rounded,
                    label: 'Telepon',
                    color: AppColors.brand,
                    onTap: () async {
                      final ok = await ContactLauncher.callPhone(telepon);
                      if (!ok && context.mounted) {
                        ContactLauncher.showLaunchFailure(context, 'aplikasi telepon');
                      }
                    },
                  ),
                if (hasPhone)
                  _ActionChip(
                    icon: Icons.chat_rounded,
                    label: 'WhatsApp',
                    color: const Color(0xFF25D366),
                    onTap: () async {
                      final ok = await ContactLauncher.openWhatsApp(
                        telepon,
                        message: nama != null
                            ? 'Halo $nama, kami dari petugas Jargas ingin menindaklanjuti laporan Anda.'
                            : null,
                      );
                      if (!ok && context.mounted) {
                        ContactLauncher.showLaunchFailure(context, 'WhatsApp');
                      }
                    },
                  ),
                if (_hasLocation)
                  _ActionChip(
                    icon: Icons.map_rounded,
                    label: 'Lihat Lokasi',
                    color: AppColors.proses,
                    onTap: () async {
                      final ok = await ContactLauncher.openMap(lat!, lng!, label: nama);
                      if (!ok && context.mounted) {
                        ContactLauncher.showLaunchFailure(context, 'aplikasi peta');
                      }
                    },
                  ),
                if (_hasLocation)
                  _ActionChip(
                    icon: Icons.share_location_rounded,
                    label: 'Bagikan Lokasi',
                    color: AppColors.pengaduan,
                    onTap: () async {
                      final ok = await ContactLauncher.shareLocationViaWhatsApp(
                        lat!,
                        lng!,
                        toPhone: telepon,
                        label: nama,
                      );
                      if (!ok && context.mounted) {
                        ContactLauncher.showLaunchFailure(context, 'WhatsApp');
                      }
                    },
                  ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _iconRow(IconData icon, String text) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 15, color: AppColors.muted),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            style: const TextStyle(fontSize: 13, color: AppColors.body, height: 1.35),
          ),
        ),
      ],
    );
  }

  Widget _labelValueRow(String label, String value) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 96,
          child: Text(
            label,
            style: const TextStyle(fontSize: 12.5, color: AppColors.muted),
          ),
        ),
        Expanded(
          child: Text(
            value,
            style: const TextStyle(
              fontSize: 13,
              color: AppColors.ink,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ],
    );
  }
}

class _ActionChip extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;

  const _ActionChip({
    required this.icon,
    required this.label,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: color.withValues(alpha: 0.10),
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(10),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 15, color: color),
              const SizedBox(width: 6),
              Text(
                label,
                style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w700, color: color),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
