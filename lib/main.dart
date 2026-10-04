import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'screens/login_screen.dart';
import 'screens/dashboard_screen.dart';
import 'screens/main_nav_screen.dart';
import 'services/app_session_cache.dart';
import 'theme/app_theme.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Rendering lebih smooth: pakai raster cache yang lebih besar
  PaintingBinding.instance.imageCache.maximumSizeBytes = 100 << 20; // 100 MB

  final prefs = await SharedPreferences.getInstance();
  final userEmail = prefs.getString('session_user');
  final userNama  = prefs.getString('session_nama');
  final isLoggedIn = userEmail != null && userNama != null;

  if (isLoggedIn) {
    // Muat cache data dari memori lokal HP agar langsung tersedia tanpa loading
    await AppSessionCache.instance.initFromLocal(userNama!, userEmail!);
  }

  runApp(SiPekatApp(
    isLoggedIn: isLoggedIn,
    userEmail: userEmail ?? '',
    userNama: userNama ?? '',
  ));
}


class SiPekatApp extends StatelessWidget {
  final bool isLoggedIn;
  final String userEmail;
  final String userNama;

  const SiPekatApp({
    super.key,
    required this.isLoggedIn,
    required this.userEmail,
    required this.userNama,
  });

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SiPEKAT',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      home: isLoggedIn
          ? MainNavScreen(userNama: userNama, userEmail: userEmail)
          : const LoginScreen(),
    );
  }
}
