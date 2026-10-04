import 'package:flutter/material.dart';
import 'package:si_pekat/config/app_config.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../services/auth_service.dart';
import '../services/app_session_cache.dart';
import '../services/location_service.dart';
import '../theme/app_theme.dart';
import 'main_nav_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen>
    with SingleTickerProviderStateMixin {
  final _formKey = GlobalKey<FormState>();
  final _emailCtrl = TextEditingController();
  final _passCtrl = TextEditingController();
  bool _obscureText = true;
  bool _loading = false;
  String? _loadingStage;
  String? _errorMsg;

  late AnimationController _animCtrl;
  late Animation<double> _fadeAnim;
  late Animation<Offset> _slideAnim;

  @override
  void initState() {
    super.initState();
    _animCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 700),
    );
    _fadeAnim = CurvedAnimation(parent: _animCtrl, curve: Curves.easeOut);
    _slideAnim = Tween<Offset>(
      begin: const Offset(0, 0.12),
      end: Offset.zero,
    ).animate(CurvedAnimation(parent: _animCtrl, curve: Curves.easeOut));
    _animCtrl.forward();
  }

  @override
  void dispose() {
    _animCtrl.dispose();
    _emailCtrl.dispose();
    _passCtrl.dispose();
    super.dispose();
  }

  Future<void> _handleLogin() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _loading = true;
      _loadingStage = 'Memeriksa GPS perangkat...';
      _errorMsg = null;
    });

    try {
      final gps = await LocationService.instance.getCurrentLocation(
        timeLimit: const Duration(seconds: 6),
      );
      if (!gps.isOk) {
        String errMsg = 'GPS perangkat Anda harus aktif untuk masuk ke aplikasi.';
        if (gps.result == GpsResult.serviceDisabled) {
          errMsg = 'GPS perangkat Anda mati. Mohon aktifkan GPS lalu coba lagi.';
        } else if (gps.result == GpsResult.permissionDenied || gps.result == GpsResult.permissionDeniedForever) {
          errMsg = 'Aplikasi membutuhkan izin lokasi/GPS. Mohon berikan izin lokasi lalu coba lagi.';
        }
        setState(() {
          _errorMsg = errMsg;
          _loading = false;
        });
        return;
      }

      setState(() {
        _loadingStage = 'Melakukan autentikasi...';
      });

      final res = await AuthService.instance.login(
        _emailCtrl.text.trim(),
        _passCtrl.text,
      );

      if (res['status'] == 'success') {
        final nama = res['nama'] ?? _emailCtrl.text.trim();
        final email = res['email'] ?? _emailCtrl.text.trim();
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString('session_user', email);
        await prefs.setString('session_nama', nama);

        // Muat semua daftar yang dibutuhkan tiap modul (Perbaikan,
        // Kunjungan, Pembukaan, Pencatatan Meter) SEKALI di sini, saat
        // login. Setelah ini, layar-layar modul cukup membaca dari cache
        // dan tidak perlu menunggu Google Sheet setiap kali dibuka.
        if (mounted) setState(() => _loadingStage = 'Menyiapkan data awal...');
        await AppSessionCache.instance.preloadAll(
          nama: nama,
          email: email,
          onProgress: (stage) {
            if (mounted) setState(() => _loadingStage = stage);
          },
        );

        if (mounted) {
          Navigator.pushReplacement(
            context,
            PageRouteBuilder<void>(
              pageBuilder: (_, __, ___) => MainNavScreen(
                userNama: nama,
                userEmail: email,
              ),
              transitionsBuilder: (_, anim, __, child) =>
                  FadeTransition(opacity: anim, child: child),
              transitionDuration: const Duration(milliseconds: 400),
            ),
          );
        }
      } else {
        setState(() {
          _errorMsg = res['message'] ?? 'Login gagal. Coba lagi.';
          _loading = false;
        });
      }
    } catch (e) {
      setState(() {
        _errorMsg = e.toString().replaceAll('Exception: ', '');
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.brand,
      body: Stack(
        children: [
          // Background gradient decoration
          Positioned(
            top: -80,
            right: -60,
            child: Container(
              width: 280,
              height: 280,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: Colors.white.withValues(alpha: 0.05),
              ),
            ),
          ),
          Positioned(
            bottom: -100,
            left: -80,
            child: Container(
              width: 320,
              height: 320,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: Colors.white.withValues(alpha: 0.04),
              ),
            ),
          ),

          SafeArea(
            child: SingleChildScrollView(
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  minHeight: MediaQuery.of(context).size.height -
                      MediaQuery.of(context).padding.top -
                      MediaQuery.of(context).padding.bottom,
                ),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 24),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const SizedBox(height: 40),

                      // Logo & Title
                      FadeTransition(
                        opacity: _fadeAnim,
                        child: SlideTransition(
                          position: _slideAnim,
                          child: Column(
                            children: [
                              // Logo icon
                              Container(
                                width: 96,
                                height: 96,
                                padding: const EdgeInsets.all(4),
                                decoration: BoxDecoration(
                                  color: Colors.white,
                                  shape: BoxShape.circle,
                                  boxShadow: [
                                    BoxShadow(
                                      color: Colors.black.withValues(alpha: 0.1),
                                      blurRadius: 10,
                                      offset: const Offset(0, 5),
                                    ),
                                  ],
                                ),
                                child: ClipOval(
                                  child: Image.asset(
                                    'assets/icon.png',
                                    fit: BoxFit.cover,
                                  ),
                                ),
                              ),
                              const SizedBox(height: 20),
                              const Text(
                                'SiPEKAT',
                                style: TextStyle(
                                  fontSize: 36,
                                  fontWeight: FontWeight.w900,
                                  color: Colors.white,
                                  letterSpacing: 2.0,
                                ),
                                textAlign: TextAlign.center,
                              ),
                              const SizedBox(height: 6),
                              Text(
                                'Sistem Pelaporan Kegiatan\ndan Tindakan Petugas Jargas',
                                style: TextStyle(
                                  fontSize: 13,
                                  color: Colors.white.withValues(alpha: 0.75),
                                  fontWeight: FontWeight.w400,
                                  height: 1.5,
                                ),
                                textAlign: TextAlign.center,
                              ),
                            ],
                          ),
                        ),
                      ),

                      const SizedBox(height: 48),

                      // Login Card
                      FadeTransition(
                        opacity: _fadeAnim,
                        child: SlideTransition(
                          position: _slideAnim,
                          child: Container(
                            padding: const EdgeInsets.all(24),
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(24),
                              boxShadow: [
                                BoxShadow(
                                  color: Colors.black.withValues(alpha: 0.15),
                                  blurRadius: 30,
                                  offset: const Offset(0, 8),
                                ),
                              ],
                            ),
                            child: Form(
                              key: _formKey,
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: [
                                  const Text(
                                    'Masuk ke Akun Anda',
                                    style: TextStyle(
                                      fontSize: 20,
                                      fontWeight: FontWeight.bold,
                                      color: AppColors.ink,
                                    ),
                                  ),
                                  const SizedBox(height: 6),
                                  const Text(
                                    'Gunakan User ID dan password yang diberikan oleh admin.',
                                    style: TextStyle(
                                      fontSize: 12.5,
                                      color: AppColors.muted,
                                    ),
                                  ),
                                  const SizedBox(height: 24),

                                  // Error Banner
                                  if (_errorMsg != null)
                                    Container(
                                      padding: const EdgeInsets.all(12),
                                      margin: const EdgeInsets.only(bottom: 16),
                                      decoration: BoxDecoration(
                                        color: AppColors.dangerBg,
                                        borderRadius: BorderRadius.circular(12),
                                        border: Border.all(
                                          color: AppColors.danger.withValues(alpha: 0.3),
                                        ),
                                      ),
                                      child: Row(
                                        children: [
                                          const Icon(Icons.error_outline,
                                              color: AppColors.danger, size: 18),
                                          const SizedBox(width: 10),
                                          Expanded(
                                            child: Text(
                                              _errorMsg!,
                                              style: const TextStyle(
                                                color: AppColors.danger,
                                                fontSize: 13,
                                                fontWeight: FontWeight.w500,
                                              ),
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),

                                  // User ID field
                                  TextFormField(
                                    controller: _emailCtrl,
                                    keyboardType: TextInputType.text,
                                    textInputAction: TextInputAction.next,
                                    decoration: InputDecoration(
                                      labelText: 'User ID',
                                      hintText: 'Contoh: sda01',
                                      prefixIcon: const Icon(Icons.badge_outlined,
                                          color: AppColors.brand),
                                      enabledBorder: OutlineInputBorder(
                                        borderRadius: BorderRadius.circular(12),
                                        borderSide:
                                            const BorderSide(color: AppColors.line),
                                      ),
                                      focusedBorder: OutlineInputBorder(
                                        borderRadius: BorderRadius.circular(12),
                                        borderSide: const BorderSide(
                                            color: AppColors.brand, width: 1.8),
                                      ),
                                      errorBorder: OutlineInputBorder(
                                        borderRadius: BorderRadius.circular(12),
                                        borderSide:
                                            const BorderSide(color: AppColors.danger),
                                      ),
                                      focusedErrorBorder: OutlineInputBorder(
                                        borderRadius: BorderRadius.circular(12),
                                        borderSide: const BorderSide(
                                            color: AppColors.danger, width: 1.8),
                                      ),
                                      filled: true,
                                      fillColor: AppColors.canvas,
                                    ),
                                    validator: (v) => (v == null || v.trim().isEmpty)
                                        ? 'User ID wajib diisi'
                                        : null,
                                  ),
                                  const SizedBox(height: 16),

                                  // Password field
                                  TextFormField(
                                    controller: _passCtrl,
                                    obscureText: _obscureText,
                                    textInputAction: TextInputAction.done,
                                    onFieldSubmitted: (_) =>
                                        _loading ? null : _handleLogin(),
                                    decoration: InputDecoration(
                                      labelText: 'Password',
                                      prefixIcon: const Icon(Icons.lock_outline,
                                          color: AppColors.brand),
                                      suffixIcon: IconButton(
                                        icon: Icon(
                                          _obscureText
                                              ? Icons.visibility_off_outlined
                                              : Icons.visibility_outlined,
                                          color: AppColors.muted,
                                          size: 20,
                                        ),
                                        onPressed: () => setState(
                                            () => _obscureText = !_obscureText),
                                      ),
                                      enabledBorder: OutlineInputBorder(
                                        borderRadius: BorderRadius.circular(12),
                                        borderSide:
                                            const BorderSide(color: AppColors.line),
                                      ),
                                      focusedBorder: OutlineInputBorder(
                                        borderRadius: BorderRadius.circular(12),
                                        borderSide: const BorderSide(
                                            color: AppColors.brand, width: 1.8),
                                      ),
                                      errorBorder: OutlineInputBorder(
                                        borderRadius: BorderRadius.circular(12),
                                        borderSide:
                                            const BorderSide(color: AppColors.danger),
                                      ),
                                      focusedErrorBorder: OutlineInputBorder(
                                        borderRadius: BorderRadius.circular(12),
                                        borderSide: const BorderSide(
                                            color: AppColors.danger, width: 1.8),
                                      ),
                                      filled: true,
                                      fillColor: AppColors.canvas,
                                    ),
                                    validator: (v) =>
                                        (v == null || v.isEmpty)
                                            ? 'Password wajib diisi'
                                            : null,
                                  ),
                                  const SizedBox(height: 28),

                                  // Login Button
                                  SizedBox(
                                    height: 52,
                                    child: ElevatedButton(
                                      style: ElevatedButton.styleFrom(
                                        backgroundColor: AppColors.brand,
                                        foregroundColor: Colors.white,
                                        shape: RoundedRectangleBorder(
                                          borderRadius: BorderRadius.circular(12),
                                        ),
                                        elevation: 0,
                                      ),
                                      onPressed: _loading ? null : _handleLogin,
                                      child: _loading
                                          ? const SizedBox(
                                              width: 22,
                                              height: 22,
                                              child: CircularProgressIndicator(
                                                strokeWidth: 2.5,
                                                color: Colors.white,
                                              ),
                                            )
                                          : const Text(
                                              'MASUK',
                                              style: TextStyle(
                                                fontSize: 15,
                                                fontWeight: FontWeight.bold,
                                                letterSpacing: 1.2,
                                              ),
                                            ),
                                    ),
                                  ),
                                  if (_loading && _loadingStage != null) ...[
                                    const SizedBox(height: 10),
                                    Text(
                                      _loadingStage!,
                                      textAlign: TextAlign.center,
                                      style: const TextStyle(fontSize: 12, color: AppColors.muted),
                                    ),
                                  ],
                                ],
                              ),
                            ),
                          ),
                        ),
                      ),

                      const SizedBox(height: 32),

                      // Footer
                      Text(
                        '${AppConfig.appVersion} · SiPEKAT — Jargas Petugas App',
                        style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.5),
                          fontSize: 11,
                          fontWeight: FontWeight.w400,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 24),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
