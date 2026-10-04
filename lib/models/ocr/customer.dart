class Customer {
  final String id;
  final String noPelanggan;
  final String? noMeter;
  final String nama;
  final String? alamat;
  final String? kelurahan;
  final String? kecamatan;
  final double? lat;
  final double? lng;
  final int standAwal;
  final int? standBulanLalu;
  final String? bulanLalu;
  final String? tarif;
  final String? aktif;
  final String? sektor;
  final String? petugas;
  final String? linkFoto;
  final int? sheetRowNumber;

  const Customer({
    required this.id,
    required this.noPelanggan,
    this.noMeter,
    required this.nama,
    this.alamat,
    this.kelurahan,
    this.kecamatan,
    this.lat,
    this.lng,
    this.standAwal = 0,
    this.standBulanLalu,
    this.bulanLalu,
    this.tarif,
    this.aktif,
    this.sektor,
    this.petugas,
    this.linkFoto,
    this.sheetRowNumber,
  });

  factory Customer.fromJson(Map<String, dynamic> j) => Customer(
    id: j['id']?.toString() ?? '',
    noPelanggan: j['no_pelanggan']?.toString() ?? j['noPelanggan']?.toString() ?? '',
    noMeter: j['no_meter']?.toString() ?? j['noMeter']?.toString(),
    nama: j['nama']?.toString() ?? '',
    alamat: j['alamat']?.toString(),
    kelurahan: j['kelurahan']?.toString(),
    kecamatan: j['kecamatan']?.toString(),
    lat: j['lat'] != null ? double.tryParse(j['lat'].toString()) : null,
    lng: j['lng'] != null ? double.tryParse(j['lng'].toString()) : null,
    standAwal: int.tryParse(j['stand_awal']?.toString() ?? j['standAwal']?.toString() ?? '0') ?? 0,
    standBulanLalu: j['stand_bulan_lalu'] != null
        ? int.tryParse(j['stand_bulan_lalu'].toString())
        : (j['standBulanLalu'] != null
            ? int.tryParse(j['standBulanLalu'].toString())
            : null),
    bulanLalu: j['bulan_lalu']?.toString() ?? j['bulanLalu']?.toString(),
    tarif: j['tarip']?.toString() ?? j['tarif']?.toString(),
    aktif: j['aktif']?.toString(),
    sektor: j['sektor']?.toString() ?? j['Sektor']?.toString(),
    petugas: j['petugas']?.toString() ?? j['Petugas']?.toString(),
    linkFoto: j['link']?.toString() ?? j['Link']?.toString() ?? j['linkFoto']?.toString(),
    sheetRowNumber: j['sheet_row_number'] != null
        ? int.tryParse(j['sheet_row_number'].toString())
        : (j['sheetRowNumber'] != null ? int.tryParse(j['sheetRowNumber'].toString()) : null),
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'no_pelanggan': noPelanggan,
    'no_meter': noMeter,
    'nama': nama,
    'alamat': alamat,
    'kelurahan': kelurahan,
    'kecamatan': kecamatan,
    'lat': lat,
    'lng': lng,
    'stand_awal': standAwal,
    'stand_bulan_lalu': standBulanLalu,
    'bulan_lalu': bulanLalu,
    'tarif': tarif,
    'aktif': aktif,
    'sektor': sektor,
    'petugas': petugas,
    'link': linkFoto,
    'sheet_row_number': sheetRowNumber,
  };

  String get labelBulanLalu {
    if (bulanLalu == null) return 'Belum ada data';
    final parts = bulanLalu!.split('-');
    if (parts.length < 2) return bulanLalu!;
    const bln = [
      '',
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'Mei',
      'Jun',
      'Jul',
      'Ags',
      'Sep',
      'Okt',
      'Nov',
      'Des',
    ];
    final idx = int.tryParse(parts[1]) ?? 0;
    if (idx < 1 || idx > 12) return bulanLalu!;
    return '${bln[idx]} ${parts[0]}';
  }

  String get previousStandLabel {
    final value = bulanLalu?.trim();
    if (value == null || value.isEmpty) {
      return 'Stand Sebelumnya';
    }

    if (RegExp(r'^\d{4}-\d{2}$').hasMatch(value)) {
      return 'Stand $labelBulanLalu';
    }

    return value;
  }

  String get sektorLabel {
    final value = sektor?.trim();
    return value == null || value.isEmpty ? 'Tanpa sektor' : value;
  }
}
