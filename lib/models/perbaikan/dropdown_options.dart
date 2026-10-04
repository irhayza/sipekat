import 'officer.dart';

class DropdownOptions {
  final List<Officer> petugas;
  final List<String> jenis;
  final List<String> penanganan;

  const DropdownOptions({
    required this.petugas,
    required this.jenis,
    required this.penanganan,
  });

  factory DropdownOptions.fromJson(Map<String, dynamic> json) {
    return DropdownOptions(
      petugas: ((json['petugas'] as List?) ?? [])
          .map((p) => Officer.fromJson(p.cast<String, dynamic>()))
          .toList(),
      jenis: List<String>.from(json['jenis'] ?? []),
      penanganan: List<String>.from(json['penanganan'] ?? []),
    );
  }

  Map<String, dynamic> toJson() => {
    'petugas': petugas.map((p) => p.toJson()).toList(),
    'jenis': jenis,
    'penanganan': penanganan,
  };

}
