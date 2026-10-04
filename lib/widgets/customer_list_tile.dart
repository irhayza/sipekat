import 'package:flutter/material.dart';
import '../services/kunjungan_api_service.dart';
import '../theme/app_theme.dart';
import '../utils/contact_launcher.dart';

/// Kartu ringkas pelanggan untuk daftar Kunjungan & Pembukaan.
///
/// Sebelumnya modul Kunjungan (visit_list_screen) dan modul Pembukaan
/// (reopen_list_screen) masing-masing punya salinan kartu yang nyaris
/// identik dengan warna berbeda-beda (Colors.blueGrey, Colors.orange,
/// Colors.grey, dll — tidak memakai AppColors). Widget ini menyatukan
/// keduanya memakai palet & tipografi yang sama dengan modul Perbaikan
/// dan Pengaduan, ditambah aksi cepat Telepon/WhatsApp bila nomor
/// pelanggan tersedia.
class CustomerListTile extends StatelessWidget {
  final VisitCustomer customer;
  final VoidCallback onTap;

  /// Warna aksen badge tunggakan (default oranye/"proses").
  final Color accent;

  /// Jika true, render model Pembukaan (tampilkan telfon & jenis pengaduan, sembunyikan bln/rupiah)
  final bool isReopen;

  const CustomerListTile({
    super.key,
    required this.customer,
    required this.onTap,
    this.accent = AppColors.proses,
    this.isReopen = false,
  });

  String _formatRupiah(double val) {
    return 'Rp ${val.toStringAsFixed(0).replaceAllMapped(
          RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
          (Match m) => '${m[1]}.',
        )}';
  }

  @override
  Widget build(BuildContext context) {
    final hasPhone = ContactLauncher.hasValidPhone(customer.telepon);

    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Text(
                      customer.idpel,
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 15,
                        color: AppColors.muted,
                      ),
                    ),
                  ),
                  if (!isReopen)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: accent.withValues(alpha: 0.10),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: accent.withValues(alpha: 0.3)),
                      ),
                      child: Text(
                        'Tunggakan: ${customer.bln} Bln',
                        style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: accent),
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                customer.nama,
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: AppColors.ink),
              ),
              const SizedBox(height: 4),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.location_on_outlined, size: 14, color: AppColors.muted),
                  const SizedBox(width: 4),
                  Expanded(
                    child: Text(
                      customer.alamat.trim().isNotEmpty ? customer.alamat : 'Alamat tidak tersedia (Cek penamaan kolom di Sheet)',
                      style: const TextStyle(color: AppColors.muted, fontSize: 13),
                    ),
                  ),
                ],
              ),
              if (isReopen && customer.telepon.isNotEmpty) ...[
                const SizedBox(height: 4),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(Icons.phone_outlined, size: 14, color: AppColors.muted),
                    const SizedBox(width: 4),
                    const Text(
                      'Telfon: ',
                      style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.muted, fontSize: 12),
                    ),
                    Expanded(
                      child: Text(
                        customer.telepon,
                        style: const TextStyle(color: AppColors.muted, fontSize: 12),
                      ),
                    ),
                  ],
                ),
              ],
              if (customer.kendala.isNotEmpty) ...[
                const SizedBox(height: 4),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(isReopen ? Icons.assignment_outlined : Icons.report_problem_outlined, size: 14, color: AppColors.muted),
                    const SizedBox(width: 4),
                    if (isReopen)
                      const Text(
                        'Pengaduan: ',
                        style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.muted, fontSize: 12),
                      ),
                    Expanded(
                      child: Text(
                        customer.kendala,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: isReopen ? AppColors.ink : AppColors.muted,
                          fontSize: 12,
                          fontWeight: isReopen ? FontWeight.w600 : FontWeight.normal,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 12),
              const Divider(height: 1, color: AppColors.line),
              const SizedBox(height: 10),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'No Meter: ${customer.nomgrt}',
                    style: const TextStyle(fontSize: 12, color: AppColors.muted),
                  ),
                  if (!isReopen)
                    Text(
                      _formatRupiah(customer.rupiah),
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: AppColors.danger),
                    ),
                ],
              ),
              if (hasPhone) ...[
                const SizedBox(height: 10),
                Row(
                  children: [
                    _QuickActionButton(
                      icon: Icons.call_rounded,
                      label: 'Telepon',
                      color: AppColors.ink,
                      onTap: () => ContactLauncher.callPhone(customer.telepon),
                    ),
                    const SizedBox(width: 8),
                    _QuickActionButton(
                      icon: Icons.chat_rounded,
                      label: 'WhatsApp',
                      color: const Color(0xFF25D366),
                      onTap: () => ContactLauncher.openWhatsApp(customer.telepon),
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

/// Tombol aksi cepat (Telepon / WhatsApp) — dipisah menjadi StatelessWidget
/// agar bisa di-const dan tidak ikut rebuild saat tile induk di-scroll.
class _QuickActionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;

  const _QuickActionButton({
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
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 13, color: color),
              const SizedBox(width: 5),
              Text(label, style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.w700, color: color)),
            ],
          ),
        ),
      ),
    );
  }
}
