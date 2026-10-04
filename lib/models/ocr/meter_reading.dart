import 'package:uuid/uuid.dart';

class MeterReading {
  final String   id;
  final String   pelangganId;
  final String   petugasId;
  final int      standAngka;
  final String?  fotoPath;
        String?  fotoUrl;
        String?  fotoDriveId;
  final double?  lat;
  final double?  lng;
  final String?  catatan;
  final String   bulan;
  final DateTime createdAt;
        bool     isSynced;
  final int      minus;

  MeterReading({
    String? id,
    required this.pelangganId,
    required this.petugasId,
    required this.standAngka,
    this.fotoPath,
    this.fotoUrl,
    this.fotoDriveId,
    this.lat,
    this.lng,
    this.catatan,
    String?   bulan,
    DateTime? createdAt,
    this.isSynced = false,
    this.minus = 0,
  })  : id        = id ?? const Uuid().v4(),
        bulan     = bulan ?? DateTime.now().toIso8601String().substring(0, 7),
        createdAt = createdAt ?? DateTime.now();

  factory MeterReading.fromJson(Map<String, dynamic> j) => MeterReading(
    id:          j['id']?.toString() ?? const Uuid().v4(),
    pelangganId: j['pelanggan_id']?.toString() ?? '',
    petugasId:   j['petugas_id']?.toString() ?? '',
    standAngka:  int.tryParse(j['stand_angka']?.toString() ?? '0') ?? 0,
    fotoUrl:     j['foto_url']?.toString(),
    fotoDriveId: j['foto_gdrive_id']?.toString(),
    lat:         j['lat'] != null ? double.tryParse(j['lat'].toString()) : null,
    lng:         j['lng'] != null ? double.tryParse(j['lng'].toString()) : null,
    catatan:     j['catatan']?.toString(),
    bulan:       j['bulan']?.toString() ?? DateTime.now().toIso8601String().substring(0, 7),
    createdAt:   j['created_at'] != null
                   ? DateTime.tryParse(j['created_at'].toString()) ?? DateTime.now()
                   : DateTime.now(),
    isSynced: true,
    minus:       int.tryParse(j['minus']?.toString() ?? '0') ?? 0,
  );

  factory MeterReading.fromLocalDb(Map<String, dynamic> m) => MeterReading(
    id:          m['id']?.toString() ?? '',
    pelangganId: m['pelanggan_id']?.toString() ?? '',
    petugasId:   m['petugas_id']?.toString() ?? '',
    standAngka:  int.tryParse(m['stand_angka']?.toString() ?? '0') ?? 0,
    fotoPath:    m['foto_path']?.toString(),
    fotoUrl:     m['foto_url']?.toString(),
    fotoDriveId: m['foto_drive_id']?.toString(),
    lat:         m['lat'] != null ? double.tryParse(m['lat'].toString()) : null,
    lng:         m['lng'] != null ? double.tryParse(m['lng'].toString()) : null,
    catatan:     m['catatan']?.toString(),
    bulan:       m['bulan']?.toString() ?? '',
    createdAt:   DateTime.tryParse(m['created_at']?.toString() ?? '') ?? DateTime.now(),
    isSynced:    (m['is_synced'] ?? 0) == 1,
    minus:       int.tryParse(m['minus']?.toString() ?? '0') ?? 0,
  );

  Map<String, dynamic> toLocalDb() => {
    'id': id, 'pelanggan_id': pelangganId, 'petugas_id': petugasId,
    'stand_angka': standAngka, 'foto_path': fotoPath, 'foto_url': fotoUrl,
    'foto_drive_id': fotoDriveId, 'lat': lat, 'lng': lng,
    'catatan': catatan, 'bulan': bulan,
    'created_at': createdAt.toIso8601String(), 'is_synced': isSynced ? 1 : 0,
    'minus': minus,
  };

  Map<String, dynamic> toApiJson() => {
    'id': id, 'pelanggan_id': pelangganId, 'stand_angka': standAngka,
    'foto_url': fotoUrl, 'foto_gdrive_id': fotoDriveId,
    'lat': lat, 'lng': lng, 'catatan': catatan, 'bulan': bulan,
    'minus': minus,
  };
}
