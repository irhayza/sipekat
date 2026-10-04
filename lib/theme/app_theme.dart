import 'package:flutter/material.dart';

/// Palet warna terpadu SiPEKAT (Sistem Pelaporan Kegiatan dan Tindakan).
/// Tema Orisinal: Kombinasi Biru Gelap (Background), Merah (Aksen), Hijau (Sukses).
class AppColors {
  AppColors._();

  // Brand utama — MyPertamina-inspired (Red & Blue)
  static const Color brand      = Color(0xFFED1C24); // Pertamina Red
  static const Color brandDark  = Color(0xFFC7151B);
  static const Color brandLight = Color(0xFFFDE8E8);
  
  // Aksen utama — Biru & Hijau
  static const Color blueAccent = Color(0xFF005BAA); // Pertamina Blue
  static const Color flame      = Color(0xFFED1C24); 

  // Warna per-modul 
  static const Color pengaduan  = Color(0xFFED1C24); 
  static const Color pengaduanLight = Color(0x33ED1C24);
  static const Color meter      = Color(0xFF005BAA); 
  static const Color meterLight = Color(0x33005BAA);
  static const Color kunjungan  = Color(0xFF005BAA); 
  static const Color kunjunganLight = Color(0x33005BAA);
  static const Color perbaikan  = Color(0xFF10B981); 
  static const Color perbaikanLight = Color(0x3310B981);

  // Netral untuk Light Mode (Kontras bersih)
  static const Color ink     = Color(0xFF111111); // Teks Hitam Utama
  static const Color body    = Color(0xFF4B5563); // Teks Abu-abu utama
  static const Color muted   = Color(0xFF9CA3AF); // Teks Abu-abu terang
  static const Color line    = Color(0xFFE5E7EB); // Garis pemisah tipis
  static const Color surface = Color(0xFFFFFFFF); // Permukaan Card (Putih)
  static const Color canvas  = Color(0xFFF3F4F6); // Background utama (Abu-abu sangat terang)

  // Status (Red, Yellow, Green accents)
  static const Color open      = Color(0xFFEF4444);
  static const Color openBg    = Color(0x33EF4444);
  static const Color proses    = Color(0xFFF59E0B);
  static const Color prosesBg  = Color(0x33F59E0B);
  static const Color selesai   = Color(0xFF10B981);
  static const Color selesaiBg = Color(0x3310B981);
  static const Color danger    = Color(0xFFEF4444);
  static const Color dangerBg  = Color(0x33EF4444);
  static const Color success   = Color(0xFF10B981);
  static const Color successBg = Color(0x3310B981);
  static const Color warning   = Color(0xFFF59E0B);
  static const Color warningBg = Color(0x33F59E0B);
}

class AppTheme {
  AppTheme._();

  static ThemeData light() {
    final base = ThemeData.light(useMaterial3: true);
    
    return base.copyWith(
      scaffoldBackgroundColor: AppColors.canvas,
      colorScheme: const ColorScheme.light(
        primary: AppColors.brand,
        secondary: AppColors.blueAccent,
        surface: AppColors.surface,
        error: AppColors.danger,
        onPrimary: Colors.white,
        onSecondary: Colors.white,
      ),
      textTheme: base.textTheme.apply(
        bodyColor: AppColors.ink,
        displayColor: AppColors.ink,
        fontFamily: 'Roboto',
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.brand,
        foregroundColor: Colors.white,
        elevation: 0,
        centerTitle: true,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.brand,
          foregroundColor: Colors.white,
          elevation: 2,
          shadowColor: AppColors.brand.withValues(alpha: 0.3),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(24), // Pill-shaped like MyPertamina
          ),
          padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 24),
          textStyle: const TextStyle(
            fontWeight: FontWeight.w700,
            fontSize: 15,
            letterSpacing: 0.5,
          ),
        ),
      ),
      cardTheme: CardThemeData(
        color: AppColors.surface,
        elevation: 4,
        shadowColor: Colors.black.withValues(alpha: 0.05),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide.none,
        ),
        margin: EdgeInsets.zero,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        contentPadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.line),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.line),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.brand, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.danger),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.danger, width: 1.5),
        ),
        labelStyle: const TextStyle(color: AppColors.muted, fontSize: 14),
      ),
    );
  }

  // Override dark theme supaya ke light theme karena MyPertamina dominan terang
  static ThemeData dark() => light();
}
