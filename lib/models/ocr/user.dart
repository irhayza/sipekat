class User {
  final String id;
  final String nama;
  final String email;
  final String? noHp;

  const User({required this.id, required this.nama, required this.email, this.noHp});

  factory User.fromJson(Map<String, dynamic> j) => User(
    id:    j['id']?.toString() ?? '',
    nama:  j['nama']?.toString() ?? '',
    email: j['email']?.toString() ?? '',
    noHp:  j['no_hp']?.toString(),
  );

  factory User.fromTokenPayload(Map<String, dynamic> p) => User(
    id:    p['id']?.toString() ?? '',
    nama:  p['nama']?.toString() ?? '',
    email: p['email']?.toString() ?? '',
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'nama': nama,
    'email': email,
    if (noHp != null) 'no_hp': noHp,
  };
}
