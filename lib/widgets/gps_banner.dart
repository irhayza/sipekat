import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

enum GpsBannerState { loading, ok, warning }

class GpsBanner extends StatelessWidget {
  final GpsBannerState state;
  final String message;

  /// Aksi opsional saat banner disentuh, mis. membuka Pengaturan Aplikasi
  /// ketika izin lokasi ditolak, atau mencoba ulang mengunci GPS.
  final VoidCallback? onTap;

  const GpsBanner({
    super.key,
    required this.state,
    required this.message,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    Color bg;
    Color fg;
    Widget icon;

    switch (state) {
      case GpsBannerState.loading:
        bg = const Color(0xFFEFF6FF);
        fg = const Color(0xFF1E40AF);
        icon = const SizedBox(
          width: 14,
          height: 14,
          child: CircularProgressIndicator(strokeWidth: 2),
        );
        break;
      case GpsBannerState.ok:
        bg = AppColors.selesaiBg;
        fg = AppColors.selesai;
        icon = const Icon(Icons.location_on, size: 16, color: AppColors.selesai);
        break;
      case GpsBannerState.warning:
        bg = AppColors.prosesBg;
        fg = AppColors.proses;
        icon = const Icon(Icons.location_off_outlined, size: 16, color: AppColors.proses);
        break;
    }

    final content = Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(12)),
      child: Row(
        children: [
          icon,
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              message,
              style: TextStyle(color: fg, fontSize: 12.5, fontWeight: FontWeight.w600),
            ),
          ),
          if (onTap != null) Icon(Icons.chevron_right_rounded, size: 18, color: fg),
        ],
      ),
    );

    if (onTap == null) return content;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: content,
      ),
    );
  }
}
