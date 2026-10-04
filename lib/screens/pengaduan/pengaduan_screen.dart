import 'dart:async';
import 'package:si_pekat/services/kunjungan_api_service.dart';
import 'package:si_pekat/config/app_config.dart';
import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../widgets/section_card.dart';
import '../../widgets/success_sheet.dart';
import '../../services/app_session_cache.dart';
import '../../services/pengaduan_api_service.dart';
import '../../services/ocr_api_service.dart';
import '../../utils/contact_launcher.dart';
import '../../utils/sheet_sanitize.dart';
class PengaduanScreen extends StatefulWidget {
  final String userName;
  final String userNama;

  const PengaduanScreen({
    super.key,
    required this.userName,
    required this.userNama,
  });

  @override
  State<PengaduanScreen> createState() => _PengaduanScreenState();
}

class _PengaduanScreenState extends State<PengaduanScreen> {
  final _formKey = GlobalKey<FormState>();
  final _idController = TextEditingController();
  final _namaController = TextEditingController();
  final _telpController = TextEditingController();
  final _alamatController = TextEditingController();
  final _pengaduanController = TextEditingController();

  Timer? _debounce;
  bool _isLoading = false;
  String _idStatus = "";
  Color _idStatusColor = AppColors.muted;
  String? _telpError;
  String? _mrsValue;
  int _charCount = 0;
  final int _maxChars = 200;

  @override
  void initState() {
    super.initState();
    _idController.addListener(_onIdChanged);
    _pengaduanController.addListener(() {
      setState(() => _charCount = _pengaduanController.text.length);
    });
  }

  @override
  void dispose() {
    _idController.dispose();
    _namaController.dispose();
    _telpController.dispose();
    _alamatController.dispose();
    _pengaduanController.dispose();
    _debounce?.cancel();
    super.dispose();
  }

  void _onIdChanged() {
    final val = _idController.text.replaceAll(RegExp(r'\D'), '');
    if (val.isNotEmpty) {
      if (val.length >= 8) {
        final existing = AppSessionCache.instance.perbaikanTickets.where(
          (t) => t.idPelanggan.toLowerCase() == val.toLowerCase() &&
                 t.status.toUpperCase() != 'SELESAI',
        ).toList();
        if (existing.isNotEmpty) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            _showSnackbar('Pelanggan ini masih punya tiket ${existing.first.ticket} berstatus ${existing.first.status}', isError: true);
          });
        }

        setState(() {
          _idStatus = "⏳ Mencari di dbase...";
          _idStatusColor = AppColors.flame;
        });

        if (_debounce?.isActive ?? false) _debounce!.cancel();
        _debounce = Timer(const Duration(milliseconds: 400), () async {
          try {
            final result = await KunjunganApiService.instance.findRow(
              sheetName: AppConfig.ocrSheetName, 
              idpel: val
            );
            if (result.row.isNotEmpty && mounted) {
              setState(() {
                _namaController.text = result.row['NAMA']?.toString() ?? '';
                _alamatController.text = result.row['ALAMAT']?.toString() ?? '';
                _mrsValue = result.row['MRS']?.toString().trim().toUpperCase();
                _idStatus = "✅ Ditemukan";
                _idStatusColor = AppColors.selesai;
              });
              return;
            }
          } catch (e) {
            // Lanjut ke fallback jika gagal
          }

          // Fallback ke findCustomerInDapel atau checkCustomerId jika diperlukan
          try {
            final customer = await OcrApiService.instance.findCustomerInDapel(val);
            if (customer != null && mounted) {
              setState(() {
                _namaController.text = customer.nama;
                _alamatController.text = customer.alamat ?? '';
                _idStatus = "✅ Ditemukan (Dapel)";
                _idStatusColor = AppColors.selesai;
              });
              return;
            }
          } catch (_) {}

          final result = await PengaduanApiService.checkCustomerId(val);
          if (result != null && mounted) {
            setState(() {
              _namaController.text = result['nama'];
              _alamatController.text = result['alamat'];
              _idStatus = "✅ Ditemukan";
              _idStatusColor = AppColors.selesai;
            });
          } else if (mounted) {
            setState(() {
              _idStatus = "❌ Tidak ditemukan — isi manual di bawah";
              _idStatusColor = AppColors.danger;
            });
          }
        });
      } else {
        setState(() {
          _idStatus = "";
        });
      }
    } else {
      setState(() => _idStatus = "");
    }
  }


  bool _validateForm() {
    if (_namaController.text.isEmpty ||
        _alamatController.text.isEmpty ||
        _pengaduanController.text.isEmpty) {
      _showSnackbar('Harap isi semua field wajib.', isError: true);
      return false;
    }

    final val = _idController.text.replaceAll(RegExp(r'\D'), '');
    if (val.isNotEmpty) {
      final existing = AppSessionCache.instance.perbaikanTickets.where(
        (t) => t.idPelanggan.toLowerCase() == val.toLowerCase() &&
               t.status.toUpperCase() != 'SELESAI',
      ).toList();
      if (existing.isNotEmpty) {
        _showSnackbar('Pengaduan ditolak: Pelanggan ini masih memiliki tiket aktif (${existing.first.ticket}) dengan status ${existing.first.status}.', isError: true);
        return false;
      }
    }
    final RegExp telReg = RegExp(r'^(08|\+62|62)\d{7,12}$');
    if (!telReg.hasMatch(_telpController.text.replaceAll(RegExp(r'[\s\-().]'), ''))) {
      setState(() => _telpError = 'Format tidak valid. Contoh: 08123456789');
      return false;
    }
    setState(() => _telpError = null);
    return true;
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    if (!_validateForm()) return;

    setState(() => _isLoading = true);

    final response = await PengaduanApiService.submitComplaint({
      'idPelanggan': _idController.text,
      'nama': sanitizeForSheet(_namaController.text),
      'telpon': _telpController.text,
      'alamat': sanitizeForSheet(_alamatController.text),
      'pengaduan': sanitizeForSheet(_pengaduanController.text),
      'petugas': widget.userNama,
      'wilayah': (_mrsValue != null && (_mrsValue == 'KD' || _mrsValue == 'WR')) 
          ? _mrsValue! 
          : (widget.userName.toLowerCase().contains('wr') ? 'WR' : (widget.userName.toLowerCase().contains('kd') ? 'KD' : 'XX')),
    });

    setState(() => _isLoading = false);

    if (response['ok'] == true) {
      _showSuccessOverlay(response['ticket'], response['isOverdueUpdate'] ?? false);
    } else {
      _showSnackbar(response['message'] ?? 'Laporan ditolak', isError: true);
    }
  }

  void _showSuccessOverlay(String ticketCode, bool isOverdue) {
    showSuccessSheet(
      context: context,
      ticket: ticketCode,
      ticketLabel: 'NOMOR TIKET ANDA',
      title: isOverdue ? 'Peringatan Terkirim!' : 'Pengaduan Terkirim!',
      message: isOverdue
          ? 'Pengaduan Overdue berhasil dilaporkan kembali kepada tim teknis.'
          : 'Data pengaduan pelanggan telah berhasil dicatat ke sistem.',
      finishLabel: 'Dashboard',
      onFinish: () => Navigator.pop(context),
      continueLabel: 'Buat Baru',
      onContinue: _resetForm,
    );
  }

  void _resetForm() {
    _idController.clear();
    _namaController.clear();
    _telpController.clear();
    _alamatController.clear();
    _pengaduanController.clear();
    _mrsValue = null;
    setState(() {
      _idStatus = "";
      _telpError = null;
    });
  }

  void _showSnackbar(String message, {bool isError = false}) {
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError ? AppColors.danger : AppColors.selesai,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        titleSpacing: 0,
        backgroundColor: AppColors.pengaduan,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded, color: Colors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              'Form Pengaduan',
              style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16),
            ),
            Text(
              'Petugas: ${widget.userNama}',
              style: const TextStyle(color: Colors.white70, fontSize: 11),
            ),
          ],
        ),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 40),
          children: [
            SectionCard(
              icon: Icons.person_search_rounded,
              title: 'Informasi Pelanggan',
              subtitle: 'Masukkan ID Pelanggan untuk melacak data.',
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildLabel('ID Pelanggan', statusText: _idStatus, statusColor: _idStatusColor),
                  TextFormField(
                    controller: _idController,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(hintText: 'Contoh: 6120990001'),
                    validator: (v) =>
                        (v == null || v.trim().isEmpty) ? 'ID pelanggan wajib diisi' : null,
                  ),
                  const SizedBox(height: 14),
                  _buildLabel('Nama Lengkap', isRequired: true),
                  TextFormField(
                    controller: _namaController,
                    decoration: const InputDecoration(hintText: 'Nama lengkap pelanggan'),
                    validator: (v) =>
                        (v == null || v.trim().isEmpty) ? 'Nama lengkap wajib diisi' : null,
                  ),
                  const SizedBox(height: 14),
                  _buildLabel('Nomor Telepon / WA', isRequired: true),
                  TextFormField(
                    controller: _telpController,
                    keyboardType: TextInputType.phone,
                    decoration: InputDecoration(
                      hintText: 'Contoh: 08123456789',
                      errorText: _telpError,
                    ),
                    onChanged: (_) => setState(() {}),
                    validator: (v) =>
                        (v == null || v.trim().isEmpty) ? 'Nomor telepon wajib diisi' : null,
                  ),
                  if (ContactLauncher.hasValidPhone(_telpController.text)) ...[
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        TextButton.icon(
                          onPressed: () async {
                            final ok = await ContactLauncher.callPhone(_telpController.text);
                            if (!ok && context.mounted) {
                              ContactLauncher.showLaunchFailure(context, 'aplikasi telepon');
                            }
                          },
                          icon: const Icon(Icons.call_rounded, size: 16),
                          label: const Text('Telepon'),
                        ),
                        TextButton.icon(
                          onPressed: () async {
                            final ok = await ContactLauncher.openWhatsApp(_telpController.text);
                            if (!ok && context.mounted) {
                              ContactLauncher.showLaunchFailure(context, 'WhatsApp');
                            }
                          },
                          icon: const Icon(Icons.chat_rounded, size: 16, color: Color(0xFF25D366)),
                          label: const Text('WhatsApp', style: TextStyle(color: Color(0xFF25D366))),
                        ),
                      ],
                    ),
                  ],
                ],
              ),
            ),
            SectionCard(
              icon: Icons.chat_bubble_outline_rounded,
              title: 'Detail Pengaduan',
              subtitle: 'Jelaskan keluhan pelanggan dengan jelas.',
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildLabel('Alamat Lokasi', isRequired: true),
                  TextFormField(
                    controller: _alamatController,
                    maxLines: 2,
                    decoration: const InputDecoration(hintText: 'Alamat lengkap lokasi pengaduan'),
                    validator: (v) =>
                        (v == null || v.trim().isEmpty) ? 'Alamat wajib diisi' : null,
                  ),
                  const SizedBox(height: 14),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      _buildLabel('Isi Pengaduan', isRequired: true),
                      Text(
                        '$_charCount / $_maxChars',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: _charCount >= _maxChars ? AppColors.danger : AppColors.muted,
                        ),
                      )
                    ],
                  ),
                  TextFormField(
                    controller: _pengaduanController,
                    maxLines: 4,
                    maxLength: _maxChars,
                    decoration: const InputDecoration(
                      hintText: 'Tulis kendala / masalah yang dilaporkan...',
                      counterText: "",
                    ),
                    validator: (v) =>
                        (v == null || v.trim().isEmpty) ? 'Isi pengaduan wajib diisi' : null,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),
            SizedBox(
              height: 52,
              child: ElevatedButton(
                onPressed: _isLoading ? null : _submit,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.pengaduan,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                child: _isLoading
                    ? const SizedBox(
                        height: 22,
                        width: 22,
                        child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5),
                      )
                    : const Text(
                        'Kirim Pengaduan',
                        style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                      ),
              ),
            )
          ],
        ),
      ),
    );
  }

  Widget _buildLabel(String text, {bool isRequired = false, String statusText = "", Color? statusColor}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        children: [
          Text(
            text,
            style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppColors.ink),
          ),
          if (isRequired)
            const Text(' *', style: TextStyle(color: AppColors.danger, fontWeight: FontWeight.bold)),
          if (statusText.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(left: 8),
              child: Text(
                statusText,
                style: TextStyle(fontSize: 11, color: statusColor, fontWeight: FontWeight.bold),
              ),
            )
        ],
      ),
    );
  }
}
