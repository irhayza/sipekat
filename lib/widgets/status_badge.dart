import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class StatusBadge extends StatelessWidget {
  final String status;
  final double fontSize;

  const StatusBadge(this.status, {super.key, this.fontSize = 11});

  @override
  Widget build(BuildContext context) {
    Color bg = AppColors.brandLight;
    Color fg = AppColors.muted;
    String label = status.isEmpty ? '—' : status;

    switch (status.toUpperCase()) {
      case 'SELESAI':
        bg = AppColors.selesaiBg;
        fg = AppColors.selesai;
        label = 'SELESAI';
        break;
      case 'PROSES':
        bg = AppColors.prosesBg;
        fg = AppColors.proses;
        label = 'PROSES';
        break;
      case 'OPEN':
        bg = AppColors.openBg;
        fg = AppColors.open;
        label = 'OPEN';
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(99)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 6,
            height: 6,
            margin: const EdgeInsets.only(right: 6),
            decoration: BoxDecoration(color: fg, shape: BoxShape.circle),
          ),
          Text(
            label,
            style: TextStyle(
              color: fg,
              fontSize: fontSize,
              fontWeight: FontWeight.w700,
              letterSpacing: 0.3,
            ),
          ),
        ],
      ),
    );
  }
}
