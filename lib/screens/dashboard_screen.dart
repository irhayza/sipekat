import 'package:flutter/material.dart';
import 'package:si_pekat/config/app_config.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../services/app_session_cache.dart';
import '../theme/app_theme.dart';
import 'login_screen.dart';
import 'pengaduan/pengaduan_screen.dart';
import 'perbaikan/perbaikan_screen.dart';
import 'kunjungan/kunjungan_dashboard_screen.dart';
import 'ocr/ocr_home_screen.dart';

class DashboardScreen extends StatefulWidget {
  final String userNama;
  final String userEmail;

  const DashboardScreen({
    super.key,
    required this.userNama,
    required this.userEmail,
  });

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  bool _warming = false;

  @override
  void initState() {
    super.initState();
    // Jaring pengaman: bila layar ini dibuka lewat sesi yang sudah login
    // sebelumnya (app dibuka ulang, bukan lewat form login), cache mungkin
    // masih kosong. Muat sekali di sini secara diam-diam supaya modul-modul
    // tetap tidak perlu antre ke Google Sheet saat pertama dibuka.
    if (!AppSessionCache.instance.isPreloaded && !AppSessionCache.instance.isPreloading) {
      _warming = true;
      AppSessionCache.instance
          .preloadAll(nama: widget.userNama, email: widget.userEmail)
          .whenComplete(() {
        if (mounted) setState(() => _warming = false);
      });
    }
  }

  String get userNama => widget.userNama;
  String get userEmail => widget.userEmail;

  Future<void> _logout(BuildContext context) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text('Keluar Aplikasi?'),
        content: const Text('Sesi aktif Anda akan dihapus. Yakin ingin keluar?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Batal'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: TextButton.styleFrom(foregroundColor: AppColors.danger),
            child: const Text('Keluar', style: TextStyle(fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );

    if (confirm == true && context.mounted) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('session_user');
      await prefs.remove('session_nama');
      await prefs.remove('session_role');
      AppSessionCache.instance.clear();
      if (context.mounted) {
        Navigator.pushReplacement(
          context,
          PageRouteBuilder<void>(
            pageBuilder: (_, __, ___) => const LoginScreen(),
            transitionsBuilder: (_, anim, __, child) =>
                FadeTransition(opacity: anim, child: child),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.canvas,
      body: CustomScrollView(
        slivers: [
          // === HERO APP BAR (MODERN REVAMP) ===
          SliverAppBar(
            expandedHeight: 220,
            floating: false,
            pinned: true,
            backgroundColor: AppColors.brandDark,
            automaticallyImplyLeading: false,
            actions: [],
            flexibleSpace: FlexibleSpaceBar(
              background: Stack(
                children: [
                  // Gradient background for a deep neon feel
                  Container(
                    decoration: const BoxDecoration(
                      gradient: LinearGradient(
                        colors: [AppColors.brandDark, AppColors.brand],
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                      ),
                    ),
                  ),
                  // Glowing decorative elements
                  Positioned(
                    top: -50,
                    right: -50,
                    child: Container(
                      width: 250,
                      height: 250,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: AppColors.pengaduan.withValues(alpha: 0.1),
                        boxShadow: [
                          BoxShadow(color: AppColors.pengaduan.withValues(alpha: 0.2), blurRadius: 100)
                        ],
                      ),
                    ),
                  ),
                  Positioned(
                    bottom: -80,
                    left: -40,
                    child: Container(
                      width: 200,
                      height: 200,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: AppColors.selesai.withValues(alpha: 0.1),
                        boxShadow: [
                          BoxShadow(color: AppColors.selesai.withValues(alpha: 0.2), blurRadius: 100)
                        ],
                      ),
                    ),
                  ),
                  // Content
                  Padding(
                    padding: const EdgeInsets.fromLTRB(24, 60, 24, 20),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.end,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Container(
                              width: 56,
                              height: 56,
                              decoration: BoxDecoration(
                                color: Colors.white,
                                shape: BoxShape.circle,
                                border: Border.all(color: Colors.white.withValues(alpha: 0.5), width: 2),
                                boxShadow: [
                                  BoxShadow(
                                    color: AppColors.flame.withValues(alpha: 0.3),
                                    blurRadius: 12,
                                  )
                                ],
                              ),
                              child: const Icon(
                                  Icons.person_rounded,
                                  color: AppColors.flame,
                                  size: 30,
                                ),
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  const Text(
                                    'Selamat datang,',
                                    style: TextStyle(
                                      color: Colors.white70,
                                      fontSize: 13,
                                      fontWeight: FontWeight.w500,
                                    ),
                                  ),
                                  const SizedBox(height: 4),
                                  Text(
                                    userNama,
                                    style: const TextStyle(
                                      color: Colors.white,
                                      fontSize: 22,
                                      fontWeight: FontWeight.w800,
                                      letterSpacing: 0.5,
                                    ),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 24),
                        // Dashboard Status Bar (Task Management style)
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(
                              color: AppColors.line.withValues(alpha: 0.5),
                            ),
                          ),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Row(
                                children: [
                                  Icon(Icons.check_circle_rounded, color: AppColors.selesai, size: 18),
                                  SizedBox(width: 8),
                                  Text(
                                    'Sistem Online',
                                    style: TextStyle(
                                      color: AppColors.ink,
                                      fontSize: 13,
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                                ],
                              ),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                decoration: BoxDecoration(
                                  color: AppColors.flame.withValues(alpha: 0.15),
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                child: Text(
                                  AppConfig.appVersion,
                                  style: const TextStyle(
                                    color: AppColors.flame,
                                    fontSize: 11,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),

          // === CACHE WARMING INDICATOR ===
          if (_warming)
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                  decoration: BoxDecoration(
                    color: AppColors.brandLight,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Row(
                    children: [
                      SizedBox(
                        width: 14,
                        height: 14,
                        child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.ink),
                      ),
                      SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          'Menyegarkan data modul di latar belakang...',
                          style: TextStyle(fontSize: 12, color: AppColors.ink, fontWeight: FontWeight.w600),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),

          // === MODULE GRID ===
          SliverPadding(
            padding: const EdgeInsets.all(16),
            sliver: SliverGrid(
              delegate: SliverChildListDelegate([
                _ModuleCard(
                  icon: Icons.campaign_rounded,
                  label: 'Pengaduan',
                  subtitle: 'Laporan keluhan pelanggan',
                  color: AppColors.pengaduan,
                  lightColor: AppColors.pengaduanLight,
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute<void>(
                      builder: (_) => PengaduanScreen(
                        userName: userEmail,
                        userNama: userNama,
                      ),
                    ),
                  ),
                ),
                _ModuleCard(
                  icon: Icons.speed_rounded,
                  label: 'Pencatatan Meter',
                  subtitle: 'Baca & catat stand meter',
                  color: AppColors.meter,
                  lightColor: AppColors.meterLight,
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute<void>(
                      builder: (_) => OcrHomeScreen(
                        userNama: userNama,
                        userEmail: userEmail,
                      ),
                    ),
                  ),
                ),
                _ModuleCard(
                  icon: Icons.directions_walk_rounded,
                  label: 'Laporan Kunjungan',
                  subtitle: 'Kunjungan & reopen tiket',
                  color: AppColors.kunjungan,
                  lightColor: AppColors.kunjunganLight,
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute<void>(
                      builder: (_) => const KunjunganDashboardScreen(),
                    ),
                  ),
                ),
                _ModuleCard(
                  icon: Icons.build_rounded,
                  label: 'Laporan Perbaikan',
                  subtitle: 'Form penanganan petugas',
                  color: AppColors.perbaikan,
                  lightColor: AppColors.perbaikanLight,
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute<void>(
                      builder: (_) => const PerbaikanScreen(),
                    ),
                  ),
                ),
              ]),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                crossAxisSpacing: 12,
                mainAxisSpacing: 12,
                childAspectRatio: 0.95,
              ),
            ),
          ),

        ],
      ),
    );
  }
}

class _ModuleCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final String subtitle;
  final Color color;
  final Color lightColor;
  final VoidCallback onTap;

  const _ModuleCard({
    required this.icon,
    required this.label,
    required this.subtitle,
    required this.color,
    required this.lightColor,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(24),
        child: Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: AppColors.surface.withValues(alpha: 0.85),
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: AppColors.line.withValues(alpha: 0.8), width: 1.5),
            boxShadow: [
              BoxShadow(
                color: color.withValues(alpha: 0.15),
                blurRadius: 16,
                offset: const Offset(0, 8),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Icon badge (Neon glow effect)
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: color.withValues(alpha: 0.3)),
                ),
                child: Icon(icon, color: color, size: 28),
              ),
              const Spacer(),
              // Label
              Text(
                label,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                  color: AppColors.ink,
                  letterSpacing: 0.3,
                  height: 1.2,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                subtitle,
                style: const TextStyle(
                  fontSize: 12,
                  color: AppColors.body,
                  height: 1.3,
                ),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 14),
              // Arrow indicator
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: AppColors.brandDark,
                      shape: BoxShape.circle,
                      border: Border.all(color: AppColors.line),
                    ),
                    child: const Icon(
                      Icons.arrow_forward_rounded,
                      color: AppColors.flame, // Vibrant Red for CTAs
                      size: 16,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}