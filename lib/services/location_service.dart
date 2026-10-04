import 'dart:async';
import 'package:geolocator/geolocator.dart';

enum GpsResult {
  ok,
  serviceDisabled,
  permissionDenied,
  permissionDeniedForever,
  timeout,
  unknown,
}

class LocationOutcome {
  final Position? position;
  final GpsResult result;
  const LocationOutcome(this.position, this.result);
  bool get isOk => result == GpsResult.ok && position != null;
}

class LocationService {
  LocationService._();
  static final LocationService instance = LocationService._();

  Future<LocationOutcome> getCurrentLocation({
    Duration timeLimit = const Duration(seconds: 15),
  }) async {
    final serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      return const LocationOutcome(null, GpsResult.serviceDisabled);
    }

    LocationPermission permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.denied) {
      return const LocationOutcome(null, GpsResult.permissionDenied);
    }
    if (permission == LocationPermission.deniedForever) {
      return const LocationOutcome(null, GpsResult.permissionDeniedForever);
    }

    try {
      final pos = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        timeLimit: timeLimit,
      );
      return LocationOutcome(pos, GpsResult.ok);
    } on TimeoutException {
      final last = await Geolocator.getLastKnownPosition();
      if (last != null) return LocationOutcome(last, GpsResult.ok);
      return const LocationOutcome(null, GpsResult.timeout);
    } catch (_) {
      return const LocationOutcome(null, GpsResult.unknown);
    }
  }

  String messageFor(GpsResult r) {
    switch (r) {
      case GpsResult.serviceDisabled:
        return 'GPS perangkat tidak aktif. Laporan tetap bisa dikirim tanpa koordinat.';
      case GpsResult.permissionDenied:
        return 'Izin lokasi ditolak. Laporan tetap bisa dikirim tanpa koordinat.';
      case GpsResult.permissionDeniedForever:
        return 'Izin lokasi diblokir permanen. Aktifkan lewat Pengaturan aplikasi jika ingin menyertakan koordinat.';
      case GpsResult.timeout:
        return 'Sinyal GPS lemah. Laporan tetap bisa dikirim tanpa koordinat.';
      case GpsResult.unknown:
        return 'Lokasi tidak tersedia. Laporan tetap bisa dikirim tanpa koordinat.';
      case GpsResult.ok:
        return 'Lokasi berhasil dikunci.';
    }
  }
}
