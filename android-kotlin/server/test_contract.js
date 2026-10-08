// Uji KONTRAK: payload persis seperti yang dikirim aplikasi Kotlin → Code.patched.gs (tiruan Drive & Sheet)
const vm = require('vm'), fs = require('fs'), crypto = require('crypto');
const src = fs.readFileSync(__dirname + '/Code.patched.gs', 'utf8').replace(/^\uFEFF/, '');

const props = {}; const cacheStore = {}; const drive = {};   // drive[folderId] = [file...]
const spreadsheets = {};
const out = (t) => ({ _t: t, setMimeType() { return this; } });

function mkSheet(rows) {
  const width = () => Math.max(0, ...rows.map(r => r.length));
  const pad = () => rows.map(r => { const c = r.slice(); while (c.length < width()) c.push(''); return c; });
  return {
    rows,
    getDataRange: () => ({ getValues: () => pad(), getDisplayValues: () => pad().map(r => r.map(String)) }),
    getLastColumn: () => width(), getLastRow: () => rows.length,
    getRange(r, c, nr = 1, nc = 1) {
      return {
        setValue(v) { while (rows.length < r) rows.push([]); while (rows[r - 1].length < c) rows[r - 1].push(''); rows[r - 1][c - 1] = v; },
        setValues(vals) { vals.forEach((row, i) => row.forEach((v, j) => { while (rows.length < r + i) rows.push([]); while (rows[r - 1 + i].length < c + j) rows[r - 1 + i].push(''); rows[r - 1 + i][c - 1 + j] = v; })); },
        getValue() { return (rows[r - 1] || [])[c - 1] === undefined ? '' : rows[r - 1][c - 1]; },
        getValues() { const o = []; for (let i = 0; i < nr; i++) { const rr = []; for (let j = 0; j < nc; j++) rr.push((rows[r - 1 + i] || [])[c - 1 + j] ?? ''); o.push(rr); } return o; },
      };
    },
  };
}
const book = { getSheetByName: (n) => spreadsheets[n] || null };

const ctx = {
  console: { log() {}, error() {}, warn() {} }, Date, Math, JSON, String, Number, Object, Array, Boolean, Error, encodeURIComponent, parseInt, RegExp, Buffer,
  Session: { getScriptTimeZone: () => 'Asia/Jakarta', getActiveUser: () => ({ getEmail: () => 'x@y' }) },
  ContentService: { createTextOutput: (t) => out(t), MimeType: { JSON: 'json', TEXT: 'text' } },
  PropertiesService: { getScriptProperties: () => ({ getProperty: (k) => (k in props ? props[k] : null), setProperty: (k, v) => { props[k] = v; }, getProperties: () => props }) },
  CacheService: { getScriptCache: () => ({ get: (k) => (k in cacheStore ? cacheStore[k] : null), put: (k, v) => { cacheStore[k] = v; }, remove: (k) => { delete cacheStore[k]; } }) },
  LockService: { getScriptLock: () => ({ waitLock() {}, releaseLock() {} }) },
  Utilities: {
    getUuid: () => crypto.randomUUID(), sleep() {},
    base64EncodeWebSafe: (x) => Buffer.from(x).toString('base64url') + '=',
    base64DecodeWebSafe: (s) => Array.from(Buffer.from(String(s).replace(/=+$/, ''), 'base64url')),
    base64Decode: (s) => Array.from(Buffer.from(String(s), 'base64')),
    computeHmacSha256Signature: (v, k) => Array.from(crypto.createHmac('sha256', k).update(v).digest()),
    newBlob: (bytes, mime, name) => (name === undefined ? { getDataAsString: () => Buffer.from(bytes).toString('utf8') } : { bytes, mime, name }),
    formatDate: (d, tz, f) => '2026-10-07 10:00:00',
  },
  MimeType: { JPEG: 'image/jpeg' },
  SpreadsheetApp: { openById: () => book, getActiveSpreadsheet: () => book, flush() {} },
  DriveApp: {
    Access: { ANYONE_WITH_LINK: 'ANYONE_WITH_LINK' }, Permission: { VIEW: 'VIEW' },
    getFolderById: (id) => ({ createFile: (blob) => { const f = { id: 'FILE' + ((drive[id] = drive[id] || []).length + 1), blob, shared: false }; drive[id].push(f); return { getId: () => f.id, getUrl: () => 'https://drive.google.com/file/d/' + f.id + '/view?usp=drivesdk', setSharing() { f.shared = true; } }; } }),
  },
  UrlFetchApp: { fetch() { throw new Error('tidak boleh memanggil jaringan'); } },
};
vm.createContext(ctx); vm.runInContext(src, ctx);
const run = (c) => vm.runInContext(c, ctx);
const post = (obj, param) => JSON.parse(run(`doPost(${JSON.stringify({ postData: { contents: JSON.stringify(obj) }, parameter: param || {} })})`)._t);

let fails = 0;
const check = (n, ok, extra) => { console.log((ok ? 'OK   ' : 'GAGAL') + ' ' + n + (ok ? '' : '  → ' + JSON.stringify(extra))); if (!ok) fails++; };

// ─────────── Berkas foto: seperti keluaran ImageCompress (JPEG ~ 120 KB, dibungkus data-URI) ───────────
const jpeg = Buffer.concat([Buffer.from([0xff, 0xd8, 0xff, 0xe0]), crypto.randomBytes(120 * 1024), Buffer.from([0xff, 0xd9])]);
const dataUri = 'data:image/jpeg;base64,' + jpeg.toString('base64');
const FOLDER1 = run('FOLDER_ID_SRC1'); const FOLDER_OCR = run('CONFIG_OCR.DRIVE_FOLDER_ID');

// 1) upload_photo (Kunjungan/Perbaikan)
let r = post({ action: 'upload_photo', base64: dataUri, filename: '2610071030_before.jpg', target: 'perbaikan' });
const f1 = (drive[FOLDER1] || [])[0];
check('upload_photo: sukses + URL Drive', r.status === 'success' && /drive\.google\.com\/file\/d\/FILE1/.test(r.url), r);
check('upload_photo: byte file identik dengan foto (data-URI dibuang prefiksnya)', f1 && Buffer.from(f1.blob.bytes).equals(jpeg));
check('upload_photo: nama file & tipe JPEG, dibagikan via tautan', f1 && f1.blob.name === '2610071030_before.jpg' && f1.blob.mime === 'image/jpeg' && f1.shared);
r = post({ action: 'upload_photo', base64: '', filename: 'x.jpg' });
check('upload_photo kosong → galat jelas', r.status === 'error');

// 2) Perbaikan → REPORT (kolom *_RP)
const HEADERS = run('CONFIG.HEADERS');
check('CONFIG.HEADERS terbaca', Array.isArray(HEADERS) && HEADERS.includes('Ticket_RP'), HEADERS);
const report = mkSheet([HEADERS.slice(), HEADERS.map((h) => ({ Ticket_RP: 'PGD-KD-261007-001', IDPEL_RP: '6120990001', PENGADUAN_RP: 'GAS BAU', STATUS_RP: 'OPEN' }[h] || ''))]);
spreadsheets['REPORT'] = report;
const sebUrl = r.url; const sesUrl = 'https://drive.google.com/file/d/FILE9/view';
const updPerbaikan = {
  STATUS_RP: 'SELESAI', Petugas_RP: 'BUDI', PENGADUAN_RP: 'KEBOCORAN', Tindakan_RP: 'PENGGANTIAN MGRT',
  Foto_Seb_RP: sebUrl || 'u1', Foto_Ses_RP: sesUrl, Waktu_Seb_RP: '2026-10-07 10:30:00', Waktu_Ses_RP: '2026-10-07 10:45:00',
  Latitude_RP: '-7.2575', Longitude_RP: '112.7521',
  NoMGRT_RP: 'OLD-111', Angka_MGRT_RP: '00450', No_MGRT_Baru_RP: 'NEW-222', Angka_MGRT_Baru_RP: '00000',
  // nama lama (kompatibilitas) — tidak ada di REPORT → harus dilewati & dilaporkan, bukan membuat galat
  Status: 'SELESAI', Petugas: 'BUDI', 'Foto Sebelum': 'u1', 'No MGRT': 'OLD-111',
};
let fr = post({ action: 'find_row', sheetName: 'REPORT', idpel: 'PGD-KD-261007-001', keyColumn: 'Ticket_RP' });
check('Perbaikan: find_row by Ticket_RP', fr.status === 'success' && fr.row.STATUS_RP === 'OPEN', fr);
r = post({ action: 'update_row_cells', sheetName: 'REPORT', idpel: 'PGD-KD-261007-001', keyColumn: 'Ticket_RP', updates: updPerbaikan });
const row = Object.fromEntries(HEADERS.map((h, i) => [h, report.rows[1][i]]));
check('Perbaikan: semua kolom *_RP tertulis benar', Object.keys(updPerbaikan).filter((k) => k.endsWith('_RP')).every((k) => String(row[k]) === String(updPerbaikan[k])), row);
check('Perbaikan: MGRT LAMA ke NoMGRT_RP/Angka_MGRT_RP, BARU ke *_Baru_RP', row.NoMGRT_RP === 'OLD-111' && row.No_MGRT_Baru_RP === 'NEW-222' && row.Angka_MGRT_RP === '00450' && row.Angka_MGRT_Baru_RP === '00000', row);
check('Perbaikan: server melaporkan kolom terlewati (nama lama saja)', r.status === 'success' && JSON.stringify(r.skipped.sort()) === JSON.stringify(['Foto Sebelum', 'No MGRT', 'Petugas', 'Status'].sort()), r);
const reqMissing = r.skipped.filter((k) => Object.keys(updPerbaikan).filter((x) => x.endsWith('_RP')).includes(k));
check('Perbaikan: tidak ada kolom WAJIB (*_RP) yang terlewati → tanpa peringatan di app', reqMissing.length === 0);
// sheet tanpa kolom Foto_Ses_RP → harus terdeteksi
const noSes = HEADERS.filter((h) => h !== 'Foto_Ses_RP');
spreadsheets['REPORT'] = mkSheet([noSes, noSes.map((h) => ({ Ticket_RP: 'T9' }[h] || ''))]);
r = post({ action: 'update_row_cells', sheetName: 'REPORT', idpel: 'T9', keyColumn: 'Ticket_RP', updates: { STATUS_RP: 'SELESAI', Foto_Ses_RP: 'u' } });
check('Kolom wajib hilang di sheet → dilaporkan lewat skipped', r.status === 'success' && r.skipped.includes('Foto_Ses_RP'), r);

// 3) Kunjungan → dbase
const DB = ['IDPEL', 'NAMA', 'ALAMAT', 'MRS', 'RUPIAH', 'PERSONIL', 'Hasil_OCR', 'Link_OCR', 'KJG', 'LINK_SEB', 'LINK_SES', 'LATITUDE_KJG', 'LONGITUDE_KJG', 'EXIF_SEB', 'EXIF_SES', 'NOMGRT_BARU', 'STMGRT_BARU', 'TGL_KJG', 'ORDER'];
const mkDb = () => mkSheet([DB.slice(), ['6120990001', 'SITI', 'JL. MAWAR 1', 'KD', '1250', 'BUDI SANTOSO', '', '', '', '', '', '', '', '', '', '', '', '', '1']]);
spreadsheets['dbase'] = mkDb();
const updVisit = { KJG: 'CABUT', LINK_SEB: 'u1', LINK_SES: 'u2', LATITUDE_KJG: '-7.257500', LONGITUDE_KJG: '112.752100', NOMGRT_BARU: 'N-1', STMGRT_BARU: '120', TGL_KJG: '2026-10-07 11:00:00', ORDER: '2' };
r = post({ action: 'update_row_cells', sheetName: 'dbase', idpel: '6120990001', updates: updVisit });
let drow = Object.fromEntries(DB.map((h, i) => [h, spreadsheets['dbase'].rows[1][i]]));
check('Kunjungan: kolom dbase tertulis (KJG, LINK_*, koordinat, MGRT, TGL, ORDER)', Object.keys(updVisit).every((k) => String(drow[k]) === String(updVisit[k])) && r.status === 'success' && r.skipped.length === 0, { drow, r });
fr = post({ action: 'find_row', sheetName: 'dbase', idpel: '6120990001' });
check('Pengaduan: find_row dbase → NAMA/ALAMAT/MRS untuk autofill', fr.status === 'success' && fr.row.NAMA === 'SITI' && fr.row.ALAMAT === 'JL. MAWAR 1' && fr.row.MRS === 'KD', fr);

// 4) Pembukaan Aliran: filter bawah-ke-atas → baris TERBARU yang cocok
spreadsheets['REPORT'] = mkSheet([HEADERS.slice(),
  HEADERS.map((h) => ({ Ticket_RP: 'T1', IDPEL_RP: '777', PENGADUAN_RP: 'BUKA SEGEL', STATUS_RP: 'SELESAI' }[h] || '')),
  HEADERS.map((h) => ({ Ticket_RP: 'T2', IDPEL_RP: '777', PENGADUAN_RP: 'BUKA SEGEL', STATUS_RP: 'OPEN' }[h] || ''))]);
r = post({ action: 'update_row_cells', sheetName: 'REPORT', idpel: '777', keyColumn: 'IDPEL_RP', filterColumn: 'PENGADUAN_RP', filterValue: 'BUKA SEGEL', updates: { STATUS_RP: 'SELESAI', Foto_Seb_RP: 'a', Foto_Ses_RP: 'b' } });
const rep = spreadsheets['REPORT'].rows; const iS = HEADERS.indexOf('STATUS_RP'), iF = HEADERS.indexOf('Foto_Seb_RP');
check('Pembukaan: yang diperbarui baris T2 (OPEN), baris lama T1 tidak disentuh', rep[2][iS] === 'SELESAI' && rep[2][iF] === 'a' && rep[1][iF] === '', rep);

// 5) Pencatatan Meter: sync_ocr_dapel (rute ?api=ocr) dengan payload Kotlin
spreadsheets['dbase'] = mkDb();
const kt = {
  action: 'sync_ocr_dapel', spreadsheet_id: 'ID-PALSU-DARI-KLIEN', sheet_name: 'dbase', drive_folder_id: 'FOLDER-PALSU-DARI-KLIEN',
  idpel: '6120990001', st_lalu: '01234', hasil_ocr: '01234', stand_column: 'RUPIAH', hasil_ocr_column: 'Hasil_OCR', link_column: 'Link_OCR',
  koordinat_column: 'LATITUDE_OCR', koordinat_lng_column: 'LONGITUDE_OCR', keterangan_column: 'CATATAN', exif_column: 'EXIF_OCR',
  keterangan: 'Meter Tertimbun', timestamp: '2026-10-07T10:30:00.123', exif: '2026-10-07 10:30:00', petugas: 'BUDI SANTOSO',
  latitude: -7.2575, longitude: 112.7521, catatan: 'Meter Tertimbun',
  image_base64: jpeg.toString('base64'), file_name: 'meter-upload-6120990001-1790000000000.jpg', content_type: 'image/jpeg',
};
r = post(kt, { api: 'ocr' });
const sheetRows = spreadsheets['dbase'].rows; const H = sheetRows[0]; const get = (n) => sheetRows[1][H.indexOf(n)];
check('OCR sync: respons ok + foto_url Drive', r.status === 'ok' && /drive\.google\.com\/file\/d\//.test(r.data.foto_url), r);
check('OCR sync: Hasil_OCR terisi, RUPIAH (stand lalu) DILINDUNGI (tetap 1250)', get('Hasil_OCR') === '01234' && String(get('RUPIAH')) === '1250', { h: get('Hasil_OCR'), rp: get('RUPIAH') });
check('OCR sync: LATITUDE_OCR & LONGITUDE_OCR terpisah (bug lama: lat berisi "lat, lng")', get('LATITUDE_OCR') === '-7,2575' && get('LONGITUDE_OCR') === '112,7521', { lat: get('LATITUDE_OCR'), lng: get('LONGITUDE_OCR') });
check('OCR sync: EXIF_OCR terisi', get('EXIF_OCR') === '2026-10-07 10:30:00', get('EXIF_OCR'));
check('OCR sync: PERSONIL = nama petugas, CATATAN, Timestamp OCR, Link_OCR', get('PERSONIL') === 'BUDI SANTOSO' && get('CATATAN') === 'Meter Tertimbun' && get('Timestamp OCR') && /drive\.google\.com/.test(get('Link_OCR')), sheetRows[1]);
const fo = (drive[FOLDER_OCR] || [])[0];
check('OCR sync: file masuk folder OCR TETAP (folder dari klien diabaikan) & byte identik', fo && Buffer.from(fo.blob.bytes).equals(jpeg) && !drive['FOLDER-PALSU-DARI-KLIEN'], Object.keys(drive));
// admin: tanpa petugas → PERSONIL tidak tertimpa
spreadsheets['dbase'] = mkDb();
const adminPayload = { ...kt }; delete adminPayload.petugas; delete adminPayload.image_base64; delete adminPayload.file_name; delete adminPayload.content_type;
r = post(adminPayload, { api: 'ocr' });
check('OCR sync (admin, tanpa field petugas): PERSONIL penugasan tidak berubah', spreadsheets['dbase'].rows[1][DB.indexOf('PERSONIL')] === 'BUDI SANTOSO' || spreadsheets['dbase'].rows[1][H.indexOf('PERSONIL')] === 'BUDI SANTOSO');
// IDPEL tidak ada → foto TIDAK diunggah (tanpa file yatim)
const before = (drive[FOLDER_OCR] || []).length;
r = post({ ...kt, idpel: '000' }, { api: 'ocr' });
check('OCR sync: IDPEL tak ada → galat & tidak ada file yatim di Drive', r.status === 'error' && (drive[FOLDER_OCR] || []).length === before, r);

// 6) Notifikasi selesai: kontrak field (tanpa jaringan → best-effort tidak boleh melempar)
r = post({ action: 'notify_selesai', ticket: 'T2', idpel: '777', nama: 'X', pengaduan: 'GAS BAU', petugas: 'BUDI', tindakan: 'BUKA SEGEL', fotoSeb: 'u', fotoSes: 'u', lat: '-7.2', lng: '112.7' });
check('notify_selesai: respons terstruktur (tidak melempar walau kirim notifikasi gagal)', typeof r === 'object' && (r.status === 'success' || r.ok === true || r.status === 'error'), r);

console.log(fails === 0 ? '\nSEMUA LULUS' : `\n${fails} GAGAL`);
process.exit(fails ? 1 : 0);
