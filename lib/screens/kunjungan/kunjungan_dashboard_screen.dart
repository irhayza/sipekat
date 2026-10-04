import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../services/kunjungan_local_cache_service.dart';
import '../../services/kunjungan_api_service.dart';
import '../../theme/app_theme.dart';
import '../../config/app_config.dart';
import '../../services/app_session_cache.dart';
import 'visit_list_screen.dart';
import 'reopen_list_screen.dart';

class KunjunganDashboardScreen extends StatefulWidget {
  const KunjunganDashboardScreen({super.key});

  @override
  State<KunjunganDashboardScreen> createState() => _KunjunganDashboardScreenState();
}

class _KunjunganDashboardScreenState extends State<KunjunganDashboardScreen> {
  String _userName = 'Petugas';
  String _userRole = 'petugas';
  String _userId = '';
  int _pendingCount = 0;

  int _currentIndex = 0;
  List<Map<String, dynamic>> _historyList = [];
  bool _loadingHistory = false;

  @override
  void initState() {
    super.initState();
    _loadSession();
    _checkPendingCount();
  }

  Future<void> _loadSession() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _userName = prefs.getString('session_nama') ?? 'Petugas';
      _userRole = prefs.getString('session_role') ?? 'petugas';
      _userId = prefs.getString('session_user') ?? '';
    });
  }

  Future<void> _checkPendingCount() async {
    final pending = await KunjunganLocalCacheService.instance.getPendingSubmissions();
    if (mounted) {
      setState(() {
        _pendingCount = pending.length;
      });
    }
  }

  Future<void> _loadHistory() async {
    setState(() => _loadingHistory = true);
    final list = await KunjunganLocalCacheService.instance.getHistorySubmissions();
    setState(() {
      _historyList = list;
      _loadingHistory = false;
    });
  }

  void _showSnackbar(String message, {bool isError = false}) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError ? AppColors.danger : AppColors.selesai,
      ),
    );
  }

  Future<void> _handleDelete(String idpel, String status) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text('Hapus Riwayat?'),
        content: Text('Apakah Anda yakin ingin menghapus data laporan untuk pelanggan $idpel dari riwayat lokal?${status == 'pending' ? '\n\nLaporan ini bertanda PENDING dan akan dihapus permanen dari antrean pengiriman offline.' : ''}'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Batal'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Hapus', style: TextStyle(color: AppColors.danger)),
          ),
        ],
      ),
    );

    if (confirm == true) {
      await KunjunganLocalCacheService.instance.removeHistorySubmission(idpel);
      if (status == 'pending') {
        await KunjunganLocalCacheService.instance.removePendingSubmission(idpel);
      }
      _showSnackbar('Data riwayat berhasil dihapus.', isError: false);
      _loadHistory();
      _checkPendingCount();
    }
  }

  void _sendSingle(Map<String, dynamic> item) {
    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (context) {
        return _SyncProgressDialog(
          pendingItems: [item],
          onComplete: () {
            _loadHistory();
            _checkPendingCount();
          },
        );
      },
    );
  }

  void _syncAll() {
    final pending = _historyList.where((x) => x['status'] == 'pending').toList();
    if (pending.isEmpty) {
      _showSnackbar('Tidak ada data pending yang perlu disinkronkan.', isError: false);
      return;
    }

    pending.sort((a, b) {
      final tA = a['timestamp']?.toString() ?? '';
      final tB = b['timestamp']?.toString() ?? '';
      return tA.compareTo(tB);
    });

    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (context) {
        return _SyncProgressDialog(
          pendingItems: pending,
          onComplete: () {
            _loadHistory();
            _checkPendingCount();
          },
        );
      },
    );
  }

  void _showChangePasswordDialog() {
    final oldPassCtrl = TextEditingController();
    final newPassCtrl = TextEditingController();
    final confirmPassCtrl = TextEditingController();
    final formKey = GlobalKey<FormState>();
    bool obscureOld = true;
    bool obscureNew = true;
    bool obscureConfirm = true;

    showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              title: const Row(
                children: [
                  Icon(Icons.lock_rounded, color: AppColors.kunjungan),
                  SizedBox(width: 10),
                  Text('Ganti Password', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                ],
              ),
              content: Form(
                key: formKey,
                child: SingleChildScrollView(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      TextFormField(
                        controller: oldPassCtrl,
                        obscureText: obscureOld,
                        decoration: InputDecoration(
                          labelText: 'Password Lama',
                          prefixIcon: const Icon(Icons.lock_open_rounded),
                          suffixIcon: IconButton(
                            icon: Icon(obscureOld ? Icons.visibility_off : Icons.visibility),
                            onPressed: () => setDialogState(() => obscureOld = !obscureOld),
                          ),
                        ),
                        validator: (v) => (v == null || v.isEmpty) ? 'Password lama wajib diisi' : null,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: newPassCtrl,
                        obscureText: obscureNew,
                        decoration: InputDecoration(
                          labelText: 'Password Baru',
                          prefixIcon: const Icon(Icons.lock_outline_rounded),
                          suffixIcon: IconButton(
                            icon: Icon(obscureNew ? Icons.visibility_off : Icons.visibility),
                            onPressed: () => setDialogState(() => obscureNew = !obscureNew),
                          ),
                        ),
                        validator: (v) {
                          if (v == null || v.isEmpty) return 'Password baru wajib diisi';
                          if (v.length < 3) return 'Password minimal 3 karakter';
                          return null;
                        },
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: confirmPassCtrl,
                        obscureText: obscureConfirm,
                        decoration: InputDecoration(
                          labelText: 'Konfirmasi Password Baru',
                          prefixIcon: const Icon(Icons.lock_rounded),
                          suffixIcon: IconButton(
                            icon: Icon(obscureConfirm ? Icons.visibility_off : Icons.visibility),
                            onPressed: () => setDialogState(() => obscureConfirm = !obscureConfirm),
                          ),
                        ),
                        validator: (v) {
                          if (v != newPassCtrl.text) return 'Konfirmasi password tidak cocok';
                          return null;
                        },
                      ),
                    ],
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('Batal'),
                ),
                ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.kunjungan,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                  ),
                  onPressed: () async {
                    if (!formKey.currentState!.validate()) return;

                    showDialog<void>(
                      context: context,
                      barrierDismissible: false,
                      builder: (ctx) => const Center(child: CircularProgressIndicator()),
                    );

                    try {
                      final res = await KunjunganApiService.instance.changePassword(
                        user: _userId,
                        oldPassword: oldPassCtrl.text,
                        newPassword: newPassCtrl.text,
                      );
                      
                      if (!context.mounted) return;
                      Navigator.pop(context); // Close loading
                      Navigator.pop(context); // Close Change Password

                      showDialog<void>(
                        context: context,
                        builder: (ctx) => AlertDialog(
                          title: const Text('Sukses'),
                          content: Text(res['message']?.toString() ?? 'Password berhasil diubah.'),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.pop(ctx),
                              child: const Text('OK'),
                            ),
                          ],
                        ),
                      );
                    } catch (e) {
                      if (!context.mounted) return;
                      Navigator.pop(context); // Close loading
                      _showSnackbar('Gagal mengubah password: $e', isError: true);
                    }
                  },
                  child: const Text('Simpan', style: TextStyle(color: Colors.white)),
                ),
              ],
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.canvas,
      appBar: AppBar(
        title: Text(
          _currentIndex == 0
              ? 'Laporan Kunjungan'
              : _currentIndex == 1
                  ? 'Riwayat Laporan'
                  : 'Profil Akun',
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => Navigator.pop(context),
          tooltip: 'Kembali ke Dashboard Utama',
        ),
      ),
      body: _buildDashboardBody(),
    );
  }

  Widget _buildDashboardBody() {
    return SingleChildScrollView(
      child: Column(
        children: [
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
            decoration: const BoxDecoration(
              color: AppColors.kunjungan,
              borderRadius: BorderRadius.only(
                bottomLeft: Radius.circular(30),
                bottomRight: Radius.circular(30),
              ),
            ),
            child: Row(
              children: [
                CircleAvatar(
                  radius: 30,
                  backgroundColor: Colors.white,
                  child: Text(
                    _userName.isNotEmpty ? _userName[0].toUpperCase() : 'P',
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      color: AppColors.kunjungan,
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _userName,
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                        decoration: BoxDecoration(
                          color: Colors.white24,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(
                          _userRole.toUpperCase(),
                          style: const TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w600,
                            color: Colors.white,
                            letterSpacing: 0.5,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),

          Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 10),
                const Text(
                  'PILIHAN MENU UTAMA',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    color: AppColors.muted,
                    letterSpacing: 1.0,
                  ),
                ),
                const SizedBox(height: 16),

                _buildMenuCard(
                  title: 'KUNJUNGAN',
                  subtitle: 'Pencatatan Kunjungan (Cabut / Segel / Lunas / Rumah Tertutup)',
                  icon: Icons.gavel_rounded,
                  gradientColors: [
                    AppColors.kunjungan,
                    const Color(0xFF1E40AF),
                  ],
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute<void>(
                        builder: (_) => VisitListScreen(
                          userName: _userName,
                          userRole: _userRole,
                        ),
                      ),
                    ).then((_) {
                      _checkPendingCount();
                      _loadHistory();
                    });
                  },
                ),

                const SizedBox(height: 16),

                _buildMenuCard(
                  title: 'PEMBUKAAN ALIRAN',
                  subtitle: 'Pencatatan Pembukaan Aliran (Buka Segel / Pasang Kembali)',
                  icon: Icons.check_circle_outline_rounded,
                  gradientColors: [
                    AppColors.selesai,
                    const Color(0xFF15803D),
                  ],
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute<void>(
                        builder: (_) => ReopenListScreen(
                          userName: _userName,
                          userRole: _userRole,
                        ),
                      ),
                    ).then((_) {
                      _checkPendingCount();
                      _loadHistory();
                    });
                  },
                ),

                const SizedBox(height: 24),

                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: AppColors.muted),
                  ),
                  child: const Row(
                    children: [
                      Icon(
                        Icons.info_outline_rounded,
                        color: AppColors.kunjungan,
                        size: 28,
                      ),
                      SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Petunjuk Singkat',
                              style: TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 14,
                              ),
                            ),
                            SizedBox(height: 2),
                            Text(
                              'Gunakan menu Kunjungan untuk mencatat pemutusan aliran, dan menu Pembukaan untuk memproses penyambungan kembali.',
                              style: TextStyle(
                                color: AppColors.muted,
                                fontSize: 12,
                                height: 1.4,
                              ),
                            ),
                          ],
                        ),
                      )
                    ],
                  ),
                )
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRiwayatBody() {
    final pending = _historyList.where((x) => x['status'] == 'pending').toList();
    
    return _loadingHistory
        ? const Center(child: CircularProgressIndicator())
        : Column(
            children: [
              if (pending.isNotEmpty)
                Container(
                  width: double.infinity,
                  color: AppColors.prosesBg,
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                  child: Row(
                    children: [
                      const Icon(Icons.cloud_upload_outlined, color: AppColors.proses),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          'Ada ${pending.length} data laporan offline tertunda.',
                          style: const TextStyle(color: AppColors.proses, fontWeight: FontWeight.w600, fontSize: 13),
                        ),
                      ),
                      ElevatedButton(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.proses,
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                        ),
                        onPressed: _syncAll,
                        child: const Text('Sinkronkan Semua', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold)),
                      ),
                    ],
                  ),
                ),
              Expanded(
                child: _historyList.isEmpty
                    ? const Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.history_toggle_off_rounded, size: 64, color: AppColors.muted),
                            SizedBox(height: 16),
                            Text(
                              'Belum ada riwayat kunjungan.',
                              style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppColors.muted),
                            ),
                          ],
                        ),
                      )
                    : ListView.builder(
                        padding: const EdgeInsets.all(12),
                        itemCount: _historyList.length,
                        itemBuilder: (ctx, index) {
                          final item = _historyList[index];
                          final idpel = item['idpel']?.toString() ?? '';
                          final name = item['nama']?.toString() ?? 'Pelanggan';
                          final actionType = item['actionType']?.toString() ?? '';
                          final status = item['status']?.toString() ?? 'sent';
                          final timestamp = item['timestamp']?.toString() ?? '';
                          final alamat = item['alamat']?.toString() ?? 'Alamat tidak tersedia';
                          
                          String dateStr = '';
                          try {
                            final parsed = DateTime.parse(timestamp);
                            String pad(int n) => n < 10 ? '0$n' : '$n';
                            dateStr = '${parsed.year}-${pad(parsed.month)}-${pad(parsed.day)} ${parsed.hour}:${parsed.minute}';
                          } catch (_) {
                            dateStr = timestamp;
                          }

                          final isPending = status == 'pending';

                          return Card(
                            margin: const EdgeInsets.only(bottom: 12),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
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
                                          'IDPEL: $idpel',
                                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                                        ),
                                      ),
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                        decoration: BoxDecoration(
                                          color: isPending ? AppColors.prosesBg : AppColors.selesaiBg,
                                          borderRadius: BorderRadius.circular(8),
                                          border: Border.all(color: isPending ? AppColors.proses : AppColors.selesai),
                                        ),
                                        child: Row(
                                          mainAxisSize: MainAxisSize.min,
                                          children: [
                                            Icon(
                                              isPending ? Icons.offline_pin_rounded : Icons.check_circle_rounded,
                                              size: 14,
                                              color: isPending ? AppColors.proses : AppColors.selesai,
                                            ),
                                            const SizedBox(width: 4),
                                            Text(
                                              isPending ? 'PENDING' : 'TERKIRIM',
                                              style: TextStyle(
                                                fontSize: 10,
                                                fontWeight: FontWeight.bold,
                                                color: isPending ? AppColors.proses : AppColors.selesai,
                                              ),
                                            ),
                                          ],
                                        ),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 8),
                                  Text(
                                    name,
                                    style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                                  ),
                                  const SizedBox(height: 4),
                                  Row(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      const Icon(Icons.location_on_outlined, size: 14, color: AppColors.muted),
                                      const SizedBox(width: 4),
                                      Expanded(
                                        child: Text(
                                          alamat,
                                          style: const TextStyle(fontSize: 12, color: AppColors.muted),
                                        ),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 6),
                                  Row(
                                    children: [
                                      const Text('Tindakan: ', style: TextStyle(fontSize: 12, color: AppColors.muted)),
                                      Text(
                                        actionType.toUpperCase(),
                                        style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.muted),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 2),
                                  Row(
                                    children: [
                                      const Text('Waktu: ', style: TextStyle(fontSize: 12, color: AppColors.muted)),
                                      Text(
                                        dateStr,
                                        style: const TextStyle(fontSize: 12, color: AppColors.muted),
                                      ),
                                    ],
                                  ),
                                  const Divider(height: 20),
                                  Row(
                                    mainAxisAlignment: MainAxisAlignment.end,
                                    children: [
                                      TextButton.icon(
                                        style: TextButton.styleFrom(foregroundColor: AppColors.danger),
                                        onPressed: () => _handleDelete(idpel, status),
                                        icon: const Icon(Icons.delete_outline_rounded, size: 18),
                                        label: const Text('Hapus', style: TextStyle(fontSize: 12)),
                                      ),
                                      const SizedBox(width: 8),
                                      if (isPending)
                                        ElevatedButton.icon(
                                          style: ElevatedButton.styleFrom(
                                            backgroundColor: AppColors.kunjungan,
                                            foregroundColor: Colors.white,
                                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                                          ),
                                          onPressed: () => _sendSingle(item),
                                          icon: const Icon(Icons.send_rounded, size: 14),
                                          label: const Text('Kirim', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold)),
                                        ),
                                    ],
                                  ),
                                ],
                              ),
                            ),
                          );
                        },
                      ),
              ),
            ],
          );
  }

  Widget _buildAkunBody() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          const SizedBox(height: 20),
          CircleAvatar(
            radius: 50,
            backgroundColor: AppColors.kunjungan,
            child: Text(
              _userName.isNotEmpty ? _userName[0].toUpperCase() : 'P',
              style: const TextStyle(fontSize: 48, fontWeight: FontWeight.bold, color: Colors.white),
            ),
          ),
          const SizedBox(height: 16),
          Text(
            _userName,
            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 4),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
            decoration: BoxDecoration(
              color: AppColors.kunjungan.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Text(
              _userRole.toUpperCase(),
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.bold,
                color: AppColors.kunjungan,
                letterSpacing: 0.5,
              ),
            ),
          ),
          const SizedBox(height: 32),
          Card(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  ListTile(
                    leading: const Icon(Icons.person_outline_rounded, color: AppColors.kunjungan),
                    title: const Text('Nama Lengkap', style: TextStyle(fontSize: 12, color: AppColors.muted)),
                    subtitle: Text(_userName, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
                  ),
                  const Divider(),
                  ListTile(
                    leading: const Icon(Icons.security_rounded, color: AppColors.kunjungan),
                    title: const Text('Hak Akses / Role', style: TextStyle(fontSize: 12, color: AppColors.muted)),
                    subtitle: Text(_userRole.toUpperCase(), style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
                  ),
                  const Divider(),
                  const ListTile(
                    leading: Icon(Icons.offline_pin_outlined, color: AppColors.selesai),
                    title: Text('Status Aplikasi', style: TextStyle(fontSize: 12, color: AppColors.muted)),
                    subtitle: Text('Aktif & Sinkron', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppColors.selesai)),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 30),
          SizedBox(
            width: double.infinity,
            height: 50,
            child: OutlinedButton.icon(
              style: OutlinedButton.styleFrom(
                side: const BorderSide(color: AppColors.kunjungan),
                foregroundColor: AppColors.kunjungan,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              onPressed: _showChangePasswordDialog,
              icon: const Icon(Icons.lock_reset_rounded),
              label: const Text('Ganti Password', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            height: 50,
            child: ElevatedButton.icon(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.kunjungan,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              onPressed: () => Navigator.pop(context),
              icon: const Icon(Icons.arrow_back_rounded),
              label: const Text('Kembali ke Dashboard Utama', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMenuCard({
    required String title,
    required String subtitle,
    required IconData icon,
    required List<Color> gradientColors,
    required VoidCallback onTap,
  }) {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: gradientColors,
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: gradientColors[0].withValues(alpha: 0.3),
            blurRadius: 10,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(20),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 28),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: const TextStyle(
                          fontSize: 22,
                          fontWeight: FontWeight.w800,
                          color: Colors.white,
                          letterSpacing: 1.0,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        subtitle,
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.white.withValues(alpha: 0.9),
                          height: 1.4,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 16),
                CircleAvatar(
                  radius: 26,
                  backgroundColor: Colors.white24,
                  child: Icon(icon, size: 28, color: Colors.white),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _SyncProgressDialog extends StatefulWidget {
  final List<Map<String, dynamic>> pendingItems;
  final VoidCallback onComplete;

  const _SyncProgressDialog({
    required this.pendingItems,
    required this.onComplete,
  });

  @override
  State<_SyncProgressDialog> createState() => _SyncProgressDialogState();
}

class _SyncProgressDialogState extends State<_SyncProgressDialog> {
  int _currentIndex = 0;
  int _successCount = 0;
  int _failCount = 0;
  String _currentStatus = 'Memulai sinkronisasi...';
  final List<String> _logs = [];
  bool _isFinished = false;

  @override
  void initState() {
    super.initState();
    _startSync();
  }

  void _addLog(String msg) {
    final now = DateTime.now();
    final timeStr = '${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}:${now.second.toString().padLeft(2, '0')}';
    if (mounted) {
      setState(() {
        _logs.add('[$timeStr] $msg');
      });
    }
  }

  Future<void> _startSync() async {
    _addLog('Menemukan ${widget.pendingItems.length} data laporan pending.');
    
    for (int i = 0; i < widget.pendingItems.length; i++) {
      if (!mounted) return;
      final item = widget.pendingItems[i];
      final idpel = item['idpel']?.toString() ?? '';
      final actionType = item['actionType']?.toString() ?? '';

      setState(() {
        _currentIndex = i;
        _currentStatus = 'Mengirim data $idpel ($actionType)...';
      });
      _addLog('Mengirim $idpel ($actionType)...');

      int attempts = 0;
      bool success = false;
      String errorMsg = '';

      while (attempts < 2 && !success) {
        attempts++;
        try {
          String urlSeb = '';
          String urlSes = '';
          
          if ((item['fotoSebBase64']?.toString() ?? '').isNotEmpty) {
            urlSeb = await KunjunganApiService.instance.uploadPhoto(
              base64: item['fotoSebBase64'].toString(),
              filename: 'sync_${idpel}_seb.jpg',
            );
          }
          if ((item['fotoSesBase64']?.toString() ?? '').isNotEmpty) {
            urlSes = await KunjunganApiService.instance.uploadPhoto(
              base64: item['fotoSesBase64'].toString(),
              filename: 'sync_${idpel}_ses.jpg',
            );
          }

          final actionLower = actionType.toLowerCase();
          final isPembukaan = actionLower == 'pasang kembali' || actionLower == 'buka segel';
          final isPasang = actionLower == 'pasang kembali';
          final isCabut = actionLower == 'cabut';
          
          final lat = item['lat']?.toString() ?? '';
          final lng = item['lng']?.toString() ?? '';
          final timeBeforeStr = item['timeBefore']?.toString() ?? '';
          final timeAfterStr = item['timeAfter']?.toString() ?? '';
          
          if (isPembukaan) {
            final updates = <String, dynamic>{
              'PENGADUAN_RP': actionType.toUpperCase(),
              'STATUS_RP': 'SELESAI',
              'Foto_Seb_RP': urlSeb,
              'Foto_Ses_RP': urlSes,
              if (timeBeforeStr.isNotEmpty) 'Waktu_Seb_RP': timeBeforeStr,
              if (timeAfterStr.isNotEmpty) 'Waktu_Ses_RP': timeAfterStr,
              if (lat.isNotEmpty) 'Latitude_RP': lat,
              if (lng.isNotEmpty) 'Longitude_RP': lng,
              if (isPasang) 'No_MGRT_Baru_RP': item['nomgrtBru']?.toString() ?? '',
              if (isPasang) 'Angka_MGRT_Baru_RP': item['stMgrt']?.toString() ?? '',
              'Petugas_RP': AppSessionCache.instance.officerNama,
            };
            await KunjunganApiService.instance.updateRowCells(
              sheetName: AppConfig.pengaduanSheetName,
              idpel: idpel,
              keyColumn: 'IDPEL_RP',
              filterColumn: 'PENGADUAN_RP',
              filterValue: actionType.toUpperCase(),
              updates: updates,
            );
          } else {
            final updates = <String, dynamic>{
              'KJG': actionType.toUpperCase(),
              'LINK_SEB': urlSeb,
              'LINK_SES': urlSes,
              if (lat.isNotEmpty) 'LATITUDE_KJG': lat,
              if (lng.isNotEmpty) 'LONGITUDE_KJG': lng,
              if (timeBeforeStr.isNotEmpty && !isCabut) 'EXIF_SEB': timeBeforeStr,
              if (timeAfterStr.isNotEmpty && !isCabut) 'EXIF_SES': timeAfterStr,
              if (isCabut && (item['nomgrtBru']?.toString() ?? '').isNotEmpty) 'NOMGRT_BARU': item['nomgrtBru'].toString(),
              if (isCabut && (item['stMgrt']?.toString() ?? '').isNotEmpty) 'STMGRT_BARU': item['stMgrt'].toString(),
              'TGL_KJG': timeAfterStr.isNotEmpty ? timeAfterStr : DateTime.now().toIso8601String(),
              'ORDER': '2',
            };
            await KunjunganApiService.instance.updateRowCells(
              sheetName: 'dbase',
              idpel: idpel,
              updates: updates,
            );
          }

          success = true;
        } catch (e) {
          if (e is ServerException) {
            errorMsg = e.message;
          } else {
            errorMsg = e.toString();
          }
        }

        if (!success && attempts < 2) {
          _addLog('Gagal: $errorMsg. Percobaan mengirim kembali ($attempts/2)...');
          await Future<void>.delayed(const Duration(seconds: 2));
        }
      }

      if (success) {
        _addLog('IDPEL $idpel sukses terkirim.');
        await KunjunganLocalCacheService.instance.updateHistorySubmissionStatus(idpel, 'sent');
        await KunjunganLocalCacheService.instance.removePendingSubmission(idpel);
        
        setState(() {
          _successCount++;
        });
      } else {
        _addLog('IDPEL $idpel gagal dikirim setelah 2 percobaan. Error: $errorMsg');
        setState(() {
          _failCount++;
        });
      }
    }

    setState(() {
      _isFinished = true;
      _currentStatus = 'Sinkronisasi selesai!';
    });
    _addLog('Proses sinkronisasi selesai. Sukses: $_successCount, Gagal: $_failCount.');
    widget.onComplete();
  }

  @override
  Widget build(BuildContext context) {
    final total = widget.pendingItems.length;
    final progress = total > 0 ? (_currentIndex + 1) / total : 0.0;

    return AlertDialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      title: const Row(
        children: [
          Icon(Icons.sync_rounded, color: AppColors.kunjungan),
          SizedBox(width: 10),
          Text('Sinkronisasi Data', style: TextStyle(fontWeight: FontWeight.bold)),
        ],
      ),
      content: SizedBox(
        width: double.maxFinite,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(_currentStatus, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
            const SizedBox(height: 12),
            LinearProgressIndicator(
              value: _isFinished ? 1.0 : progress,
              backgroundColor: AppColors.muted,
              color: AppColors.kunjungan,
              minHeight: 8,
              borderRadius: BorderRadius.circular(4),
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Proses: ${_currentIndex + 1}/$total', style: const TextStyle(fontSize: 11, color: AppColors.muted)),
                Text('Sukses: $_successCount | Gagal: $_failCount', style: const TextStyle(fontSize: 11, color: AppColors.muted)),
              ],
            ),
            const SizedBox(height: 16),
            const Text('Log Pengiriman:', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
            const SizedBox(height: 6),
            Container(
              height: 150,
              width: double.infinity,
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: AppColors.muted,
                borderRadius: BorderRadius.circular(8),
              ),
              child: ListView.builder(
                shrinkWrap: true,
                itemCount: _logs.length,
                itemBuilder: (ctx, index) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 4),
                    child: Text(
                      _logs[index],
                      style: const TextStyle(color: AppColors.selesai, fontSize: 10, fontFamily: 'monospace'),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
      actions: [
        if (_isFinished)
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.kunjungan,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
            onPressed: () => Navigator.pop(context),
            child: const Text('Tutup', style: TextStyle(color: Colors.white)),
          ),
      ],
    );
  }
}