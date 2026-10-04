/**
 * ═══════════════════════════════════════════════════════════════════════
 * GABUNGAN SISTEM PENGADUAN v9.6, SISTEM MANAJEMEN CABUT/SET, API CRUD, 
 * & OCR SINKRONISASI v2 (PATCHED)
 * ═══════════════════════════════════════════════════════════════════════
 */

// =======================================================================
// KONFIGURASI SUMBER 1 (CABUT / SET / PENGADUAN VIA APP)
// =======================================================================
const SPREADSHEET_ID = '1-G0vVvkCnlWedHtWR5TIs_mxz_1wdVJjwMWMAshzy6U';
const FOLDER_ID_SRC1 = '1OKYMEiGhx2s-Qq3NL-I0gemoAMh2h4hM';

// =======================================================================
// KONFIGURASI SUMBER 2 (SISTEM PENGADUAN UTAMA)
// =======================================================================
const CONFIG = {
  WEBAPP_URL_FALLBACK: 'https://script.google.com/macros/s/AKfycbxU4wPDlkG7Ab4MYFJiYgiJ-MpBoXdfYnTLmmnH2ta0TKtROrV2sTpc5RvUVS3FYJ2x5w/exec',
  SHEET_PENGADUAN: 'REPORT',
  SHEET_DAPELL:    'dbase',
  SHEET_JENIS:     'JEPENG',
  SHEET_PETUGAS:   'Nama petugas',
  SHEET_PENANGANAN:'AKSI',
  SHEET_LOG:       'Log',
  TZ: Session.getScriptTimeZone(),
  HEADERS: [
    'Timestamp_RP', 'Ticket_RP', 'IDPEL_RP', 'Nama_RP', 'Alamat_RP', 'PENGADUAN_RP',
    'TELFON_RP', 'STATUS_RP', 'Petugas_RP', 'Tindakan_RP', 'Foto_Seb_RP',
    'Foto_Ses_RP', 'Waktu_Seb_RP', 'Waktu_Ses_RP', 'Latitude_RP',
    'Longitude_RP', 'NoMGRT_RP', 'Angka_MGRT_RP', 'No_MGRT_Baru_RP', 'Angka_MGRT_Baru_RP'
  ]
};

const COL = {
  TIMESTAMP: 1, TICKET: 2, ID_PELANGGAN: 3, NAMA: 4, ALAMAT: 5,
  PENGADUAN: 6, TELEPON: 7, STATUS: 8, PETUGAS: 9, KETERANGAN: 10,
  FOTO_SEBELUM: 11, FOTO_SESUDAH: 12, WAKTU_SEBELUM: 13, WAKTU_SESUDAH: 14,
  LATITUDE: 15, LONGITUDE: 16, NO_MGRT: 17, ANGKA_MGRT: 18, NO_MGRT_BARU: 19, ANGKA_MGRT_BARU: 20
};

const STATUS = { OPEN: 'OPEN', PROSES: 'PROSES', SELESAI: 'SELESAI' };


// ═══════════════════════════════════════════════════════════════════════
//  FUNGSI UTAMA DOPOST (PENGGABUNGAN ROUTING SEMUA SUMBER & OCR)
// ═══════════════════════════════════════════════════════════════════════
function doPost(e) {
  try {
    const path = e?.pathInfo ? String(e.pathInfo).replace(/^\/+|\/+$/g, '').toLowerCase() : '';
    const api = String(e?.parameter?.api || '').trim().toLowerCase();
    
    // Route Khusus via URL Path/Parameter untuk Flutter OCR
    if (api === 'ocr' || path === 'ocr') {
      if (typeof handleFlutterOcrPost === 'function') return handleFlutterOcrPost(e);
    }

    if (!e || !e.postData || !e.postData.contents) {
      return ContentService.createTextOutput('OK');
    }

    let payload = {};
    try {
      payload = JSON.parse(e.postData.contents); 
    } catch (err) {
      // Fallback for simple payload or string
      payload = e.parameter || {};
      if (Object.keys(payload).length === 0) {
        return ContentService.createTextOutput('Invalid JSON');
      }
    }

    const action = String(payload.action || '').trim();
    const actionLower = action.toLowerCase();

    // ======================================================
    // ROUTING CRUD (API Insert, Update, Read, Upload)
    // ======================================================
    if (action === "submit_pengaduan") { return _json(doSubmitPengaduan(payload)); } else if (action === "insertRow") {
      return _json(doInsertRow(payload));
    } else if (action === "updateRow") {
      return _json(doUpdateRow(payload));
    } else if (action === "readSheet") {
      return _json(doReadSheet(payload));
    } else if (action === "uploadFile") {
      return _json(doUploadFile(payload));
    }

    // ======================================================
    // ROUTING SUMBER 1 (Aplikasi Petugas Cabut/Set)
    // Dihapus karena sudah migrasi ke Flutter
    // ======================================================
    // ======================================================
    // ROUTING SUMBER 2 (Flutter OCR, Dashboard, WAHA)
    // ======================================================
    if (actionLower === 'ping' || actionLower === 'sync_ocr_dapel') {
      if (typeof handleFlutterOcrPost === 'function') return handleFlutterOcrPost(e);
    }
    if (actionLower === 'get_dropdown_options') {
      return jsonOutput_(getDropdownOptions());
    }
    if (actionLower === 'get_active_tickets') {
      return jsonOutput_(getActiveTickets());
    }
    if (actionLower === 'update_penyelesaian') {
      return jsonOutput_(updatePenyelesaian(payload.data || payload));
    }
    if (actionLower === 'get_customer_by_id') {
      const idpel = payload.idpel || payload.idPelanggan;
      if (!idpel) return _json({ status: 'error', message: 'ID Pelanggan tidak diberikan' });
      const customer = DapellService.getById(idpel);
      if (customer) {
        return _json({ status: 'success', nama: customer.nama, alamat: customer.alamat });
      } else {
        return _json({ status: 'error', message: 'ID Pelanggan tidak ditemukan' });
      }
    }
    
    // ======================================================
    // ROUTING [API BARU] (Generik)
    // ======================================================
    if (actionLower === 'find_row') {
      return _json(findRowHandler(payload));
    }
    if (actionLower === 'update_row_cells') {
      return _json(updateRowCellsHandler(payload));
    }
    if (actionLower === 'upload_photo') {
      return _json(uploadPhotoGeneric(payload));
    }
    if (actionLower === 'notify_selesai') {
      return _json(notifySelesaiGeneric(payload));
    }

    // Abaikan Webhook Telegram (Fokus sebagai notifikasi outbound saja)
    if (payload.update_id || payload.message || payload.edited_message) {
      return ContentService.createTextOutput('OK');
    }

    // Validasi Keamanan & Proses Webhook WAHA
    const isWaha = !!(payload.event && payload.event.includes('message') && payload.payload);
    
    if (!isWaha) {
      const secret = getPropSafe_('WEBHOOK_SECRET'); 
      if (secret && e.parameter?.token !== secret) {
        return ContentService.createTextOutput('Unauthorized');
      }
    }

    if (isWaha) {
      if (typeof processWahaUpdate_ === 'function') {
        processWahaUpdate_(payload);
      } else if (typeof LoggerService !== 'undefined') {
        LoggerService.warn('processWahaUpdate_ is not defined, skipping webhook processing.');
      }
      return ContentService.createTextOutput('OK');
    }

    // Fallback jika action tidak ditemukan
    if (action && !isWaha) {
      return _json({ ok: false, message: "Unknown action: " + action });
    }

    return ContentService.createTextOutput('OK');

  } catch (err) {
    if (typeof LoggerService !== 'undefined') {
      LoggerService.error('doPost Error', { error: err.message, stack: err.stack });
    }
    console.error(err);

    const isOcrRequest = (e?.parameter?.api === 'ocr') || 
                         (e?.pathInfo && String(e.pathInfo).includes('ocr')) ||
                         (payload && (payload.action === 'ping' || payload.action === 'sync_ocr_dapel'));
                         
    if (isOcrRequest) {
      return ContentService
        .createTextOutput(JSON.stringify({ status: 'error', error: err.message || String(err) }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    return _json({ status: 'error', ok: false, message: err.toString() });
  }
}


// ═══════════════════════════════════════════════════════════════════════
//  FUNGSI CRUD (READ, INSERT, UPDATE, UPLOAD)
// ═══════════════════════════════════════════════════════════════════════

function doReadSheet(payload) {
  var sheetId = payload.sheetId; 
  var sheetName = payload.sheetName;
  
  var ss = sheetId ? SpreadsheetApp.openById(sheetId) : SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) throw new Error("Sheet " + sheetName + " not found");
  
  var dataRange = sheet.getDataRange();
  var values = dataRange.getDisplayValues();
  if (values.length === 0) return { ok: true, data: [] };
  
  var headers = values[0];
  var result = [];
  
  for (var i = 1; i < values.length; i++) {
    var row = values[i];
    var obj = {};
    for (var j = 0; j < headers.length; j++) {
      obj[headers[j]] = row[j];
    }
    result.push(obj);
  }
  
  return { ok: true, data: result };
}

function doInsertRow(payload) {
  var sheetId = payload.sheetId; 
  var sheetName = payload.sheetName;
  var data = payload.data; 
  
  var ss = sheetId ? SpreadsheetApp.openById(sheetId) : SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) throw new Error("Sheet " + sheetName + " not found");
  
  var headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
  var rowData = [];
  
  for (var i = 0; i < headers.length; i++) {
    var key = headers[i];
    rowData.push(data[key] !== undefined ? data[key] : "");
  }
  
  sheet.appendRow(rowData);
  return { ok: true, message: "Row inserted successfully" };
}

function doUpdateRow(payload) {
  var sheetId = payload.sheetId;
  var sheetName = payload.sheetName;
  var searchCol = payload.searchColumn; 
  var searchVal = String(payload.searchValue);  
  var data = payload.data; 
  
  var ss = sheetId ? SpreadsheetApp.openById(sheetId) : SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) throw new Error("Sheet " + sheetName + " not found");
  
  var dataRange = sheet.getDataRange();
  var values = dataRange.getDisplayValues();
  var headers = values[0];
  
  var colIndex = headers.indexOf(searchCol);
  if (colIndex === -1) throw new Error("Search column " + searchCol + " not found");
  
  var rowIndex = -1;
  // search from bottom to top for latest entry
  for (var i = values.length - 1; i > 0; i--) {
    if (String(values[i][colIndex]) === searchVal) {
      rowIndex = i + 1; // 1-based for Apps Script
      break;
    }
  }
  
  if (rowIndex === -1) throw new Error("Row not found for " + searchCol + " = " + searchVal);
  
  for (var key in data) {
    var cIndex = headers.indexOf(key);
    if (cIndex !== -1) {
      sheet.getRange(rowIndex, cIndex + 1).setValue(data[key]);
    }
  }
  
  return { ok: true, message: "Row updated successfully" };
}

function doUploadFile(payload) {
  var folderId = payload.folderId;
  var fileName = payload.fileName;
  var base64 = payload.base64Data;
  var mimeType = payload.mimeType || "image/jpeg";
  
  var folder = DriveApp.getFolderById(folderId);
  var decoded = Utilities.base64Decode(base64);
  var blob = Utilities.newBlob(decoded, mimeType, fileName);
  var file = folder.createFile(blob);
  
  // Set file permissions to be accessible by anyone with the link
  file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  
  return { ok: true, fileUrl: file.getUrl(), fileId: file.getId() };
}


// ═══════════════════════════════════════════════════════════════════════
//  FUNGSI & HELPER SUMBER 1
// ═══════════════════════════════════════════════════════════════════════

function _json(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
      .setMimeType(ContentService.MimeType.JSON);
}

function _norm(v) {
  return String(v == null ? '' : v).trim().toLowerCase();
}

// Fungsi login, getList, submitAction telah dihapus (migrasi ke Flutter)
function uploadImageToDrive(base64Str, filename) {
  if (!base64Str) return '';
  const folder = DriveApp.getFolderById(FOLDER_ID_SRC1);
  const base64Data = base64Str.indexOf(',') > -1 ? base64Str.split(',')[1] : base64Str;
  const blob = Utilities.newBlob(Utilities.base64Decode(base64Data), MimeType.JPEG, filename);
  const file = folder.createFile(blob);
  file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  return file.getUrl();
}


// ═══════════════════════════════════════════════════════════════════════
//  FUNGSI & HELPER SUMBER 2
// ═══════════════════════════════════════════════════════════════════════

function normalizeText_(text) { return String(text || '').toLowerCase().replace(/\s+/g, ' ').trim(); }
function buildMapsLink_(koordinat) { if (!koordinat || koordinat === '0,0' || koordinat === ',') return ''; const clean = String(koordinat).replace(/\s/g, ''); return `https://www.google.com/maps/search/?api=1&query=${clean}`; }
function getProp_(key) { const val = PropertiesService.getScriptProperties().getProperty(key); if (!val) throw new Error(`Property "${key}" belum diset.`); return val; }
function getPropSafe_(key) { try { return PropertiesService.getScriptProperties().getProperty(key) || ''; } catch (e) { return ''; } }
function getWebAppUrl_() { return getPropSafe_('WEBAPP_URL') || CONFIG.WEBAPP_URL_FALLBACK; }
function nowIso_() { return Utilities.formatDate(new Date(), CONFIG.TZ, "yyyy-MM-dd'T'HH:mm:ss"); }
function clearCache_() { CacheService.getScriptCache().remove('dashboard_data'); }
function escHtml_(s) { return String(s ?? '').replace(/&/g, '&').replace(/</g, '<').replace(/>/g, '>').replace(/"/g, '"').replace(/'/g, "'"); }
function clean_(v) { return String(v ?? '').trim().toUpperCase(); }

function parseTimestamp_(ts) {
  if (!ts) return null; if (ts instanceof Date) return ts;
  const s = String(ts).trim();
  if (/^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}/.test(s)) { const d = new Date(s.replace(' ', 'T')); if (!isNaN(d.getTime())) return d; }
  const m = s.match(/^(\d{2})\/(\d{2})\/(\d{4})\s+(\d{1,2}):(\d{2}):(\d{2})/);
  if (m) return new Date(+m[3], +m[2]-1, +m[1], +m[4], +m[5], +m[6]);
  const d = new Date(s); return isNaN(d.getTime()) ? null : d;
}

const LoggerService = {
  _getLogSheet: function() {
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    let sh = ss.getSheetByName(CONFIG.SHEET_LOG);
    if (!sh) { sh = ss.insertSheet(CONFIG.SHEET_LOG); sh.appendRow(['Timestamp', 'Level', 'Message', 'Data']); }
    return sh;
  },
  log: function(level, message, data = {}) { 
    try { 
      const sh = this._getLogSheet();
      const lr = sh.getLastRow();
      const rowData = [ nowIso_(), level, message, JSON.stringify(data).substring(0, 5000) ];
      sh.getRange(lr + 1, 1, 1, 4).setValues([rowData]);
    } catch (e) {} 
  },
  info:  (msg, data) => LoggerService.log('INFO',  msg, data),
  warn:  (msg, data) => LoggerService.log('WARN',  msg, data),
  error: (msg, data) => LoggerService.log('ERROR', msg, data)
};

const Validation = {
  isValidPhone: function(phone) { const cleanPhone = String(phone || '').replace(/[^\d+]/g, ''); return /^(08|\+62|62)\d{7,12}$/.test(cleanPhone); },
  validateLaporPayload: function(d) {
    const errors = [];
    if (!d.idPelanggan?.trim()) errors.push('ID Pelanggan wajib diisi');
    if (!d.nama?.trim()) errors.push('Nama wajib diisi');
    if (!d.alamat?.trim()) errors.push('Alamat wajib diisi');
    if (!d.pengaduan?.trim()) errors.push('Kendala wajib diisi');
    if (!d.telpon?.trim()) errors.push('No Telepon wajib diisi');
    if (d.telpon && !this.isValidPhone(d.telpon)) errors.push('Format telepon tidak valid');
    return errors.length > 0 ? { ok: false, message: errors.join(', ') } : { ok: true };
  },
  validateUpdatePayload: function(d) {
    if (!d?.ticket || !d?.petugas || !d?.status || !d?.tindakan || !d?.jenisPengaduan) return { ok: false, message: 'Data tidak lengkap' };
    if (d.status === STATUS.SELESAI && (!d.before || !d.after)) return { ok: false, message: 'Foto wajib untuk SELESAI' };
    return { ok: true };
  },
  sanitize: function(v) { const s = String(v ?? '').trim().toUpperCase(); return ['=', '+', '-', '@'].includes(s[0]) ? "'" + s : s; }
};

function jsonOutput_(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

function processWahaUpdate_(update) {
  try {
    const payload = update.payload;
    if (!payload || payload.fromMe === true || update.event !== 'message') return;

    const rawText = payload.body || payload.text || payload.message?.conversation || payload.message?.extendedTextMessage?.text || payload.message?.text || '';
    const text = normalizeText_(rawText);
    const from = String(payload.from || '').trim();
    
    if (!text || from === getPropSafe_('WA_ID') || from === 'status@broadcast') return;

    const regex = /^#?(lapor|cek|al(?:amat)?|pt|petugas|ll|lokasi|koordinat|tag|open|menu|halo|hi|help|start)(?:#|\s+)?(.*)/i;
    const match = text.match(regex);

    if (!match) return; 

    const command = match[1].toLowerCase(), cleanPayload = match[2].trim();

    if (['menu', 'halo', 'hi', 'help', 'start'].includes(command)) return sendMenu_(from);

    switch (command) {
      case 'lapor': return handleLapor_(from, cleanPayload);
      case 'cek':   return handleCek_(from, cleanPayload);
      case 'tag':   return handleTag_(from, cleanPayload);
      case 'open':  return handleOpen_(from, cleanPayload);
      case 'al': case 'alamat': return handleInfo_(from, cleanPayload, 'al');
      case 'pt': case 'petugas':return handleInfo_(from, cleanPayload, 'pt');
      case 'll': case 'lokasi': case 'koordinat': return handleLokasiWA_(from, cleanPayload);
    }
  } catch (err) { LoggerService.error('WAHA Router ERROR', { error: err.message }); }
}

const LaloService = {
  get: function(id) {
    const target = String(id).trim();
    if (!target) return null;
    
    const cache = CacheService.getScriptCache();
    let laloMapStr = cache.get('LaloMap');
    let laloMap = null;
    
    if (laloMapStr) {
      laloMap = JSON.parse(laloMapStr);
    } else {
      const sh = SpreadsheetApp.getActiveSpreadsheet().getSheetByName('LOLA');
      if (!sh || sh.getLastRow() < 2) return null;
      const data = sh.getDataRange().getValues();
      laloMap = {};
      for (let i = 1; i < data.length; i++) {
        let k = String(data[i][0]).trim();
        if (k) laloMap[k] = String(data[i][1]).trim();
      }
      try {
        const str = JSON.stringify(laloMap);
        if (str.length < 100000) {
          cache.put('LaloMap', str, 21600);
        }
      } catch (e) {}
    }
    
    if (laloMap) {
      if (laloMap[target]) return laloMap[target];
    }
    
    // Fallback
    const key = 'lalo_' + target;
    const cached = cache.get(key);
    if (cached) return cached;
    const sh = SpreadsheetApp.getActiveSpreadsheet().getSheetByName('LOLA');
    if (!sh) return null;
    const data = sh.getDataRange().getValues();
    for (let i = 1; i < data.length; i++) {
      if (String(data[i][0]).trim() === target) {
        const val = String(data[i][1]).trim();
        cache.put(key, val, 21600);
        return val;
      }
    }
    return null;
  },

  fetchAndSave: function(id) {
    return this.get(id);
  }
};

const DapellService = {
  getById: function(id) {
    const target = String(id).trim();
    if (!target) return null;
    
    const cache = CacheService.getScriptCache();
    let dapellMapStr = cache.get('DapellMap');
    let dapellMap = null;
    
    if (dapellMapStr) {
      dapellMap = JSON.parse(dapellMapStr);
    } else {
      try {
        const sh = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(CONFIG.SHEET_DAPELL);
        if (!sh || sh.getLastRow() < 2) return null;
        const data = sh.getRange(2, 1, sh.getLastRow() - 1, 10).getValues();
        dapellMap = {};
        for (let i = 0; i < data.length; i++) {
          let k = String(data[i][0]).trim();
          if (k) {
            dapellMap[k] = { nama: clean_(data[i][1]), alamat: clean_(data[i][2]), petugas: clean_(data[i][8]), kodeWilayah: clean_(data[i][9]) };
          }
        }
        const str = JSON.stringify(dapellMap);
        if (str.length < 100000) {
          cache.put('DapellMap', str, 21600);
        }
      } catch (e) {}
    }
    
    if (dapellMap) {
      if (dapellMap[target]) return dapellMap[target];
    }
    
    try {
      const sh = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(CONFIG.SHEET_DAPELL);
      if (!sh || sh.getLastRow() < 2) return null;
      const ids = sh.getRange(2, 1, sh.getLastRow() - 1, 1).getValues();
      for (let i = 0; i < ids.length; i++) {
        if (String(ids[i][0]).trim() === target) {
          const r = sh.getRange(i + 2, 1, 1, 10).getValues()[0];
          return { nama: clean_(r[1]), alamat: clean_(r[2]), petugas: clean_(r[8]), kodeWilayah: clean_(r[9]) };
        }
      }
    } catch (e) { LoggerService.error('DapellService Error', { error: e.message }); }
    return null;
  }
};

const TicketService = {
  generateId: function(sh, kodeWilayah) {
    const lock = LockService.getScriptLock();
    const today = Utilities.formatDate(new Date(), CONFIG.TZ, 'yyMMdd');
    const prefix = `PGD-${kodeWilayah}-${today}-`;
    try {
      lock.waitLock(10000);
      const props = PropertiesService.getScriptProperties();
      const urut = parseInt(props.getProperty('ticket_counter_' + prefix) || '0', 10) + 1;
      props.setProperty('ticket_counter_' + prefix, String(urut));
      return prefix + String(urut).padStart(3, '0');
    } finally { lock.releaseLock(); }
  },
  findRow: function(sh, ticket) {
    const target = String(ticket).trim().toUpperCase();
    const lastRow = sh.getLastRow();
    if (lastRow < 2) return -1;
    const values = sh.getRange(2, COL.TICKET, lastRow - 1, 1).getValues();
    for (let i = 0; i < values.length; i++) if (String(values[i][0]).trim().toUpperCase() === target) return i + 2;
    return -1;
  }
};

function getSheet() {
  let sh = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(CONFIG.SHEET_PENGADUAN);
  if (!sh) {
    sh = SpreadsheetApp.getActiveSpreadsheet().insertSheet(CONFIG.SHEET_PENGADUAN);
    sh.appendRow(CONFIG.HEADERS);
    sh.getRange(1, 1, 1, CONFIG.HEADERS.length).setFontWeight('bold').setBackground('#1d4ed8').setFontColor('#ffffff');
  }
  return sh;
}

const PengaduanService = {
  submit: function(d) {
    d.nama = String(d.nama).toUpperCase();
    d.alamat = String(d.alamat).toUpperCase();
    d.pengaduan = String(d.pengaduan).toUpperCase();

    const validation = Validation.validateLaporPayload(d);
    if (!validation.ok) throw new Error(validation.message);

    const sh = getSheet(), targetId = Validation.sanitize(d.idPelanggan), now = new Date();
    
    let koor = LaloService.get(targetId);
    if (!koor) koor = LaloService.fetchAndSave(targetId);
    d.koordinat = koor;

    let lat = '', lng = '';
    if (koor && koor.includes(',')) {
      const parts = koor.split(',');
      lat = parts[0]; lng = parts[1];
    }

    const dapel = DapellService.getById(targetId);
    const kodeWilayah = dapel?.kodeWilayah || 'XX';
    const data = sh.getDataRange().getValues();
    
    let activeRow = -1;
    for (let i = data.length - 1; i > 0; i--) {
      const idPel = String(data[i][COL.ID_PELANGGAN - 1]).trim();
      const status = String(data[i][COL.STATUS - 1]).trim().toUpperCase();
      if (idPel === targetId && status !== STATUS.SELESAI) {
        activeRow = i + 1; 
        break;
      }
    }

    if (activeRow !== -1) {
      const activeData = sh.getRange(activeRow, 1, 20).getValues()[0];
      const activeTs = parseTimestamp_(activeData[COL.TIMESTAMP - 1]);
      const hoursElapsed = activeTs ? (now - activeTs) / 3600000 : 0;
      
      if (hoursElapsed < 24) {
        const remainingHours = Math.ceil(24 - hoursElapsed);
        return { ok: false, isLimit1x24: true, message: `PENGADUAN ANDA SEDANG DALAM PROSES (TIKET: ${activeData[COL.TICKET - 1]}).\nHARAPMENUNGGU ~${remainingHours} JAM LAGI.` };
      }
      
      sh.getRange(activeRow, COL.STATUS).setValue(STATUS.OPEN);
      sh.getRange(activeRow, COL.PENGADUAN).setValue(Validation.sanitize(d.pengaduan));
      sh.getRange(activeRow, COL.KETERANGAN).setValue(''); 
      SpreadsheetApp.flush(); clearCache_(); 
      notifPengaduanBaru_({ idpel: d.idPelanggan, nama: d.nama, alamat: d.alamat, keluhan: d.pengaduan, tiket: activeData[COL.TICKET - 1], telpon: d.telpon }); 
      return { ok: true, ticket: activeData[COL.TICKET - 1], isOverdueUpdate: true };
    }

    const ticket = TicketService.generateId(sh, kodeWilayah);
    const newRow = [ nowIso_(), ticket, targetId, Validation.sanitize(d.nama), Validation.sanitize(d.alamat), Validation.sanitize(d.pengaduan), Validation.sanitize(d.telpon), STATUS.OPEN, '', '', '', '', '', '', lat, lng, '', '', '', '' ];
    
    const lr = sh.getLastRow();
    sh.getRange(lr + 1, 1, 1, newRow.length).setValues([newRow]);
    
    SpreadsheetApp.flush(); clearCache_();
    notifPengaduanBaru_({ idpel: d.idPelanggan, nama: d.nama, alamat: d.alamat, keluhan: d.pengaduan, tiket: ticket, telpon: d.telpon }); 
    return { ok: true, ticket, isOverdueUpdate: false };
  }
};

function reply_(chatId, htmlText) { return sendWhatsAppTo_(chatId, htmlText); }

function handleLapor_(chatId, payload) {
  // --- 1. FILTER NOMOR YANG DIIZINKAN ---
  let cleanChatId = String(chatId).replace(/[^0-9]/g, '');
  if (cleanChatId.startsWith('0')) {
    cleanChatId = '62' + cleanChatId.substring(1);
  }
  const allowedNumbers = [
    '126005884797153', //6285731669222
    '150341068787799', //6281333310874
    '239869779538055', //6285731333794
    '220542326034620', //6281703708467
    '228672950763573'  //6285175081338
  ];
  if (!allowedNumbers.includes(cleanChatId)) {
    return reply_(chatId, `❌ <b>MAAF, AKSES DITOLAK</b> untuk nomor ${cleanChatId}\n\nUntuk membuat laporan pengaduan, silakan gunakan <b>Aplikasi</b> atau hubungi Customer Service kami di nomor WhatsApp: <b>085161222706</b>`);
  }
  // --------------------------------------

  // --- 2. PROSES PAYLOAD JIKA NOMOR DIIZINKAN ---
  const parts = payload.split('#').map(p => p.trim());
  if (parts.length < 5) return reply_(chatId, '❌ <b>FORMAT SALAH</b>\n<code>#LAPOR#ID#NAMA#ALAMAT#KENDALA#NO TELEPON</code>');
  
  const cleanId = String(parts[0]).replace(/[^0-9a-zA-Z]/g, '').toUpperCase();
  const cleanTelp = String(parts[4] || '').replace(/[^\d+]/g, '');
  const d = { idPelanggan: cleanId, nama: parts[1], alamat: parts[2], pengaduan: parts[3], telpon: cleanTelp };
  
  try {
    const res = PengaduanService.submit(d);
    if (res.ok) reply_(chatId, res.isOverdueUpdate ? `⚠️ LAPORAN OVERDUE. TIKET: <b>${res.ticket}</b>` : `✅ LAPORAN DICATAT. TIKET: <b>${res.ticket}</b>\n\nKETIK #CEK#${res.ticket}`);
    else reply_(chatId, `⏳ DITOLAK: ${escHtml_(res.message).toUpperCase()}`);
  } catch (e) { 
    reply_(chatId, `❌ GAGAL MEMPROSES: ` + escHtml_(e.message).toUpperCase()); 
  }
}

function handleCek_(chatId, ticket) {
  const cleanTicket = String(ticket).replace(/[^0-9a-zA-Z\-]/g, '').trim().toUpperCase();
  if (!cleanTicket) return reply_(chatId, '📌 <b>FORMAT:</b> <code>#CEK#NOMOR-TIKET</code>');
  const sh = getSheet(), row = TicketService.findRow(sh, cleanTicket);
  if (row === -1) return reply_(chatId, `❌ TIKET TIDAK DITEMUKAN.`);
  const d = sh.getRange(row, 1, 1, 20).getValues()[0];
  const status = String(d[COL.STATUS - 1]).trim().toUpperCase();
  const emoji = { OPEN: '🔴', PROSES: '🟡', SELESAI: '🟢' }[status] || '⚪';
  reply_(chatId, `📋 <b>DETAIL TIKET</b>\n\nTIKET: ${String(d[COL.TICKET - 1]).trim().toUpperCase()}\nNAMA: ${escHtml_(String(d[COL.NAMA - 1]).trim().toUpperCase())}\nMASALAH: ${escHtml_(String(d[COL.PENGADUAN - 1]).trim().toUpperCase())}\n\nSTATUS: ${emoji} <b>${status}</b>`);
}

function handleInfo_(chatId, id, type) {
  const cleanId = String(id).replace(/[^0-9a-zA-Z]/g, '').toUpperCase();
  if (!cleanId) return reply_(chatId, `❌ FORMAT SALAH.`);
  const data = DapellService.getById(cleanId);
  if (!data) return reply_(chatId, '❌ ID PELANGGAN TIDAK DITEMUKAN DI DATABASE.');
  reply_(chatId, type === 'al' ? `📍 <b>ALAMAT:</b>\n${escHtml_(data.alamat).toUpperCase()}` : `👷 <b>PETUGAS:</b>\n${data.petugas ? escHtml_(data.petugas).toUpperCase() : '<i>BELUM ADA PETUGAS TERDATA</i>'}`);
}

function handleLokasiWA_(chatId, text) {
  const id = String(text).replace(/[^0-9a-zA-Z]/g, '').trim().toUpperCase();
  if (!id) return reply_(chatId, '❌ <b>FORMAT:</b> <code>#LL#ID</code>');
  const existing = LaloService.get(id);
  if (existing) return kirimLokasiWA_(chatId, existing);
  reply_(chatId, '⏳ <i>MENCARI LOKASI (MAKS 5 DETIK)...</i>');
  const result = LaloService.fetchAndSave(id);
  if (!result) reply_(chatId, '❌ LOKASI TIDAK DITEMUKAN ATAU SERVER SEDANG LAMBAT.');
  else kirimLokasiWA_(chatId, result);
}

function kirimLokasiWA_(chatId, koordinat) {
  const clean = String(koordinat).replace(/\s/g, '');
  const maps = buildMapsLink_(clean);
  reply_(chatId, `📍 <b>LOKASI:</b>\n${clean}\n\n${maps}`);
}

function sendMenu_(chatId) {
  return reply_(chatId, '👋 <b>PANDUAN PERINTAH:</b>\n\n📝 LAPOR: <code>#LAPOR#ID#NAMA#ALAMAT#KENDALA#TELEPON</code>\n🔍 CEK: <code>#CEK#TIKET</code>\n📍 ALAMAT: <code>#AL#ID</code>\n👷 PETUGAS: <code>#PT#ID</code>\n🗺️ KOORDINAT: <code>#LL#ID</code>\n📋 TAG: <code>#TAG#ID</code>\n🔓 OPEN: <code>#OPEN#KD</code> atau <code>#OPEN#WARU</code>');
}

function handleTag_(chatId, idPayload) {
  const cleanId = String(idPayload).replace(/[^0-9a-zA-Z]/g, '').toUpperCase().trim();
  
  if (!cleanId || !cleanId.startsWith('612')) {
    return reply_(chatId, '❌ *FORMAT SALAH ATAU ID TIDAK VALID*\nPastikan ID diawali dengan 612.\n\n_Contoh: #TAG#6120000000_');
  }
  
  reply_(chatId, '⏳ _MENCARI DATA TUNGGAKAN (MAKS 10 DETIK)..._');
  
  try {
    const result = getTunggakanByIdPelanggan(cleanId);
    
    if (!result.success) return reply_(chatId, `❌ ${result.message}`);
    
    if (!result.data || result.data.length === 0) {
      return reply_(chatId, `✅ *PELANGGAN TIDAK MEMILIKI TUNGGAKAN*\n(Seluruh tagihan LUNAS)\n\nID: \`${cleanId}\``);
    }
    
    // Tarik data NAMA, ALAMAT, dan NO METER dari array pertama
    const firstItem = result.data[0];
    const noMeter = String(firstItem.nometer || firstItem.norek || '-').trim();
    const nama = String(firstItem.nama || '-').toUpperCase().trim();
    const alamat = String(firstItem.alamat || '-').toUpperCase().trim();
    
    let message = `📊 *DATA TUNGGAKAN*\n\n🆔 ID PELANGGAN: \`${cleanId}\`\n📞 NO METER: \`${noMeter}\`\n👤 NAMA: ${nama}\n📍 ALAMAT: ${alamat}\n\n*RINCIAN TUNGGAKAN:*\n`;
    
    let totalSemua = 0; 
    const jumlahLembar = result.data.length; 
    
    // Perhitungan otomatis periode tagihan berjalan
    const now = new Date();
    const currMonth = now.getMonth(); // 0 = Jan, 7 = Agust
    const currYear = now.getFullYear();
    
    // Tagihan berjalan adalah bulan sebelumnya
    const billMonth = currMonth === 0 ? 12 : currMonth; 
    const billYear = currMonth === 0 ? currYear - 1 : currYear;
    
    // Buat skor (YYYYMM) untuk perbandingan mudah (Contoh: Agt 2026 -> 202607)
    const currentBillScore = (billYear * 100) + billMonth;
    
    result.data.forEach((item, idx) => {
      const rp = Math.round(item.rptagihan || 0); 
      const bulan = String(item.bulanrek || '-').trim(); // e.g., "072026"
      
      let denda = 0;
      if (bulan.length === 6) {
        const m = parseInt(bulan.substring(0, 2), 10);
        const y = parseInt(bulan.substring(2, 6), 10);
        const itemScore = (y * 100) + m;
        
        // Jika periode tagihan ini di bawah periode tagihan berjalan, otomatis denda 15rb
        if (itemScore < currentBillScore) {
          denda = 15000;
        }
      }
      
      const admin = 3500;
      const subtotal = rp + denda + admin;
      totalSemua += subtotal;
      
      message += `\n${idx + 1}. Periode ${bulan} : Rp ${rp.toLocaleString('id-ID')}\n    Denda : Rp ${denda.toLocaleString('id-ID')}\n    Admin : Rp ${admin.toLocaleString('id-ID')}\n`;
    });
    
    message += `\n*━━━━━━━━━━━━━━━━━━━*\n💰 *TOTAL TUNGGAKAN:*\n   *Rp ${totalSemua.toLocaleString('id-ID')}*\n\n📌 JUMLAH TAGIHAN: ${jumlahLembar} lembar`;
    
    return reply_(chatId, message);
  } catch (e) { 
    LoggerService.error('handleTag_ Error', { error: e.message, idPayload }); 
    return reply_(chatId, `❌ GAGAL MEMPROSES: ${String(e.message).toUpperCase()}`); 
  }
}

function handleOpen_(chatId, areaPayload) {
  const areaReq = String(areaPayload).trim().toUpperCase();
  let kodeArea = '';
  if (areaReq === 'KD') kodeArea = 'KD';
  else if (areaReq === 'WARU' || areaReq === 'WR') kodeArea = 'WR';
  else return reply_(chatId, '❌ <b>FORMAT SALAH</b>\nGunakan: <code>#OPEN#KD</code> atau <code>#OPEN#WARU</code>');

  reply_(chatId, `⏳ <i>MENARIK DATA PENGADUAN OPEN UNTUK AREA ${areaReq}...</i>`);
  try {
    const sh = getSheet(); const data = sh.getDataRange().getValues(); let results = [];
    for (let i = 1; i < data.length; i++) {
      const row = data[i]; const ticket = String(row[COL.TICKET - 1]).trim().toUpperCase(); const status = String(row[COL.STATUS - 1]).trim().toUpperCase();
      if (status === STATUS.OPEN && ticket.includes(`-${kodeArea}-`)) {
        let dt = parseTimestamp_(row[COL.TIMESTAMP - 1]); if (!dt) dt = new Date(); 
        const pad = n => n < 10 ? '0' + n : n; const tanggalStr = `${pad(dt.getDate())}/${pad(dt.getMonth() + 1)}/${dt.getFullYear()} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`;
        results.push({ id: String(row[COL.ID_PELANGGAN - 1]).trim().toUpperCase(), nama: String(row[COL.NAMA - 1]).trim().toUpperCase(), alamat: String(row[COL.ALAMAT - 1]).trim().toUpperCase(), telp: String(row[COL.TELEPON - 1]).trim(), keluhan: String(row[COL.PENGADUAN - 1]).trim().toUpperCase(), tanggal: tanggalStr });
      }
    }
    if (results.length === 0) return reply_(chatId, `✅ <b>TIDAK ADA PENGADUAN OPEN</b>\nDi Area: <b>${areaReq}</b>`);
    let message = `📋 <b>DAFTAR PENGADUAN OPEN (${areaReq})</b>\nTotal: ${results.length} Tiket\n\n<b>━━━━━━━━━━━━━━━━━━━</b>\n`;
    results.forEach((item, idx) => { message += `\n${idx + 1}. 🆔 <code>${item.id}</code>\n📅 ${item.tanggal}\n👤 ${item.nama}\n📞 <code>${item.telp || '-'}</code>\n📍 ${item.alamat}\n📄 ${item.keluhan}\n`; });
    message += `\n<b>━━━━━━━━━━━━━━━━━━━</b>`;
    if (message.length > 4000) message = message.substring(0, 4000) + `\n\n... [DATA DIPOTONG, HUBUNGI ADMIN UNTUK REKAP LENGKAP]`;
    return reply_(chatId, message);
  } catch (e) { LoggerService.error('handleOpen_ Error', { error: e.message, areaPayload }); return reply_(chatId, `❌ GAGAL MEMPROSES: ${escHtml_(e.message).toUpperCase()}`); }
}

// Fungsi Internal Pengganti untuk Login Tagihan
function loginTunggakan_(userId, passw, id) {
  const cache = CacheService.getScriptCache();
  let token = cache.get('token_tagihan_' + userId);
  if (token) return { success: true, token: token };

  try {
    const res = UrlFetchApp.fetch('https://ptgn.mdp.net.id/petrogasx/public/api/post/ceklogin/data', {
      method: 'post', 
      contentType: 'application/json',
      payload: JSON.stringify({ userId: userId, passw: passw, id: id }), 
      muteHttpExceptions: true
    });
    const data = JSON.parse(res.getContentText());
    if (data.token) { 
      cache.put('token_tagihan_' + userId, data.token, 1800); // Cache 30 menit
      return { success: true, token: data.token }; 
    }
    return { success: false, message: 'Gagal mendapatkan token.' };
  } catch (e) { 
    return { success: false, message: e.message }; 
  }
}

// Logika Fetch dan Filter Tagihan
function getTunggakanByIdPelanggan(idPelanggan) {
  try {
    const cleanId = String(idPelanggan).trim().toUpperCase();
    
    // Login khusus menggunakan kredensial Sidoarjo
    const loginRes = loginTunggakan_('adminsdj', 'mgK', 11);
    if (!loginRes || !loginRes.success) { 
      LoggerService.error('getTunggakanByIdPelanggan - Login Failed', { response: loginRes }); 
      return { success: false, data: [], message: 'SISTEM LAGI MAINTENANCE. COBA LAGI NANTI.', count: 0 }; 
    }
    
    // Padding ID menjadi 15 karakter sesuai standar API
    const cari = cleanId.padEnd(15, ' ');
    
    // Menggunakan endpoint /caridispelanggan 
    const res = UrlFetchApp.fetch('https://ptgn.mdp.net.id/petrogasx/public/api/post/data/caridispelanggan', {
      method: 'post', 
      contentType: 'application/json',
      payload: JSON.stringify({ cari: cari, id: 11, token: loginRes.token }), 
      muteHttpExceptions: true
    });
    
    const resData = JSON.parse(res.getContentText());
    
    if (!resData || !resData.success) { 
      LoggerService.error('getTunggakanByIdPelanggan - Fetch Failed', { response: resData }); 
      return { success: false, data: [], message: 'DATA TUNGGAKAN TIDAK DAPAT DIAKSES SAAT INI.', count: 0 }; 
    }
    
    if (!resData.data || !Array.isArray(resData.data) || resData.data.length === 0) {
      return { success: true, data: [], message: 'Tidak ada data pelanggan', count: 0 };
    }
    
    // Filter HANYA tagihan yang belum lunas (berstatus "T" atau "t")
    const tagihanBelumLunas = resData.data.filter(item => String(item.lunas).toLowerCase() === 't');
    
    return { 
      success: true, 
      data: tagihanBelumLunas, 
      count: tagihanBelumLunas.length, 
      message: `Ditemukan ${tagihanBelumLunas.length} lembar tunggakan` 
    };
    
  } catch (e) { 
    LoggerService.error('getTunggakanByIdPelanggan Exception', { error: e.message, stack: e.stack, idPelanggan }); 
    return { success: false, data: [], message: 'TERJADI KESALAHAN SISTEM ATAU TIMEOUT', count: 0 }; 
  }
}

const NotificationService = {
  send: function(text, type) {
    try { const tgId = getProp_('NOTIF_CHATID'); if (tgId) sendTelegramTo_(tgId, text); } catch (e) {}
    try { const waGroup = getPropSafe_('WA_GROUP_ID'); if (waGroup) sendWhatsAppTo_(waGroup, text); } catch (e) {}
  }
};

function notifPengaduanBaru_(data) {
  const { idpel, nama, alamat, keluhan, tiket, telpon, petugas } = data;
  const koordinat = LaloService.get(idpel);
  const maps = buildMapsLink_(koordinat);
  const lokasiHtml = maps ? `<a href="${maps}">🗺️ BUKA PETA LOKASI</a>` : 'LOKASI BELUM TERSEDIA';
  const text = `🚨 <b>PENGADUAN BARU</b>\n\n🎫 TIKET: <code>${String(tiket).toUpperCase()}</code>\n🆔 IDPEL: <code>${String(idpel).toUpperCase()}</code>\n👤 NAMA : ${String(nama).toUpperCase()}\n📱 TELP : <code>${telpon}</code>\n📍 ALAMAT: ${String(alamat).toUpperCase()}\n📄 KELUHAN:\n${String(keluhan).toUpperCase()}\n\n📌 LOKASI:\n${koordinat ? `<code>${koordinat}</code>\n` : ''}${lokasiHtml}\n\n🔧 Petugas yg melaporkan : <b>${String(petugas || 'Admin / WA').toUpperCase()}</b>`;
  NotificationService.send(text, 'URGENT');
}

function notifSelesai_(data) {
  const { tiket, idpel, nama, pengaduan, petugas, tindakan, fotoSeb, fotoSes, koordinatPetugas, waktuSesudah } = data;
  const maps = buildMapsLink_(koordinatPetugas);
  const lokasiHtml = maps ? `<a href="${maps}">🗺️ BUKA PETA LOKASI</a>` : '<i>LOKASI BELUM TERSEDIA</i>';
  
  let waktuFotoSesudah = waktuSesudah || '';
  if (!waktuFotoSesudah && tiket) {
    try {
      const sh = SpreadsheetApp.openById(SPREADSHEET_ID).getSheetByName(CONFIG.SHEET_PENGADUAN);
      if (sh) {
        const row = TicketService.findRow(sh, tiket);
        if (row !== -1) waktuFotoSesudah = String(sh.getRange(row, COL.WAKTU_SESUDAH).getValue()).trim();
      }
    } catch(e) {}
  }
  
  let text = `✅ <b>PENGADUAN SELESAI</b>\n\n`;
  if (waktuFotoSesudah) text += `Pada : ${waktuFotoSesudah}\n`;
  text += `🎫 TIKET : <code>${String(tiket || '').toUpperCase()}</code>\n`;
  text += `🆔 IDPEL: <code>${String(idpel || '').toUpperCase()}</code>\n`;
  text += `👤 NAMA : ${String(nama || '').toUpperCase()}\n`;
  text += `💬 PENGADUAN:\n${String(pengaduan || '-').toUpperCase()}\n\n`;
  text += `🛠️ TINDAKAN:\n${String(tindakan || '').toUpperCase()}\n\n`;
  text += `👷 PETUGAS: <b>${String(petugas || '').toUpperCase()}</b>\n\n`;
  if (fotoSeb) text += `📸 FOTO SEBELUM: <a href="${fotoSeb}">Lihat Foto</a>\n`;
  if (fotoSes) text += `📸 FOTO SESUDAH: <a href="${fotoSes}">Lihat Foto</a>\n\n`;
  text += `📍 LOKASI PETUGAS:\n${koordinatPetugas ? `<code>${koordinatPetugas}</code>\n` : ''}${lokasiHtml}\n\n`;
  text += `📊 STATUS: <b>SELESAI</b>`;
  
  NotificationService.send(text, 'DONE');
}

function doGet(e) {
  const path = e?.pathInfo ? String(e.pathInfo).replace(/^\/+|\/+$/g, '').toLowerCase() : '';
  const api = String(e?.parameter?.api || '').trim().toLowerCase();
  
  if (api === 'ocr' || path === 'ocr') {
    return ContentService.createTextOutput(JSON.stringify({ status: 'ok', service: 'ocr_bridge', message: 'OCR bridge ready' })).setMimeType(ContentService.MimeType.JSON); 
  }
  
  return ContentService.createTextOutput(JSON.stringify({ status: 'ok', message: 'SiPEKAT API v3 Ready' })).setMimeType(ContentService.MimeType.JSON); 
}

function cekIdPelanggan(id) { return DapellService.getById(id); }

function getDropdownOptions() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const getList = (shName) => {
    const sh = ss.getSheetByName(shName);
    if (!sh || sh.getLastRow() < 2) return [];
    // Baca mulai baris 2 (lewati header)
    return sh.getRange(2, 1, sh.getLastRow() - 1, 1)
      .getValues()
      .flat()
      .map(s => String(s).trim())
      .filter(s => s.length > 0);
  };
  let listPetugas = [];
  const shPet = ss.getSheetByName(CONFIG.SHEET_PETUGAS);
  if (shPet?.getLastRow() > 0) {
    shPet.getRange(1, 1, shPet.getLastRow(), 2).getValues().forEach(r => {
      const nm = String(r[0]).trim().toUpperCase();
      if (nm && nm !== 'NAMA PETUGAS') listPetugas.push({ nama: nm, area: String(r[1]).trim().toUpperCase() });
    });
  }
  return { jenis: getList(CONFIG.SHEET_JENIS), penanganan: getList(CONFIG.SHEET_PENANGANAN), petugas: listPetugas };
}

function submitPengaduan(d) { try { return PengaduanService.submit(d); } catch (e) { LoggerService.error('submitPengaduan HTML Error', { error: e.message }); throw e; } }

function updatePenyelesaian(d) {
  d.petugas = String(d.petugas).toUpperCase();
  d.tindakan = String(d.tindakan).toUpperCase();
  const validation = Validation.validateUpdatePayload(d); if (!validation.ok) throw new Error(validation.message);
  const sh = getSheet(), row = TicketService.findRow(sh, d.ticket);
  if (row === -1) throw new Error('Tiket tidak ditemukan.'); if (String(sh.getRange(row, COL.STATUS).getValue()).trim().toUpperCase() === STATUS.SELESAI) throw new Error('Tiket sudah SELESAI.');

  d.idPelanggan = String(sh.getRange(row, COL.ID_PELANGGAN).getValue()).trim().toUpperCase(); 
  d.nama = String(sh.getRange(row, COL.NAMA).getValue()).trim().toUpperCase();
  d.pengaduan = String(sh.getRange(row, COL.PENGADUAN).getValue()).trim().toUpperCase();
  
  let fSeb = '', fSes = '', wSeb = '', wSes = '';
  if (d.status === STATUS.SELESAI) {
    const folder = DriveApp.getFolderById(getProp_('FOLDER_ID'));
    const b2b = (dataUrl, name) => { const p = String(dataUrl).split(','); const mime = p[0].match(/data:(.*?);base64/)?.[1] || 'image/jpeg'; return Utilities.newBlob(Utilities.base64Decode(p[1]), mime, `${name}.jpg`); };
    fSeb = folder.createFile(b2b(d.before, `before_${d.ticket}`)).getUrl(); fSes = folder.createFile(b2b(d.after, `after_${d.ticket}`)).getUrl(); wSeb = d.waktuSebelumStr || ''; wSes = d.waktuSesudahStr || '';
  }

  sh.getRange(row, COL.STATUS, 1, 13).setValues([[ Validation.sanitize(d.status), Validation.sanitize(d.petugas), Validation.sanitize(d.tindakan), fSeb, fSes, wSeb, wSes, d.lat || '', d.lng || '', Validation.sanitize(d.noMgrt || ''), Validation.sanitize(d.angkaMgrt || ''), Validation.sanitize(d.noMgrtBaru || ''), Validation.sanitize(d.angkaMgrtBaru || '') ]]);
  SpreadsheetApp.flush(); clearCache_(); 
  notifSelesai_({ tiket: d.ticket, idpel: d.idPelanggan, nama: d.nama, pengaduan: d.pengaduan, petugas: d.petugas, tindakan: d.tindakan, fotoSeb: fSeb, fotoSes: fSes, koordinatPetugas: d.lat && d.lng ? `${d.lat},${d.lng}` : '' });
  return { ok: true, ticket: d.ticket, status: d.status };
}

function getActiveTickets() {
  const data = getSheet().getDataRange().getValues(), out = [];
  for (let i = 1; i < data.length; i++) { 
    const status = String(data[i][COL.STATUS - 1]).trim().toUpperCase(), noTiket = String(data[i][COL.TICKET - 1]).trim().toUpperCase(); 
    if (status !== STATUS.SELESAI && noTiket) {
      out.push({ 
        ticket: noTiket, 
        nama: String(data[i][COL.NAMA - 1]).trim().toUpperCase(), 
        status: status, 
        area: noTiket.includes('KD') ? 'KD' : (noTiket.includes('WR') ? 'WR' : ''),
        idPelanggan: String(data[i][COL.ID_PELANGGAN - 1]).trim(),
        alamat: String(data[i][COL.ALAMAT - 1]).trim(),
        telepon: String(data[i][COL.TELEPON - 1]).trim(),
        kendala: String(data[i][COL.PENGADUAN - 1]).trim(),
        lat: String(data[i][COL.LATITUDE - 1]).trim(),
        lng: String(data[i][COL.LONGITUDE - 1]).trim()
      }); 
    }
  }
  return out.reverse();
}

function getDashboardData() {
  const sh = getSheet(); const data = sh.getDataRange().getValues(); const out = [];
  for (let i = 1; i < data.length; i++) {
    const row = data[i]; const ticket = String(row[COL.TICKET - 1]).trim().toUpperCase();
    if (!ticket) continue;
    let dt = parseTimestamp_(row[COL.TIMESTAMP - 1]); if (!dt) dt = new Date(); 
    const pad = n => n < 10 ? '0' + n : n; const waktuText = `${pad(dt.getDate())}/${pad(dt.getMonth() + 1)} ${pad(dt.getHours())}:${pad(dt.getMinutes())}`;
    out.push({ waktuISO: dt.toISOString(), waktuText: waktuText, ticket: ticket, nama: String(row[COL.NAMA - 1]).trim().toUpperCase(), pengaduan: String(row[COL.PENGADUAN - 1]).trim().toUpperCase(), petugas: String(row[COL.PETUGAS - 1]).trim().toUpperCase(), status: String(row[COL.STATUS - 1]).trim().toUpperCase() });
  }
  return out.reverse();
}

function sendTelegramTo_(chatId, text) { 
  try { return UrlFetchApp.fetch(`https://api.telegram.org/bot${getProp_('BOT_TOKEN')}/sendMessage`, { method: 'post', contentType: 'application/json', payload: JSON.stringify({ chat_id: chatId, text: String(text).substring(0, 4000), parse_mode: 'HTML', disable_web_page_preview: true }), muteHttpExceptions: true }).getResponseCode() < 300; } catch (e) { return false; }
}

function sendWhatsAppTo_(chatId, htmlText) {
  const waText = String(htmlText || '').replace(/<b>([\s\S]*?)<\/b>/gi, '*$1*').replace(/<i>([\s\S]*?)<\/i>/gi, '_$1_').replace(/<code>([\s\S]*?)<\/code>/gi, '`$1`').replace(/<a[^>]+href="([^"]+)"[^>]*>([\s\S]*?)<\/a>/gi, '$2:\n$1').replace(/<br\s*\/?>/gi, '\n').replace(/<[^>]+>/g, '').replace(/&/g, '&').replace(/</g, '<').replace(/>/g, '>').trim();
  const headers = { 'Content-Type': 'application/json', 'User-Agent': 'GAS-Client/9.6', 'X-Api-Key': getPropSafe_('WAHA_API_KEY') };
  try { return UrlFetchApp.fetch(`${getPropSafe_('WAHA_URL').replace(/\/$/, '')}/api/sendText`, { method: 'post', headers, payload: JSON.stringify({ chatId, text: waText, session: getPropSafe_('WAHA_SESSION') }), muteHttpExceptions: true }).getResponseCode() < 300; } catch(e){}
  return false;
}

function testWAHA() {
  const nomorTujuan = "6285161222706"; 
  const wahaUrl = getPropSafe_('WAHA_URL').replace(/\/$/, '');
  const apiKey = getPropSafe_('WAHA_API_KEY');
  const session = getPropSafe_('WAHA_SESSION') || 'default';
  if (!wahaUrl) { Logger.log("❌ ERROR: WAHA_URL belum diatur di PropertiesService!"); return; }
  const payload = { chatId: nomorTujuan + "@c.us", text: "✅ UJI COBA KONEKSI WAHA V9.6 BERHASIL PADA " + new Date().toLocaleString(), session: session };
  const options = { method: 'post', contentType: 'application/json', headers: apiKey ? { 'X-Api-Key': apiKey } : {}, payload: JSON.stringify(payload), muteHttpExceptions: true };
  try {
    const response = UrlFetchApp.fetch(`${wahaUrl}/api/sendText`, options);
    const code = response.getResponseCode();
    if (code >= 200 && code < 300) Logger.log("✅ SUKSES: Pesan WAHA terkirim. Status: " + code);
    else Logger.log("❌ GAGAL: " + code + " - " + response.getContentText());
  } catch (e) { Logger.log("❌ ERROR KONEKSI WAHA: " + e.message); }
}

function testDNS() {
  const wahaUrl = getPropSafe_('WAHA_URL').replace(/\/$/, '');
  const apiKey = getPropSafe_('WAHA_API_KEY');
  if (!wahaUrl) { Logger.log("❌ ERROR: WAHA_URL belum diatur!"); return; }
  Logger.log("🔍 Mengecek DNS & Koneksi ke: " + wahaUrl);
  const options = { method: 'get', headers: apiKey ? { 'X-Api-Key': apiKey, 'accept': 'application/json' } : { 'accept': 'application/json' }, muteHttpExceptions: true };
  const startTime = new Date().getTime();
  try {
    const response = UrlFetchApp.fetch(`${wahaUrl}/api/sessions`, options);
    const duration = new Date().getTime() - startTime;
    const code = response.getResponseCode();
    if (code === 200) { Logger.log(`✅ KONEKSI BERHASIL! (${duration}ms)\n📦 Session Data: ` + response.getContentText().substring(0, 100) + "..."); }
    else if (code === 401 || code === 403) Logger.log(`⚠️ TERHUBUNG (${duration}ms), tapi DITOLAK (Unauthorized). Cek WAHA_API_KEY Anda!`);
    else Logger.log(`❌ GAGAL TERHUBUNG. HTTP Code: ${code}\nDetail: ` + response.getContentText());
  } catch (e) { Logger.log("❌ ERROR FATAL / DNS TIDAK DITEMUKAN: " + e.message); }
}

function setupTriggers() {
  ScriptApp.getProjectTriggers().forEach(t => { 
    if (['cleanOldCounters', 'processPendingKoordinat', 'processQueueTasks', 'cleanQueue'].includes(t.getHandlerFunction())) ScriptApp.deleteTrigger(t); 
  });
  ScriptApp.newTrigger('cleanOldCounters').timeBased().onWeekDay(ScriptApp.WeekDay.MONDAY).atHour(1).create();
  Logger.log('✅ Trigger Basic Terpasang (Semua proses berjalan secara murni sinkronus/saat itu juga)');
}

function cleanOldCounters() {
  const props = PropertiesService.getScriptProperties().getProperties(), today = Utilities.formatDate(new Date(), CONFIG.TZ, 'yyMMdd');
  Object.keys(props).forEach(k => { if (k.startsWith('ticket_counter_') && !k.includes(today)) PropertiesService.getScriptProperties().deleteProperty(k); });
}

function deleteTelegramWebhook() {
  const botToken = getProp_('BOT_TOKEN');
  const res = UrlFetchApp.fetch(`https://api.telegram.org/bot${botToken}/deleteWebhook`, { muteHttpExceptions: true });
  Logger.log('=== HASIL HAPUS WEBHOOK ===\nTelegram tidak akan lagi mengirim pesan masuk ke script ini.\nResponse: ' + res.getContentText());
}

// ═══════════════════════════════════════════════════════════════════════
//  OCR.gs — v2 (PATCHED)
// ═══════════════════════════════════════════════════════════════════════

const CONFIG_OCR = {
  SHEET_OCRDAPEL: 'dbase',
  DRIVE_FOLDER_ID: '15qWsqFLufop5_bpthZOqa0jjJPui0o3H',
  PROTECT_ST_LALU: true
};

// ═══════════════════════════════════════════════════════════════════════
//  ROUTER KHUSUS OCR FLUTTER
// ═══════════════════════════════════════════════════════════════════════
function handleFlutterOcrPost(e) {
  try {
    const payload = safeParseJson(e);

    if (Object.keys(payload).length === 0) {
      return jsonOutput({ status: 'error', error: 'Payload kosong atau tidak valid' });
    }

    const action = String(payload.action || '').trim().toLowerCase();

    if (action === 'ping') {
      return jsonOutput({ status: 'ok', service: 'ocr_bridge', message: 'OCR bridge ready' });
    }

    if (action !== 'sync_ocr_dapel') {
      return jsonOutput({
        status: 'error',
        error: 'Action tidak dikenali',
        received_action: payload.action ?? null
      });
    }

    const result = syncOcrDapel(payload);
    return jsonOutput(result);
  } catch (err) {
    if (typeof LoggerService !== 'undefined' && LoggerService.error) {
      LoggerService.error('OCR Bridge Error', { error: err.message, stack: err.stack });
    }
    return jsonOutput({ status: 'error', error: String(err) });
  }
}

// ═══════════════════════════════════════════════════════════════════════
//  FUNGSI UTAMA SINKRONISASI OCR KE GOOGLE SHEETS
// ═══════════════════════════════════════════════════════════════════════
function syncOcrDapel(data) {
  const sheetName = String(data.sheet_name || CONFIG_OCR.SHEET_OCRDAPEL).trim();
  const folderId = String(data.drive_folder_id || CONFIG_OCR.DRIVE_FOLDER_ID).trim();
  const idpel = String(data.idpel || '').trim();

  if (typeof LoggerService !== 'undefined' && LoggerService.info) {
    const debugPayload = Object.assign({}, data);
    delete debugPayload.image_base64;
    LoggerService.info('syncOcrDapel: payload diterima', debugPayload);
  }

  if (!idpel) {
    return { status: 'error', error: 'IDPEL tidak ditemukan dalam payload' };
  }

  let ss;
  try {
    ss = data.spreadsheet_id ? SpreadsheetApp.openById(data.spreadsheet_id) : SpreadsheetApp.getActiveSpreadsheet();
  } catch (err) {
    return { status: 'error', error: `Spreadsheet ID tidak valid / tidak bisa diakses: ${err.message}` };
  }

  const precheck = findIdpelRow_(ss, sheetName, idpel);
  if (precheck.error) {
    if (typeof LoggerService !== 'undefined' && LoggerService.warn) {
      LoggerService.warn('syncOcrDapel: precheck gagal, upload foto dibatalkan', { idpel, error: precheck.error });
    }
    return { status: 'error', error: precheck.error };
  }

  const fileName = data.file_name || `METER_${idpel}_${Date.now()}.jpg`;
  let fileUrl = '';
  let fileId = '';

  try {
    if (data.image_base64) {
      const rawBase64 = String(data.image_base64).replace(/^data:.+;base64,/, '');
      const blob = Utilities.newBlob(
        Utilities.base64Decode(rawBase64),
        data.content_type || 'image/jpeg',
        fileName
      );

      const folder = DriveApp.getFolderById(folderId);
      const file = folder.createFile(blob);
      try {
        file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      } catch (shareErr) {
        if (typeof LoggerService !== 'undefined' && LoggerService.warn) {
          LoggerService.warn('Gagal set sharing link: ' + shareErr.message);
        }
      }

      fileId = file.getId();
      fileUrl = `https://drive.google.com/file/d/${fileId}/view?usp=drivesdk`;
    }
  } catch (err) {
    return { status: 'error', error: `Gagal upload gambar: ${err.message}` };
  }

  const lock = LockService.getScriptLock();
  lock.waitLock(30000);

  try {
    const sheet = ss.getSheetByName(sheetName);
    if (!sheet) return { status: 'error', error: `Sheet '${sheetName}' tidak ditemukan` };

    const values = sheet.getDataRange().getValues();
    if (!values || values.length === 0) return { status: 'error', error: 'Sheet kosong' };

    let headers = values[0].map(normalizeHeader);

    const idpelCol = findHeaderIndex(headers, ['idpel', 'id pel', 'no pelanggan', 'id pelanggan']);
    if (idpelCol === -1) return { status: 'error', error: 'Kolom IDPEL tidak ditemukan' };

    const stLaluCol = ensureHeader(sheet, headers, data.stand_column || 'St Lalu');
    const hasilOcrCol = ensureHeader(sheet, headers, data.hasil_ocr_column || 'Hasil OCR');
    const linkCol = ensureHeader(sheet, headers, data.link_column || 'Link');
    const koordinatCol = ensureHeader(sheet, headers, data.koordinat_column || 'Koordinat');
    const koordinatLngCol = data.koordinat_lng_column ? ensureHeader(sheet, headers, data.koordinat_lng_column) : -1;
    const exifCol = data.exif_column ? ensureHeader(sheet, headers, data.exif_column) : -1;

    const petugasCol = ('petugas' in data) ? ensureHeader(sheet, headers, data.petugas_column || 'PERSONIL') : -1;
    const catatanCol = ('catatan' in data) ? ensureHeader(sheet, headers, data.keterangan_column || 'Catatan') : -1;
    const timeCol = ('timestamp' in data) ? ensureHeader(sheet, headers, 'Timestamp OCR') : -1;

    const target = idpel.toUpperCase();
    let targetRowIndex = -1;
    for (let i = 1; i < values.length; i++) {
      if (String(values[i][idpelCol] || '').trim().toUpperCase() === target) {
        targetRowIndex = i;
        break;
      }
    }

    if (targetRowIndex === -1) {
      return {
        status: 'error',
        error: `IDPEL ${idpel} tidak ditemukan di sheet ${sheetName} (berubah sejak validasi awal)`,
        orphan_file_id: fileId || null
      };
    }

    let rowData = values[targetRowIndex];
    while (rowData.length < headers.length) rowData.push('');

    if (data.hasil_ocr !== undefined) rowData[hasilOcrCol] = data.hasil_ocr;
    if (fileUrl) rowData[linkCol] = fileUrl;

    let stLaluUpdated = false;
    if (data.st_lalu !== undefined) {
      const currentStLalu = rowData[stLaluCol];
      const hasExistingValue = currentStLalu !== '' && currentStLalu !== null && currentStLalu !== undefined;
      const shouldProtect = CONFIG_OCR.PROTECT_ST_LALU && !data.force_overwrite_st_lalu && hasExistingValue;

      if (shouldProtect) {
        if (typeof LoggerService !== 'undefined' && LoggerService.warn) {
          LoggerService.warn('syncOcrDapel: st_lalu DIABAIKAN (proteksi aktif, sel sudah berisi nilai)', {
            idpel,
            nilai_dikirim: data.st_lalu,
            nilai_sheet_saat_ini: currentStLalu,
            catatan: 'Kirim force_overwrite_st_lalu=true jika memang ingin menimpa'
          });
        }
      } else {
        rowData[stLaluCol] = data.st_lalu;
        stLaluUpdated = true;
      }
    }

    if (data.koordinat !== undefined) {
      rowData[koordinatCol] = formatKoordinat_(data.koordinat);
    } else if (data.latitude !== undefined && data.longitude !== undefined) {
      const latStr = String(data.latitude).replace(/\./g, ',');
      const lngStr = String(data.longitude).replace(/\./g, ',');
      if (koordinatLngCol !== -1) {
        rowData[koordinatCol] = latStr;
        rowData[koordinatLngCol] = lngStr;
      } else {
        rowData[koordinatCol] = `${latStr}, ${lngStr}`;
      }
    }
    
    if (exifCol !== -1 && data.exif !== undefined) {
      rowData[exifCol] = data.exif;
    }

    if (petugasCol !== -1) {
      rowData[petugasCol] = data.petugas || '';
      if (!data.petugas && typeof LoggerService !== 'undefined' && LoggerService.warn) {
        LoggerService.warn('syncOcrDapel: field "petugas" dikirim tapi kosong', { idpel });
      }
    }
    if (catatanCol !== -1) rowData[catatanCol] = data.catatan || '';
    if (timeCol !== -1) rowData[timeCol] = data.timestamp || '';

    sheet.getRange(targetRowIndex + 1, 1, 1, headers.length).setValues([rowData]);

    return {
      status: 'ok',
      message: `Data pelanggan ${idpel} berhasil diperbarui`,
      data: {
        foto_url: fileUrl,
        foto_gdrive_id: fileId,
        koordinat_tersimpan: rowData[koordinatCol],
        st_lalu_ditulis: stLaluUpdated
      }
    };

  } catch (err) {
    if (typeof LoggerService !== 'undefined' && LoggerService.error) {
      LoggerService.error('syncOcrDapel Error', { error: err.message, stack: err.stack, idpel: idpel });
    }
    return { status: 'error', error: String(err) };
  } finally {
    lock.releaseLock();
  }
}

// ═══════════════════════════════════════════════════════════════════════
//  HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════

function findIdpelRow_(ss, sheetName, idpel) {
  const sheet = ss.getSheetByName(sheetName);
  if (!sheet) return { error: `Sheet '${sheetName}' tidak ditemukan` };

  const values = sheet.getDataRange().getValues();
  if (!values || values.length === 0) return { error: 'Sheet kosong' };

  const headers = values[0].map(normalizeHeader);
  const idpelCol = findHeaderIndex(headers, ['idpel', 'idpel_rp', 'id pel', 'no pelanggan', 'id pelanggan']);
  if (idpelCol === -1) return { error: 'Kolom IDPEL tidak ditemukan' };

  const target = String(idpel).trim().toUpperCase();
  for (let i = 1; i < values.length; i++) {
    if (String(values[i][idpelCol] || '').trim().toUpperCase() === target) {
      return { rowIndex: i };
    }
  }
  return { error: `IDPEL ${idpel} tidak ditemukan di sheet ${sheetName}` };
}

function formatKoordinat_(koordinatStr) {
  const str = String(koordinatStr);
  const firstComma = str.indexOf(',');

  if (firstComma === -1) {
    return str.replace(/\./g, ',');
  }

  const latPart = str.substring(0, firstComma).trim().replace(/\./g, ',');
  const lngPart = str.substring(firstComma + 1).trim().replace(/\./g, ',');
  return `${latPart}, ${lngPart}`;
}

function safeParseJson(e) {
  try {
    if (e && e.postData && e.postData.contents) {
      return JSON.parse(e.postData.contents);
    }
  } catch (err) {} 
  return {};
}

function normalizePathInfo(pathInfo) {
  return String(pathInfo || '').trim().replace(/^\/+|\/+$/g, '').toLowerCase();
}

function normalizeHeader(value) {
  return String(value || '').trim().toLowerCase();
}

function findHeaderIndex(headers, aliases) {
  const normalizedAliases = aliases.map(alias => normalizeHeader(alias));
  for (let i = 0; i < headers.length; i++) {
    if (normalizedAliases.includes(headers[i])) return i;
  }
  return -1;
}

function ensureHeader(sheet, headers, headerName) {
  const normalized = normalizeHeader(headerName);
  const existing = headers.indexOf(normalized);

  if (existing !== -1) return existing;

  const newCol = headers.length + 1;
  sheet.getRange(1, newCol).setValue(headerName);
  headers.push(normalized);
  return newCol - 1;
}

function jsonOutput(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

function testOcrPing() {
  return handleFlutterOcrPost({
    postData: { contents: JSON.stringify({ action: 'ping' }) }
  });
}

function testSyncProtectStLalu() {
  const result = syncOcrDapel({
    sheet_name: CONFIG_OCR.SHEET_OCRDAPEL,
    idpel: '6120010003',
    hasil_ocr: '3010',
    st_lalu: '3010' 
  });
  Logger.log(JSON.stringify(result, null, 2));
  return result;
}


function doSubmitPengaduan(payload) {
  try {
    var res = PengaduanService.submit(payload.data);
    if (!res.ok) {
      return { ok: false, status: "error", message: res.message };
    }
    return { ok: true, status: "success", ticket: res.ticket, message: "Pengaduan berhasil disubmit" };
  } catch (e) {
    Logger.log("doSubmitPengaduan Error: " + e.message);
    return { ok: false, status: "error", message: e.message };
  }
}

// =======================================================================
// [API BARU] GENERIC HANDLERS
// =======================================================================

function findRowHandler(data) {
  var sheetName = data.sheetName;
  var idpel = data.idpel;
  var keyColumn = data.keyColumn || 'IDPEL';
  
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName(sheetName);
  
  if (!sheet) return {status: 'error', message: 'Sheet ' + sheetName + ' tidak ditemukan.'};
  
  var values = sheet.getDataRange().getValues();
  if (values.length < 2) return {status: 'error', message: 'Sheet kosong.'};
  
  var headers = values[0];
  var searchColIndex = -1;
  
  var possibleKeys = [keyColumn.toLowerCase()];
  if (keyColumn === 'IDPEL') {
    possibleKeys.push('id pelanggan', 'no pelanggan', 'nometer', 'no meter');
  } else if (keyColumn === 'Ticket') {
    possibleKeys.push('ticket', 'no ticket', 'tiket', 'no tiket');
  }
  
  for (var i = 0; i < headers.length; i++) {
    var h = String(headers[i]).toLowerCase().trim();
    if (possibleKeys.indexOf(h) !== -1) {
      searchColIndex = i;
      break;
    }
  }
  
  if (searchColIndex === -1) {
    for (var i = 0; i < headers.length; i++) {
      if (String(headers[i]).toLowerCase().trim() === keyColumn.toLowerCase().trim()) {
        searchColIndex = i;
        break;
      }
    }
  }
  
  if (searchColIndex === -1) return {status: 'error', message: 'Kolom kunci ' + keyColumn + ' tidak ditemukan.'};
  
  var rowIndex = -1;
  for (var i = 1; i < values.length; i++) {
    if (String(values[i][searchColIndex]).trim() === String(idpel).trim()) {
      rowIndex = i;
      break;
    }
  }
  
  if (rowIndex === -1) return {status: 'error', message: 'Data dengan ' + keyColumn + ' = ' + idpel + ' tidak ditemukan.'};
  
  var rowData = values[rowIndex];
  var rowObj = {};
  for (var i = 0; i < headers.length; i++) {
    var headerName = String(headers[i]).trim();
    if (headerName !== '') rowObj[headerName] = rowData[i];
  }
  
  return { status: 'success', row: rowObj, rowValues: rowData };
}

function updateRowCellsHandler(data) {
  var sheetName = data.sheetName;
  var idpel = data.idpel;
  var keyColumn = data.keyColumn || 'IDPEL';
  var filterColumn = data.filterColumn;
  var filterValue = data.filterValue;
  var updates = data.updates; 
  var updatesByIndex = data.updatesByIndex;
  
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName(sheetName);
  
  if (!sheet) return {status: 'error', message: 'Sheet ' + sheetName + ' tidak ditemukan.'};
  
  var values = sheet.getDataRange().getValues();
  if (values.length < 2) return {status: 'error', message: 'Sheet kosong.'};
  
  var headers = values[0];
  var searchColIndex = -1;
  var filterColIndex = -1;
  
  for (var i = 0; i < headers.length; i++) {
    var h = String(headers[i]).toLowerCase().trim();
    if (h === keyColumn.toLowerCase().trim()) searchColIndex = i;
    if (filterColumn && h === filterColumn.toLowerCase().trim()) filterColIndex = i;
  }
  
  if (searchColIndex === -1 && keyColumn === 'IDPEL') {
    var possibleKeys = ['idpel', 'idpel_rp', 'id pelanggan', 'no pelanggan', 'nometer', 'no meter'];
    for (var i = 0; i < headers.length; i++) {
       var h = String(headers[i]).toLowerCase().trim();
       if (possibleKeys.indexOf(h) !== -1) {
         searchColIndex = i;
         break;
       }
    }
  }
  
  if (searchColIndex === -1) return {status: 'error', message: 'Kolom kunci ' + keyColumn + ' tidak ditemukan.'};
  
  var rowIndex = -1;
  // Cari dari bawah ke atas agar mendapat entri terbaru
  for (var i = values.length - 1; i > 0; i--) {
    if (String(values[i][searchColIndex]).trim() === String(idpel).trim()) {
      if (filterColumn && filterColIndex !== -1) {
        if (String(values[i][filterColIndex]).trim().toUpperCase() !== String(filterValue).trim().toUpperCase()) {
          continue; // Skip this row since it doesn't match the filter
        }
      }
      rowIndex = i;
      break;
    }
  }
  
  if (rowIndex === -1) return {status: 'error', message: 'Data dengan ' + keyColumn + ' = ' + idpel + ' tidak ditemukan.'};
  
  var sheetRow = rowIndex + 1;
  
  if (updates) {
    var keys = Object.keys(updates);
    for (var k = 0; k < keys.length; k++) {
      var updateKey = keys[k];
      var updateVal = updates[updateKey];
      var targetCol = -1;
      for (var i = 0; i < headers.length; i++) {
        if (String(headers[i]).toLowerCase().trim() === String(updateKey).toLowerCase().trim()) {
          targetCol = i + 1;
          break;
        }
      }
      if (targetCol !== -1) {
        sheet.getRange(sheetRow, targetCol).setValue(updateVal);
      }
    }
  }
  
  if (updatesByIndex) {
    var indexKeys = Object.keys(updatesByIndex);
    for (var k = 0; k < indexKeys.length; k++) {
      var colIdx = parseInt(indexKeys[k]);
      var updateVal = updatesByIndex[indexKeys[k]];
      if (colIdx > 0) {
        sheet.getRange(sheetRow, colIdx).setValue(updateVal);
      }
    }
  }
  
  return {status: 'success'};
}

function uploadPhotoGeneric(data) {
  try {
    var base64 = data.base64;
    var filename = data.filename || ('foto_' + new Date().getTime() + '.jpg');
    var folderId = FOLDER_ID_SRC1; 

    if (!base64) {
      return { status: 'error', message: 'Data foto (base64) kosong / tidak dikirim.' };
    }

    var base64Data = base64.indexOf(',') > -1 ? base64.split(',')[1] : base64;

    var folder = DriveApp.getFolderById(folderId);
    var byteString = Utilities.base64Decode(base64Data);
    var blob = Utilities.newBlob(byteString, 'image/jpeg', filename);
    var file = folder.createFile(blob);

    try {
      file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    } catch (sharingError) {
      console.log('setSharing gagal (diabaikan, file tetap tersimpan): ' + String(sharingError));
    }

    var url = file.getUrl();
    
    return { status: 'success', url: url };
  } catch(e) {
    return { status: 'error', message: String(e) };
  }
}

function notifySelesaiGeneric(data) {
  try {
      var koordinat = "";
      if (data.lat && data.lng) {
         koordinat = data.lat + "," + data.lng;
      } else if (data.koordinatPetugas) {
         koordinat = data.koordinatPetugas;
      }

      var webappData = {
        tiket: data.ticket,
        idpel: data.idpel,
        nama: data.nama,
        pengaduan: data.pengaduan,
        petugas: data.petugas,
        tindakan: data.tindakan,
        fotoSeb: data.fotoSeb,
        fotoSes: data.fotoSes,
        koordinatPetugas: koordinat
      };

      if (typeof notifSelesai_ === 'function') {
         notifSelesai_(webappData);
      }
      
      return { status: 'success' };
  } catch(e) {
      return { status: 'error', message: String(e) };
  }
}
