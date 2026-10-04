/// Data tiket laporan perbaikan, sekaligus menyimpan ringkasan data
/// pelanggan (ID, alamat, telepon, kendala, koordinat) sehingga petugas
/// bisa langsung melihat & menghubungi pelanggan dari layar Laporan
/// Perbaikan tanpa berpindah modul.
class Ticket {
  final String ticket;
  final String nama;
  final String status;
  final String area;

  /// ID pelanggan (idpel) — dipakai untuk menampilkan & mencocokkan data
  /// pelanggan lintas-modul (mis. saat autofill di modul Pengaduan).
  final String idPelanggan;
  final String alamat;
  final String telepon;

  /// Ringkasan kendala/keluhan yang dilaporkan pelanggan.
  final String kendala;

  final double? lat;
  final double? lng;

  const Ticket({
    required this.ticket,
    required this.nama,
    required this.status,
    required this.area,
    this.idPelanggan = '',
    this.alamat = '',
    this.telepon = '',
    this.kendala = '',
    this.lat,
    this.lng,
  });

  factory Ticket.fromJson(Map<String, dynamic> json) {
    String pick(List<String> keys) {
      for (final k in keys) {
        final v = json[k];
        if (v != null && v.toString().trim().isNotEmpty) return v.toString().trim();
      }
      return '';
    }

    double? pickDouble(List<String> keys) {
      for (final k in keys) {
        final v = json[k];
        if (v == null) continue;
        final parsed = double.tryParse(v.toString());
        if (parsed != null) return parsed;
      }
      return null;
    }

    return Ticket(
      ticket: json['ticket']?.toString() ?? '',
      nama: json['nama']?.toString() ?? '',
      status: json['status']?.toString() ?? 'OPEN',
      area: json['area']?.toString() ?? '',
      idPelanggan: pick(const [
        'idPelanggan', 'idpel', 'idPel', 'id_pelanggan', 'IDPEL', 'noPelanggan', 'no_pelanggan',
      ]),
      alamat: pick(const ['alamat', 'ALAMAT', 'address']),
      telepon: pick(const [
        'telepon', 'telp', 'noHp', 'no_hp', 'hp', 'whatsapp', 'wa', 'phone', 'noTelepon', 'TELEPON',
      ]),
      kendala: pick(const [
        'kendala', 'keluhan', 'masalah', 'deskripsi', 'keterangan', 'pengaduan', 'KENDALA',
      ]),
      lat: pickDouble(const ['lat', 'latitude', 'LAT']),
      lng: pickDouble(const ['lng', 'lon', 'longitude', 'LNG']),
    );
  }

  Map<String, dynamic> toJson() => {
    'ticket': ticket,
    'nama': nama,
    'status': status,
    'area': area,
    'idPelanggan': idPelanggan,
    'alamat': alamat,
    'telepon': telepon,
    'kendala': kendala,
    'lat': lat,
    'lng': lng,
  };


  @override
  bool operator ==(Object other) =>
      identical(this, other) || (other is Ticket && other.ticket == ticket);

  @override
  int get hashCode => ticket.hashCode;
}
