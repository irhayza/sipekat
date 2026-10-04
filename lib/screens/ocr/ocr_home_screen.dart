import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import '../../models/ocr/customer.dart';
import '../../models/ocr/user.dart';
import '../../models/ocr/meter_reading.dart';
import '../../services/ocr_api_service.dart';
import '../../services/ocr_local_db_service.dart';
import '../../services/ocr_sync_service.dart';
import '../../services/app_session_cache.dart';
import '../../theme/app_theme.dart';
import 'meter_reading_screen.dart';

class OcrHomeScreen extends StatefulWidget {
  final String userNama;
  final String userEmail;

  const OcrHomeScreen({
    super.key,
    required this.userNama,
    required this.userEmail,
  });

  @override
  State<OcrHomeScreen> createState() => _OcrHomeScreenState();
}

class _OcrHomeScreenState extends State<OcrHomeScreen> {
  StreamSubscription<int>? _syncSubscription;
  List<Customer> _assignedCustomers = [];
  bool _loadingAssignments = true;
  String? _loadErr;
  String? _selectedSektor;
  String? _selectedIdpel;
  int _unsynced = 0;
  int _currentIndex = 0;

  bool get _canManualSync => OcrApiService.instance.canSyncPendingReadings;

  List<String> get _sektorOptions {
    final sectors = _assignedCustomers.map((customer) => customer.sektorLabel).toSet().toList();
    sectors.sort();
    return sectors;
  }

  List<Customer> get _customersBySektor {
    if (_selectedSektor == null) return const <Customer>[];
    return _assignedCustomers
        .where((customer) => customer.sektorLabel == _selectedSektor)
        .toList();
  }

  Customer? get _selectedCustomer {
    final idpel = _selectedIdpel;
    if (idpel == null) return null;
    for (final customer in _customersBySektor) {
      if (customer.noPelanggan == idpel) {
        return customer;
      }
    }
    return null;
  }

  User get _currentUser => User(
        id: widget.userEmail,
        nama: widget.userNama,
        email: widget.userEmail,
      );

  @override
  void initState() {
    super.initState();
    _loadUnsyncedCount();
    _loadInitialAssignedCustomers();
    _syncSubscription = OcrSyncService.instance.autoSync.listen((count) {
      if (count > 0) {
        _loadUnsyncedCount();
        _loadAssignedCustomers();
      }
    });
  }

  @override
  void dispose() {
    _syncSubscription?.cancel();
    super.dispose();
  }

  Future<void> _loadUnsyncedCount() async {
    final count = await OcrLocalDbService.instance.countUnsynced();
    if (mounted) {
      setState(() => _unsynced = count);
    }
  }

  /// Dipanggil sekali dari initState. Bila cache sesi (diisi saat login)
  /// sudah punya daftar pelanggan, tampilkan langsung tanpa menunggu
  /// jaringan, lalu segarkan diam-diam di latar belakang. Bila cache masih
  /// kosong (mis. gagal saat login), baru ambil dari server seperti biasa.
  Future<void> _loadInitialAssignedCustomers() async {
    final cached = AppSessionCache.instance.ocrCustomers;
    if (cached.isNotEmpty) {
      final unsyncedReadings = await OcrLocalDbService.instance.getUnsynced();
      final unsyncedIdpels = unsyncedReadings.map((r) => r.pelangganId).toSet();
      final customers = cached.where((c) => !unsyncedIdpels.contains(c.noPelanggan)).toList();

      if (mounted) {
        final sectors = customers.map((customer) => customer.sektorLabel).toSet();
        setState(() {
          _assignedCustomers = customers;
          _selectedSektor = sectors.contains(_selectedSektor) ? _selectedSektor : null;
          _selectedIdpel = customers.any((c) => c.noPelanggan == _selectedIdpel) ? _selectedIdpel : null;
          _loadingAssignments = false;
        });
      }
      unawaited(_refreshAssignedCustomersSilently());
      return;
    }
    await _loadAssignedCustomers();
  }

  Future<void> _loadAssignedCustomers() async {
    setState(() {
      _loadingAssignments = true;
      _loadErr = null;
    });

    try {
      var customers = await OcrApiService.instance.getAssignedCustomers(widget.userNama);
      AppSessionCache.instance.ocrCustomers = customers; // segarkan cache sesi juga

      final unsyncedReadings = await OcrLocalDbService.instance.getUnsynced();
      final unsyncedIdpels = unsyncedReadings.map((r) => r.pelangganId).toSet();
      customers = customers.where((c) => !unsyncedIdpels.contains(c.noPelanggan)).toList();

      if (!mounted) return;

      final sectors = customers.map((customer) => customer.sektorLabel).toSet();
      final selectedSektor = sectors.contains(_selectedSektor) ? _selectedSektor : null;
      final selectedIdpel = customers.any((customer) => customer.noPelanggan == _selectedIdpel)
          ? _selectedIdpel
          : null;

      setState(() {
        _assignedCustomers = customers;
        _selectedSektor = selectedSektor;
        _selectedIdpel = selectedIdpel;
        _loadingAssignments = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _loadingAssignments = false;
        _loadErr = error.toString().replaceFirst('Exception: ', '');
      });
    }
  }

  Future<void> _refreshAssignedCustomersSilently() async {
    try {
      var customers = await OcrApiService.instance.getAssignedCustomers(widget.userNama);
      AppSessionCache.instance.ocrCustomers = customers; // segarkan cache sesi juga

      final unsyncedReadings = await OcrLocalDbService.instance.getUnsynced();
      final unsyncedIdpels = unsyncedReadings.map((r) => r.pelangganId).toSet();
      customers = customers.where((c) => !unsyncedIdpels.contains(c.noPelanggan)).toList();

      if (!mounted) return;

      final sectors = customers.map((customer) => customer.sektorLabel).toSet();
      final selectedSektor = sectors.contains(_selectedSektor) ? _selectedSektor : null;
      final selectedIdpel = customers.any((customer) => customer.noPelanggan == _selectedIdpel)
          ? _selectedIdpel
          : null;

      setState(() {
        _assignedCustomers = customers;
        _selectedSektor = selectedSektor;
        _selectedIdpel = selectedIdpel;
        _loadErr = null;
      });
    } catch (error) {
      if (kDebugMode) debugPrint('Silent refresh failed: $error');
    }
  }

  Future<void> _manualSync() async {
    if (!_canManualSync) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Sinkronisasi Google belum aktif. Isi AppConfig.googleBridgeUrl terlebih dahulu.',
          ),
        ),
      );
      return;
    }

    final count = await OcrSyncService.instance.syncAll();
    if (!mounted) return;

    await _loadUnsyncedCount();
    await _loadAssignedCustomers();
    if (!mounted) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          count > 0
              ? '$count data berhasil disinkronisasi ke OCRDAPEL'
              : 'Tidak ada data yang berhasil disinkronisasi',
        ),
      ),
    );
  }

  Future<void> _openSelectedCustomer() async {
    final customer = _selectedCustomer;
    if (customer == null) return;

    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => MeterReadingScreen(
          customer: customer,
          officerNama: widget.userNama,
          officerEmail: widget.userEmail,
        ),
      ),
    );
    await _loadUnsyncedCount();
    await _refreshAssignedCustomersSilently();
  }

  void _showCustomersListSheet(BuildContext context) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return Container(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'Daftar Pelanggan Penugasan',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: AppColors.meter,
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close),
                    onPressed: () => Navigator.pop(context),
                  ),
                ],
              ),
              const Divider(),
              Expanded(
                child: _assignedCustomers.isEmpty
                    ? const Center(
                        child: Text('Tidak ada pelanggan penugasan'),
                      )
                    : ListView.builder(
                        itemCount: _assignedCustomers.length,
                        itemBuilder: (context, index) {
                          final cust = _assignedCustomers[index];
                          return ListTile(
                            leading: Container(
                              padding: const EdgeInsets.all(8),
                              decoration: BoxDecoration(
                                color: AppColors.meter.withValues(alpha: 0.1),
                                shape: BoxShape.circle,
                              ),
                              child: const Icon(
                                  Icons.person,
                                  color: AppColors.meter,
                                ),
                            ),
                            title: Text(
                              cust.nama,
                              style: const TextStyle(fontWeight: FontWeight.bold),
                            ),
                            subtitle: Text('IDPEL: ${cust.noPelanggan} | Sektor: ${cust.sektorLabel}'),
                            trailing: const Icon(Icons.chevron_right),
                            onTap: () {
                              Navigator.pop(context);
                              setState(() {
                                _selectedSektor = cust.sektorLabel;
                                _selectedIdpel = cust.noPelanggan;
                              });
                            },
                          );
                        },
                      ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildMenuGrid(BuildContext context) {
    final menuItems = [
      {
        'icon': Icons.qr_code_scanner_rounded,
        'label': 'Input Stand',
        'color': AppColors.meter,
        'action': () {},
      },
      {
        'icon': Icons.people_rounded,
        'label': 'Pelanggan',
        'color': AppColors.meter,
        'action': () => _showCustomersListSheet(context),
      },
      {
        'icon': Icons.cloud_upload_outlined,
        'label': 'Sync Offline',
        'color': AppColors.selesai,
        'action': _manualSync,
      },
      {
        'icon': Icons.history_rounded,
        'label': 'Riwayat',
        'color': AppColors.proses,
        'action': () {
          setState(() => _currentIndex = 1);
        },
      },
    ];

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 12),
      child: GridView.builder(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 4,
          mainAxisSpacing: 12,
          crossAxisSpacing: 8,
          childAspectRatio: 0.85,
        ),
        itemCount: menuItems.length,
        itemBuilder: (context, index) {
          final item = menuItems[index];
          return InkWell(
            onTap: item['action'] as VoidCallback,
            borderRadius: BorderRadius.circular(12),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Container(
                  width: 50,
                  height: 50,
                  decoration: BoxDecoration(
                    color: (item['color'] as Color).withValues(alpha: 0.10),
                    shape: BoxShape.circle,
                    border: Border.all(
                      color: (item['color'] as Color).withValues(alpha: 0.25),
                      width: 1.5,
                    ),
                  ),
                  child: Icon(
                    item['icon'] as IconData,
                    color: item['color'] as Color,
                    size: 22,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  item['label'] as String,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.w500,
                    color: AppColors.ink,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildBerandaTab(User user) {
    return RefreshIndicator(
      onRefresh: () async {
        await _loadAssignedCustomers();
        await _loadUnsyncedCount();
      },
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildMenuGrid(context),
          const SizedBox(height: 12),

          if (_loadingAssignments)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 40),
              child: Center(child: CircularProgressIndicator()),
            )
          else if (_loadErr != null)
            _RetryCard(
              message: _loadErr!,
              onRetry: _loadAssignedCustomers,
            )
          else if (_assignedCustomers.isEmpty)
            const _EmptyCard(
              title: 'Belum ada pelanggan untuk petugas ini',
              description:
                  'Periksa kolom Petugas pada sheet OCRDAPEL agar sesuai dengan nama petugas pada sheet LOGIN.',
            )
          else
            _SelectorCard(
              sektorOptions: _sektorOptions,
              selectedSektor: _selectedSektor,
              onSektorChanged: (value) {
                setState(() {
                  _selectedSektor = value;
                  _selectedIdpel = null;
                });
              },
              customersBySektor: _customersBySektor,
              selectedIdpel: _selectedIdpel,
              onIdpelChanged: (value) {
                setState(() => _selectedIdpel = value);
                if (value != null) {
                  final customer = _customersBySektor.firstWhere((c) => c.noPelanggan == value);
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => MeterReadingScreen(
                        customer: customer,
                        officerNama: widget.userNama,
                        officerEmail: widget.userEmail,
                      ),
                    ),
                  ).then((_) {
                    _loadUnsyncedCount();
                    _refreshAssignedCustomersSilently();
                  });
                }
              },
            ),

          if (_selectedCustomer != null) ...[
            const SizedBox(height: 12),
            _SelectedCustomerCard(
              customer: _selectedCustomer!,
              onOpen: _openSelectedCustomer,
            ),
          ],
          
          if (!_canManualSync) ...[
            const SizedBox(height: 16),
            const _InfoBanner(
              icon: Icons.info_outline,
              color: AppColors.meter,
              background: Color(0xFFFDE8E8),
              border: Color(0xFFFBD5D5),
              message:
                  'Data dibaca dari Google Sheet, tetapi sinkronisasi kirim ke OCRDAPEL dan Google Drive baru aktif jika AppConfig.googleBridgeUrl diisi.',
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildRiwayatTab() {
    return FutureBuilder<List<MeterReading>>(
      future: OcrLocalDbService.instance.getAllReadings(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return Center(child: Text('Gagal memuat riwayat: ${snapshot.error}'));
        }
        final list = snapshot.data ?? [];
        if (list.isEmpty) {
          return const Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.history_rounded, size: 64, color: AppColors.muted),
                SizedBox(height: 12),
                Text(
                  'Belum ada riwayat pembacaan.',
                  style: TextStyle(color: AppColors.muted),
                ),
              ],
            ),
          );
        }

        return RefreshIndicator(
          onRefresh: () async {
            setState(() {});
          },
          child: ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: list.length,
            itemBuilder: (context, index) {
              final reading = list[index];
              return Card(
                margin: const EdgeInsets.only(bottom: 12),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: reading.isSynced
                              ? AppColors.selesai.withValues(alpha: 0.12)
                              : AppColors.proses.withValues(alpha: 0.12),
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          reading.isSynced ? Icons.cloud_done : Icons.cloud_off,
                          color: reading.isSynced ? AppColors.selesai : AppColors.proses,
                          size: 24,
                        ),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'IDPEL: ${reading.pelangganId}',
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 14,
                                color: AppColors.ink,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'Tanggal: ${reading.createdAt.day}-${reading.createdAt.month}-${reading.createdAt.year} ${reading.createdAt.hour.toString().padLeft(2, '0')}:${reading.createdAt.minute.toString().padLeft(2, '0')}',
                              style: const TextStyle(fontSize: 11, color: AppColors.muted),
                            ),
                            if (reading.catatan != null) ...[
                              const SizedBox(height: 4),
                              Text(
                                'Catatan: ${reading.catatan}',
                                style: const TextStyle(fontSize: 11, color: AppColors.muted),
                              ),
                            ]
                          ],
                        ),
                      ),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text(
                            '${reading.standAngka}',
                            style: const TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                              color: AppColors.meter,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            reading.isSynced ? 'Tersinkron' : 'Offline',
                            style: TextStyle(
                              fontSize: 10,
                              fontWeight: FontWeight.bold,
                              color: reading.isSynced
                                  ? AppColors.selesai
                                  : AppColors.proses,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(width: 8),
                      IconButton(
                        icon: const Icon(Icons.delete_outline, color: AppColors.danger, size: 20),
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                        onPressed: () async {
                          final confirm = await showDialog<bool>(
                            context: context,
                            builder: (ctx) => AlertDialog(
                              title: const Text('Hapus Riwayat'),
                              content: const Text('Apakah Anda yakin ingin menghapus data bacaan ini dari riwayat lokal?'),
                              actions: [
                                TextButton(
                                  onPressed: () => Navigator.pop(ctx, false),
                                  child: const Text('Batal'),
                                ),
                                TextButton(
                                  onPressed: () => Navigator.pop(ctx, true),
                                  child: const Text('Hapus', style: TextStyle(color: AppColors.danger)),
                                ),
                              ],
                            ),
                          );
                          if (confirm == true) {
                            await OcrLocalDbService.instance.deleteReading(reading.id);
                            setState(() {});
                          }
                        },
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
        );
      },
    );
  }

  Widget _buildPesanTab() {
    final messages = [
      {
        'title': 'Penugasan Baru Tersedia',
        'desc': 'Data penugasan sektor baru telah dimuat. Silakan periksa daftar penugasan Anda.',
        'time': 'Baru saja',
        'icon': Icons.assignment_turned_in_rounded,
      },
      {
        'title': 'Sinkronisasi Berhasil',
        'desc': 'Data pembacaan stand meter berhasil dikirim ke server google sheet.',
        'time': '1 jam yang lalu',
        'icon': Icons.check_circle_rounded,
      },
      {
        'title': 'Tips Membaca OCR',
        'desc': 'Posisikan kamera sejajar dengan angka meter dan pastikan cahaya cukup untuk hasil maksimal.',
        'time': 'Kemarin',
        'icon': Icons.tips_and_updates_rounded,
      },
    ];

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: messages.length,
      itemBuilder: (context, index) {
        final msg = messages[index];
        return Card(
          margin: const EdgeInsets.only(bottom: 12),
          child: ListTile(
            leading: Icon(
              msg['icon'] as IconData,
              color: AppColors.meter,
              size: 28,
            ),
            title: Text(
              msg['title'] as String,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
            ),
            subtitle: Text(msg['desc'] as String, style: const TextStyle(fontSize: 12)),
            trailing: Text(
              msg['time'] as String,
              style: const TextStyle(fontSize: 9, color: AppColors.muted),
            ),
          ),
        );
      },
    );
  }

  Widget _buildProfilTab(String petugasName, String email) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        children: [
          const SizedBox(height: 20),
          Center(
            child: CircleAvatar(
              radius: 50,
              backgroundColor: AppColors.meter.withValues(alpha: 0.1),
              child: const Text(
                'P',
                style: TextStyle(
                  fontSize: 48,
                  fontWeight: FontWeight.bold,
                  color: AppColors.meter,
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),
          Text(
            petugasName,
            style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.ink),
          ),
          Text(
            email,
            style: const TextStyle(fontSize: 14, color: AppColors.muted),
          ),
          const SizedBox(height: 32),
          Card(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: const Column(
              children: [
                ListTile(
                  leading: Icon(Icons.security, color: AppColors.meter),
                  title: Text('Keamanan Akun', style: TextStyle(fontSize: 14)),
                  trailing: Icon(Icons.chevron_right),
                ),
                Divider(height: 1),
                ListTile(
                  leading: Icon(Icons.settings, color: AppColors.meter),
                  title: Text('Pengaturan Aplikasi', style: TextStyle(fontSize: 14)),
                  trailing: Icon(Icons.chevron_right),
                ),
                Divider(height: 1),
                ListTile(
                  leading: Icon(Icons.help_outline, color: AppColors.meter),
                  title: Text('Pusat Bantuan', style: TextStyle(fontSize: 14)),
                  trailing: Icon(Icons.chevron_right),
                ),
              ],
            ),
          ),
          const SizedBox(height: 32),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.meter,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
              onPressed: () => Navigator.pop(context),
              icon: const Icon(Icons.arrow_back_rounded),
              label: const Text('Kembali ke Dashboard Utama', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold)),
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final user = _currentUser;

    return Scaffold(
      appBar: PreferredSize(
        preferredSize: const Size.fromHeight(135),
        child: Container(
          decoration: const BoxDecoration(
            color: AppColors.meter,
            borderRadius: BorderRadius.only(
              bottomLeft: Radius.circular(20),
              bottomRight: Radius.circular(20),
            ),
          ),
          padding: const EdgeInsets.only(top: 40, left: 20, right: 20, bottom: 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'Pencatatan Meter',
                    style: TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  Row(
                    children: [
                      if (_canManualSync && _unsynced > 0)
                        IconButton(
                          icon: Badge(
                            label: Text('$_unsynced'),
                            child: const Icon(Icons.cloud_upload_outlined, color: Colors.white),
                          ),
                          tooltip: 'Sinkronisasi data offline',
                          onPressed: _manualSync,
                        ),
                      IconButton(
                        icon: const Icon(Icons.refresh, color: Colors.white),
                        tooltip: 'Muat ulang',
                        onPressed: () async {
                          await _loadAssignedCustomers();
                          await _loadUnsyncedCount();
                        },
                      ),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  IconButton(
                    icon: const Icon(Icons.arrow_back_rounded, color: Colors.white),
                    onPressed: () => Navigator.pop(context),
                    tooltip: 'Kembali ke Dashboard Utama',
                  ),
                  const SizedBox(width: 4),
                  CircleAvatar(
                    radius: 18,
                    backgroundColor: Colors.white.withValues(alpha: 0.2),
                    child: const Text('P', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                  ),
                  const SizedBox(width: 10),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Halo, ${user.nama}',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      Text(
                        'Pelanggan harus dicatat: ${_assignedCustomers.length} | Unsynced: $_unsynced',
                        style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.85),
                          fontSize: 11,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
      body: _buildBerandaTab(user),
    );
  }
}

class _SelectorCard extends StatelessWidget {
  const _SelectorCard({
    required this.sektorOptions,
    required this.selectedSektor,
    required this.onSektorChanged,
    required this.customersBySektor,
    required this.selectedIdpel,
    required this.onIdpelChanged,
  });

  final List<String> sektorOptions;
  final String? selectedSektor;
  final ValueChanged<String?> onSektorChanged;
  final List<Customer> customersBySektor;
  final String? selectedIdpel;
  final ValueChanged<String?> onIdpelChanged;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0.5,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Pilih Sektor & Pelanggan',
              style: TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.bold,
                color: AppColors.meter,
              ),
            ),
            const SizedBox(height: 6),
            const Text(
              'Pilih sektor, lalu pilih IDPEL untuk membuka scanner stand meter.',
              style: TextStyle(color: AppColors.muted, fontSize: 12, height: 1.4),
            ),
            const SizedBox(height: 14),
            DropdownButtonFormField<String>(
              initialValue: selectedSektor,
              decoration: const InputDecoration(
                labelText: 'Sektor',
                labelStyle: TextStyle(fontSize: 13),
                contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                border: OutlineInputBorder(),
              ),
              items: sektorOptions
                  .map(
                    (sektor) => DropdownMenuItem<String>(
                      value: sektor,
                      child: Text(sektor, style: const TextStyle(fontSize: 13)),
                    ),
                  )
                  .toList(),
              onChanged: onSektorChanged,
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              initialValue: selectedIdpel,
              decoration: const InputDecoration(
                labelText: 'IDPEL',
                labelStyle: TextStyle(fontSize: 13),
                contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                border: OutlineInputBorder(),
              ),
              items: customersBySektor
                  .map(
                    (customer) => DropdownMenuItem<String>(
                      value: customer.noPelanggan,
                      child: Text(
                        '${customer.noPelanggan} - ${customer.nama}',
                        style: const TextStyle(fontSize: 13),
                      ),
                    ),
                  )
                  .toList(),
              onChanged: customersBySektor.isEmpty ? null : onIdpelChanged,
            ),
          ],
        ),
      ),
    );
  }
}

class _SelectedCustomerCard extends StatelessWidget {
  const _SelectedCustomerCard({
    required this.customer,
    required this.onOpen,
  });

  final Customer customer;
  final Future<void> Function() onOpen;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0.5,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              customer.nama,
              style: const TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.bold,
                color: AppColors.meter,
              ),
            ),
            const SizedBox(height: 8),
            Text('IDPEL: ${customer.noPelanggan}', style: const TextStyle(fontSize: 12)),
            const SizedBox(height: 4),
            Text('No. Meter: ${customer.noMeter?.isNotEmpty == true ? customer.noMeter : '-'}', style: const TextStyle(fontSize: 12)),
            const SizedBox(height: 4),
            Text('Alamat: ${customer.alamat?.isNotEmpty == true ? customer.alamat : '-'}', style: const TextStyle(fontSize: 12)),
            const SizedBox(height: 4),
            Text(
              'St Lalu: ${customer.standBulanLalu?.toString() ?? customer.standAwal.toString()}',
              style: const TextStyle(fontSize: 12),
            ),
            const SizedBox(height: 14),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.meter,
                  foregroundColor: Colors.white,
                ),
                onPressed: onOpen,
                icon: const Icon(Icons.qr_code_scanner, size: 18),
                label: const Text('Buka Scan Meter', style: TextStyle(fontWeight: FontWeight.bold)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoBanner extends StatelessWidget {
  const _InfoBanner({
    required this.icon,
    required this.color,
    required this.background,
    required this.border,
    required this.message,
  });

  final IconData icon;
  final Color color;
  final Color background;
  final Color border;
  final String message;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: border),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: color, size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              message,
              style: TextStyle(color: color, height: 1.4, fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }
}

class _RetryCard extends StatelessWidget {
  const _RetryCard({
    required this.message,
    required this.onRetry,
  });

  final String message;
  final Future<void> Function() onRetry;

  @override
  Widget build(BuildContext context) {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            const Icon(Icons.cloud_off, size: 48, color: AppColors.muted),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(color: AppColors.muted, fontSize: 12),
            ),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh, size: 16),
              label: const Text('Coba Lagi', style: TextStyle(fontSize: 12)),
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard({
    required this.title,
    required this.description,
  });

  final String title;
  final String description;

  @override
  Widget build(BuildContext context) {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            const Icon(Icons.list_alt_outlined, size: 48, color: AppColors.muted),
            const SizedBox(height: 12),
            Text(
              title,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              description,
              textAlign: TextAlign.center,
              style: const TextStyle(color: AppColors.muted, height: 1.4, fontSize: 12),
            ),
          ],
        ),
      ),
    );
  }
}
