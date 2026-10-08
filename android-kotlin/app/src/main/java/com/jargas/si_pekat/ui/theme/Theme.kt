package com.jargas.si_pekat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Palet warna terpadu SiPEKAT (port dari theme/app_theme.dart): biru gelap, merah (aksen), hijau (sukses).
 * Hex "0x33RRGGBB" di Flutter = alpha 20% → ditulis sama di sini (Color(0x33...)).
 */
object AppColors {
    // Brand utama — Merah & Biru
    val Brand = Color(0xFFED1C24)
    val BrandDark = Color(0xFFC7151B)
    val BrandLight = Color(0xFFFDE8E8)
    val BlueAccent = Color(0xFF005BAA)
    val Flame = Color(0xFFED1C24)

    // Warna per modul
    val Pengaduan = Color(0xFFED1C24)
    val PengaduanLight = Color(0x33ED1C24)
    val Meter = Color(0xFF005BAA)
    val MeterLight = Color(0x33005BAA)
    val Kunjungan = Color(0xFF005BAA)
    val KunjunganLight = Color(0x33005BAA)
    val Perbaikan = Color(0xFF10B981)
    val PerbaikanLight = Color(0x3310B981)

    // Netral
    val Ink = Color(0xFF111111)
    val Body = Color(0xFF4B5563)
    val Muted = Color(0xFF9CA3AF)
    val Line = Color(0xFFE5E7EB)
    val Surface = Color(0xFFFFFFFF)
    val Canvas = Color(0xFFF3F4F6)

    // Status
    val Open = Color(0xFFEF4444)
    val OpenBg = Color(0x33EF4444)
    val Proses = Color(0xFFF59E0B)
    val ProsesBg = Color(0x33F59E0B)
    val Selesai = Color(0xFF10B981)
    val SelesaiBg = Color(0x3310B981)
    val Danger = Color(0xFFEF4444)
    val DangerBg = Color(0x33EF4444)
    val Success = Color(0xFF10B981)
    val SuccessBg = Color(0x3310B981)
    val Warning = Color(0xFFF59E0B)
    val WarningBg = Color(0x33F59E0B)

    val WhatsApp = Color(0xFF25D366)
}

private val LightScheme = lightColorScheme(
    primary = AppColors.Brand,
    onPrimary = Color.White,
    secondary = AppColors.BlueAccent,
    onSecondary = Color.White,
    surface = AppColors.Surface,
    background = AppColors.Canvas,
    error = AppColors.Danger,
    onSurface = AppColors.Ink,
    onBackground = AppColors.Ink,
    outline = AppColors.Line,
)

/** Hanya tema terang — sama seperti Flutter (dark() mengembalikan light()). */
@Composable
fun SiPekatTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightScheme, content = content)
}
