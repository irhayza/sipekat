import 'dart:io';
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class PhotoCaptureTile extends StatelessWidget {
  final String label;
  final File? photo;
  final DateTime? capturedAt;
  final VoidCallback onCapture;

  const PhotoCaptureTile({
    super.key,
    required this.label,
    required this.photo,
    required this.capturedAt,
    required this.onCapture,
  });

  String _formatTime(DateTime d) {
    String two(int n) => n < 10 ? '0$n' : '$n';
    return '${d.year}-${two(d.month)}-${two(d.day)} ${two(d.hour)}:${two(d.minute)}';
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppColors.ink),
        ),
        const SizedBox(height: 8),
        ClipRRect(
          borderRadius: BorderRadius.circular(12),
          child: AspectRatio(
            aspectRatio: 16 / 10,
            child: GestureDetector(
              onTap: onCapture,
              child: Stack(
                fit: StackFit.expand,
                children: [
                  if (photo != null)
                    Image.file(photo!, fit: BoxFit.cover)
                  else
                    Container(
                      color: AppColors.canvas,
                      child: const Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.photo_camera_outlined, size: 30, color: AppColors.muted),
                          SizedBox(height: 6),
                          Text(
                            'Ketuk untuk buka kamera',
                            style: TextStyle(color: AppColors.muted, fontSize: 12),
                          ),
                        ],
                      ),
                    ),
                  if (photo != null)
                    Positioned(
                      left: 0,
                      right: 0,
                      bottom: 0,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            begin: Alignment.bottomCenter,
                            end: Alignment.topCenter,
                            colors: [Colors.black.withValues(alpha: 0.65), Colors.transparent],
                          ),
                        ),
                        child: Row(
                          children: [
                            const Icon(Icons.access_time, size: 12, color: Colors.white70),
                            const SizedBox(width: 4),
                            Expanded(
                              child: Text(
                                capturedAt != null ? _formatTime(capturedAt!) : '-',
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 11,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  if (photo != null)
                    Positioned(
                      top: 8,
                      right: 8,
                      child: Material(
                        color: Colors.black.withValues(alpha: 0.55),
                        shape: const CircleBorder(),
                        child: InkWell(
                          customBorder: const CircleBorder(),
                          onTap: onCapture,
                          child: const Padding(
                            padding: EdgeInsets.all(6),
                            child: Icon(Icons.refresh, size: 16, color: Colors.white),
                          ),
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }
}
