class Officer {
  final String nama;
  final String area;

  const Officer({required this.nama, required this.area});

  factory Officer.fromJson(Map<String, dynamic> json) {
    return Officer(
      nama: json['nama']?.toString() ?? '',
      area: json['area']?.toString() ?? '',
    );
  }

  Map<String, dynamic> toJson() => {
    'nama': nama,
    'area': area,
  };


  @override
  bool operator ==(Object other) =>
      identical(this, other) || (other is Officer && other.nama == nama && other.area == area);

  @override
  int get hashCode => Object.hash(nama, area);
}
