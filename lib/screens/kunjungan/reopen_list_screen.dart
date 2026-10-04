import 'dart:async';

import 'package:flutter/material.dart';
import '../../services/app_session_cache.dart';
import '../../services/kunjungan_api_service.dart';
import '../../services/kunjungan_local_cache_service.dart';
import '../../theme/app_theme.dart';
import '../../widgets/customer_list_tile.dart';
import '../../widgets/empty_state.dart';
import 'reopen_detail_screen.dart';

/// Daftar pelanggan untuk modul Pembukaan (reopen segel/cabut).
/// Mengikuti pola cache yang sama dengan [VisitListScreen]: cache sesi ->
/// server -> cache offline lokal.
class ReopenListScreen extends StatefulWidget {
  final String userName;
  final String userRole;

  const ReopenListScreen({
    super.key,
    required this.userName,
    required this.userRole,
  });

  @override
  State<ReopenListScreen> createState() => _ReopenListScreenState();
}

class _ReopenListScreenState extends State<ReopenListScreen> {
  List<VisitCustomer> _customers = [];
  List<VisitCustomer> _filteredCustomers = [];
  bool _loading = true;
  bool _fromOfflineCache = false;
  String? _errorMsg;
  final _searchCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _fetchList();
    _searchCtrl.addListener(() => _filterSearch(_searchCtrl.text));
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _fetchList({bool forceRefresh = false}) async {
    setState(() {
      _loading = true;
      _errorMsg = null;
      _fromOfflineCache = false;
    });

    final cache = AppSessionCache.instance;

    if (!forceRefresh && cache.pembukaanList.isNotEmpty && cache.pembukaanError == null) {
      final list = cache.pembukaanList;
      unawaited(KunjunganLocalCacheService.instance.saveReopenList(list));
      setState(() {
        _customers = list;
        _filteredCustomers = list;
        _loading = false;
      });
      _filterSearch(_searchCtrl.text);
      return;
    }

    try {
      await cache.refreshPembukaan();
      if (cache.pembukaanError != null) throw ApiException(cache.pembukaanError!);
      final list = cache.pembukaanList;
      await KunjunganLocalCacheService.instance.saveReopenList(list);
      if (!mounted) return;
      setState(() {
        _customers = list;
        _filteredCustomers = list;
        _loading = false;
      });
      _filterSearch(_searchCtrl.text);
    } catch (e) {
      final cachedList = await KunjunganLocalCacheService.instance.getCachedReopenList();
      if (!mounted) return;
      if (cachedList.isNotEmpty) {
        setState(() {
          _customers = cachedList;
          _filteredCustomers = cachedList;
          _loading = false;
          _fromOfflineCache = true;
        });
        _filterSearch(_searchCtrl.text);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Menampilkan daftar pembukaan dari memori lokal (Offline).'),
            duration: Duration(seconds: 3),
          ),
        );
      } else {
        setState(() {
          _errorMsg = 'Gagal memuat daftar pembukaan: $e';
          _loading = false;
        });
      }
    }
  }

  void _filterSearch(String query) {
    final q = query.toLowerCase();
    setState(() {
      _filteredCustomers = _customers.where((c) {
        return c.idpel.toLowerCase().contains(q) || c.nama.toLowerCase().contains(q);
      }).toList();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.canvas,
      appBar: AppBar(
        title: const Text('Daftar Pembukaan', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh_rounded),
            onPressed: _loading ? null : () => _fetchList(forceRefresh: true),
            tooltip: 'Segarkan',
          ),
        ],
      ),
      body: Column(
        children: [
          if (_fromOfflineCache)
            Container(
              width: double.infinity,
              color: AppColors.prosesBg,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: const Row(
                children: [
                  Icon(Icons.cloud_off_rounded, size: 14, color: AppColors.proses),
                  SizedBox(width: 8),
                  Text('Mode offline — data terakhir tersimpan', style: TextStyle(fontSize: 11.5, color: AppColors.proses)),
                ],
              ),
            ),
          Padding(
            padding: const EdgeInsets.all(12),
            child: TextField(
              controller: _searchCtrl,
              decoration: InputDecoration(
                hintText: 'Cari IDPEL atau Nama Pelanggan...',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _searchCtrl.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () => _searchCtrl.clear(),
                      )
                    : null,
              ),
            ),
          ),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _errorMsg != null
                    ? EmptyState(
                        icon: Icons.cloud_off_rounded,
                        title: 'Gagal memuat data',
                        message: _errorMsg!,
                        actionLabel: 'Coba Lagi',
                        onAction: () => _fetchList(forceRefresh: true),
                      )
                    : _filteredCustomers.isEmpty
                        ? const EmptyState(
                            icon: Icons.inbox_rounded,
                            title: 'Tidak Ada Data',
                            message: 'Tidak ada data pembukaan segel/cabut untuk saat ini.',
                          )
                        : RefreshIndicator(
                            onRefresh: () => _fetchList(forceRefresh: true),
                            child: ListView.builder(
                              padding: const EdgeInsets.symmetric(horizontal: 12),
                              itemCount: _filteredCustomers.length,
                              itemBuilder: (ctx, index) {
                                final c = _filteredCustomers[index];
                                return CustomerListTile(
                                  customer: c,
                                  accent: AppColors.pengaduan,
                                  isReopen: true,
                                  onTap: () {
                                    Navigator.push(
                                      context,
                                      MaterialPageRoute<void>(
                                        builder: (_) => ReopenDetailScreen(customer: c),
                                      ),
                                    ).then((_) => _fetchList(forceRefresh: true));
                                  },
                                );
                              },
                            ),
                          ),
          ),
        ],
      ),
    );
  }
}
