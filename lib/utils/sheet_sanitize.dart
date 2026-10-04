/// Utilitas kecil yang meniru `Validation.sanitize()` di sisi GAS lama:
/// trim + uppercase, dan tambahkan tanda kutip di depan kalau nilainya
/// diawali karakter yang bisa dibaca Google Sheets sebagai awal formula
/// (=, +, -, @) — supaya orang tidak bisa menyuntik formula lewat input
/// bebas teks (mis. kolom "Kendala"/"Tindakan").
///
/// Sekarang beberapa layar menulis langsung ke sel lewat endpoint generik
/// `update_row_cells`, jadi perlindungan ini perlu direplikasi di sisi
/// Flutter juga (dulu otomatis terjadi di GAS sebelum menyentuh sheet).
String sanitizeForSheet(String? value) {
  final s = (value ?? '').trim().toUpperCase();
  if (s.isEmpty) return s;
  if ('=+-@'.contains(s[0])) return "'$s";
  return s;
}
