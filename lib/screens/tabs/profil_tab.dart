import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../services/app_session_cache.dart';
import '../login_screen.dart';

class ProfilTab extends StatelessWidget {
  final String userNama;
  final String userEmail;

  const ProfilTab({
    super.key,
    required this.userNama,
    required this.userEmail,
  });

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
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.danger, foregroundColor: Colors.white),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Keluar'),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    if (context.mounted) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('session_email');
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

  void _showGantiPasswordDialog(BuildContext context) {
    final oldPasswordController = TextEditingController();
    final newPasswordController = TextEditingController();
    final confirmPasswordController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Ganti Password'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: oldPasswordController,
                obscureText: true,
                decoration: const InputDecoration(
                  labelText: 'Password Lama',
                  prefixIcon: Icon(Icons.lock_outline),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: newPasswordController,
                obscureText: true,
                decoration: const InputDecoration(
                  labelText: 'Password Baru',
                  prefixIcon: Icon(Icons.lock),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: confirmPasswordController,
                obscureText: true,
                decoration: const InputDecoration(
                  labelText: 'Konfirmasi Password Baru',
                  prefixIcon: Icon(Icons.lock_reset),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Batal'),
            ),
            ElevatedButton(
              onPressed: () {
                // TODO: Implement actual password change via API/Apps Script
                if (newPasswordController.text != confirmPasswordController.text) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Konfirmasi password tidak cocok!')),
                  );
                  return;
                }
                
                Navigator.pop(context);
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('Fitur ganti password akan terhubung ke server segera.')),
                );
              },
              child: const Text('Simpan'),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final initial = userNama.isNotEmpty ? userNama[0].toUpperCase() : 'P';
    
    return Scaffold(
      backgroundColor: AppColors.canvas,
      appBar: AppBar(
        title: const Text('Profil Pengguna'),
        backgroundColor: AppColors.brandDark,
        foregroundColor: Colors.white,
        automaticallyImplyLeading: false,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            const SizedBox(height: 20),
            Center(
              child: CircleAvatar(
                radius: 50,
                backgroundColor: AppColors.brand.withValues(alpha: 0.1),
                child: Text(
                  initial,
                  style: const TextStyle(
                    fontSize: 48,
                    fontWeight: FontWeight.bold,
                    color: AppColors.brand,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Text(
              userNama,
              style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.ink),
            ),
            Text(
              userEmail,
              style: const TextStyle(fontSize: 14, color: AppColors.muted),
            ),
            const SizedBox(height: 32),
            Card(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              child: Column(
                children: [
                  ListTile(
                    leading: const Icon(Icons.lock_outline, color: AppColors.brand),
                    title: const Text('Ganti Password', style: TextStyle(fontSize: 14)),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () => _showGantiPasswordDialog(context),
                  ),
                  const Divider(height: 1),
                  const ListTile(
                    leading: Icon(Icons.settings, color: AppColors.brand),
                    title: Text('Pengaturan Aplikasi', style: TextStyle(fontSize: 14)),
                    trailing: Icon(Icons.chevron_right),
                  ),
                  const Divider(height: 1),
                  const ListTile(
                    leading: Icon(Icons.help_outline, color: AppColors.brand),
                    title: Text('Pusat Bantuan', style: TextStyle(fontSize: 14)),
                    trailing: Icon(Icons.chevron_right),
                  ),
                  const Divider(height: 1),
                  ListTile(
                    leading: const Icon(Icons.logout_rounded, color: AppColors.danger),
                    title: const Text('Keluar', style: TextStyle(fontSize: 14, color: AppColors.danger, fontWeight: FontWeight.bold)),
                    onTap: () => _logout(context),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
