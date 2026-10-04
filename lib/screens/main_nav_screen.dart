import 'package:flutter/material.dart';
import 'dashboard_screen.dart';
import 'tabs/riwayat_tab.dart';
import 'tabs/pesan_tab.dart';
import 'tabs/profil_tab.dart';
import '../theme/app_theme.dart';

class MainNavScreen extends StatefulWidget {
  final String userNama;
  final String userEmail;

  const MainNavScreen({
    super.key,
    required this.userNama,
    required this.userEmail,
  });

  @override
  State<MainNavScreen> createState() => _MainNavScreenState();
}

class _MainNavScreenState extends State<MainNavScreen> {
  int _currentIndex = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: [
          DashboardScreen(userNama: widget.userNama, userEmail: widget.userEmail),
          const RiwayatTab(),
          const PesanTab(),
          ProfilTab(userNama: widget.userNama, userEmail: widget.userEmail),
        ],
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        type: BottomNavigationBarType.fixed,
        selectedItemColor: AppColors.brand,
        unselectedItemColor: AppColors.muted,
        onTap: (index) {
          setState(() {
            _currentIndex = index;
          });
        },
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.home_rounded),
            label: 'Beranda',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.history_rounded),
            label: 'Riwayat',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.mail_rounded),
            label: 'Pesan',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.person_rounded),
            label: 'Profil',
          ),
        ],
      ),
    );
  }
}
