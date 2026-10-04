import 'package:geolocator/geolocator.dart';

enum LocationReadyStatus {
  ready,
  serviceDisabled,
  permissionDenied,
  permissionDeniedForever,
}

class KunjunganLocationService {
  KunjunganLocationService._();

  static Future<LocationReadyStatus> ensurePermission() async {
    final serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      return LocationReadyStatus.serviceDisabled;
    }

    LocationPermission permission = await Geolocator.checkPermission();

    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }

    if (permission == LocationPermission.denied) {
      return LocationReadyStatus.permissionDenied;
    }
    if (permission == LocationPermission.deniedForever) {
      return LocationReadyStatus.permissionDeniedForever;
    }

    return LocationReadyStatus.ready;
  }

  static Future<Position> getCurrentPositionSafely({
    Duration timeLimit = const Duration(seconds: 15),
  }) async {
    final status = await ensurePermission();

    switch (status) {
      case LocationReadyStatus.serviceDisabled:
        throw Exception(
            'GPS/Layanan lokasi perangkat tidak aktif. Silakan aktifkan GPS terlebih dahulu.');
      case LocationReadyStatus.permissionDenied:
        throw Exception(
            'Izin lokasi ditolak. Aplikasi memerlukan izin lokasi untuk mencatat koordinat kunjungan.');
      case LocationReadyStatus.permissionDeniedForever:
        throw Exception(
            'Izin lokasi ditolak permanen. Buka Pengaturan > Aplikasi, lalu aktifkan izin Lokasi secara manual.');
      case LocationReadyStatus.ready:
        break;
    }

    return Geolocator.getCurrentPosition(
      desiredAccuracy: LocationAccuracy.high,
      timeLimit: timeLimit,
    );
  }
}
