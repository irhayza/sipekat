const vm = require('vm'), fs = require('fs'), crypto = require('crypto');
let src = fs.readFileSync(__dirname + '/Code.patched.gs', 'utf8').replace(/^\uFEFF/, '');

// ── Mock lingkungan Apps Script ──
const props = {}; const cacheStore = {}; const sheets = {};
const out = (t) => ({ _t: t, setMimeType() { return this; }, getContent: () => t });
const ctx = {
  console, Date, Math, JSON, String, Number, Object, Array, Boolean, Error, encodeURIComponent, parseInt, RegExp,
  Session: { getScriptTimeZone: () => 'Asia/Jakarta' },
  ContentService: { createTextOutput: (t) => out(t), MimeType: { JSON: 'json', TEXT: 'text' } },
  PropertiesService: { getScriptProperties: () => ({ getProperty: (k) => (k in props ? props[k] : null), setProperty: (k, v) => { props[k] = v; } }) },
  CacheService: { getScriptCache: () => ({ get: (k) => (k in cacheStore ? cacheStore[k] : null), put: (k, v) => { cacheStore[k] = v; }, remove: (k) => { delete cacheStore[k]; } }) },
  LockService: { getScriptLock: () => ({ waitLock() {}, releaseLock() {} }) },
  Utilities: {
    getUuid: () => crypto.randomUUID(),
    base64EncodeWebSafe: (x) => Buffer.from(x).toString('base64url') + '=',   // GAS menyertakan padding
    base64DecodeWebSafe: (s) => Array.from(Buffer.from(String(s).replace(/=+$/, ''), 'base64url')),
    computeHmacSha256Signature: (v, k) => Array.from(crypto.createHmac('sha256', k).update(v).digest()),
    newBlob: (bytes) => ({ getDataAsString: () => Buffer.from(bytes).toString('utf8') }),
    formatDate: () => '20261007',
  },
  SpreadsheetApp: { openById: () => ({ getSheetByName: (n) => sheets[n] || null }) },
  UrlFetchApp: { fetch: (...a) => ctx.__fetch(...a) },
  DriveApp: {}, MimeType: { JPEG: 'image/jpeg' },
};
vm.createContext(ctx);
vm.runInContext(src, ctx);
const run = (code) => vm.runInContext(code, ctx);

let fails = 0;
const check = (n, ok) => { console.log((ok ? 'OK   ' : 'GAGAL') + ' ' + n); if (!ok) fails++; };

// Sheet LOGIN tiruan
const mkSheet = (rows) => ({ rows, getDataRange() { return { getValues: () => rows }; }, getRange(r, c) { return { setValue: (v) => { rows[r - 1][c - 1] = v; } }; } });
sheets['LOGIN'] = mkSheet([['email', 'password', 'nama'], ['SDA01', 'rahasia1', 'Budi Santoso'], ['admin1', 12345, 'Admin']]);

// ── Token ──
const tok = run("signToken_('sda01')");
check('token valid → email', run(`verifyToken_(${JSON.stringify(tok)})`) === 'sda01');
check('token dirusak → ditolak', run(`verifyToken_(${JSON.stringify(tok.slice(0, -3) + 'AAA')})`) === '');
check('token sampah → ditolak', run("verifyToken_('abc')") === '' && run("verifyToken_('')") === '');
props['TOKEN_TTL_DAYS'] = '-1';
check('token kedaluwarsa → ditolak', run(`verifyToken_(signToken_('x'))`) === '');
delete props['TOKEN_TTL_DAYS'];

// ── Login ──
let r = run("loginHandler_({user:' SDA01 ', password:'rahasia1'})");
check('login benar (case/trim email)', r.status === 'success' && r.nama === 'Budi Santoso' && r.email === 'sda01' && run(`verifyToken_(${JSON.stringify(r.token)})`) === 'sda01');
r = run("loginHandler_({user:'admin1', password:'12345'})");
check('login password numerik', r.status === 'success');
r = run("loginHandler_({user:'sda01', password:'salah'})");
check('login salah → galat generik', r.status === 'error' && /salah/i.test(r.message) && !r.token);
for (let i = 0; i < 10; i++) run("loginHandler_({user:'brute', password:'x'})");
r = run("loginHandler_({user:'brute', password:'x'})");
check('throttle setelah 10 gagal', r.status === 'error' && /Terlalu banyak/.test(r.message));
r = run("loginHandler_({user:'', password:''})");
check('login kosong ditolak', r.status === 'error');

// ── Ganti password ──
const t1 = run("signToken_('sda01')");
r = run(`changePasswordHandler_({user:'sda01', oldPassword:'rahasia1', newPassword:'baru123', token:'zzz'})`);
check('ganti password tanpa token sah ditolak', r.code === 'AUTH');
r = run(`changePasswordHandler_({user:'admin1', oldPassword:'12345', newPassword:'baru123', token:${JSON.stringify(t1)}})`);
check('token milik user lain ditolak', r.code === 'AUTH');
r = run(`changePasswordHandler_({user:'sda01', oldPassword:'rahasia1', newPassword:'abc', token:${JSON.stringify(t1)}})`);
check('password baru < 6 ditolak', r.status === 'error' && /minimal 6/.test(r.message));
r = run(`changePasswordHandler_({user:'sda01', oldPassword:'salah', newPassword:'baru123', token:${JSON.stringify(t1)}})`);
check('password lama salah ditolak', r.status === 'error' && /lama salah/.test(r.message));
r = run(`changePasswordHandler_({user:'sda01', oldPassword:'rahasia1', newPassword:'baru123', token:${JSON.stringify(t1)}})`);
check('ganti password berhasil + tertulis di sheet', r.status === 'success' && sheets['LOGIN'].rows[1][1] === 'baru123');
check('login dengan password baru', run("loginHandler_({user:'sda01', password:'baru123'})").status === 'success');

// ── Gemini ──
let lastFetch = null;
ctx.__fetch = (url, opt) => { lastFetch = { url, opt }; return { getResponseCode: () => 200, getContentText: () => JSON.stringify({ candidates: [{ content: { parts: [{ text: JSON.stringify({ angka: '01234', jumlah_digit: 5, terbaca: true }) }] } }] }) }; };
const t2 = run("signToken_('sda01')");
r = run(`geminiOcrHandler_({token:${JSON.stringify(t2)}, image_base64:'AAAA'})`);
check('Gemini tanpa API key di server → CONFIG', r.code === 'CONFIG');
props['GEMINI_API_KEY'] = 'KUNCI-RAHASIA';
r = run(`geminiOcrHandler_({token:'bad', image_base64:'AAAA'})`);
check('Gemini tanpa token sah → AUTH', r.code === 'AUTH' && lastFetch === null);
r = run(`geminiOcrHandler_({token:${JSON.stringify(t2)}, image_base64:'data:image/jpeg;base64,AAAA'})`);
check('Gemini sukses → angka/terbaca', r.status === 'success' && r.angka === '01234' && r.terbaca === true && r.jumlah_digit === 5);
check('kunci dikirim lewat header (bukan URL) & tidak bocor di respons', lastFetch.opt.headers['x-goog-api-key'] === 'KUNCI-RAHASIA' && !lastFetch.url.includes('KUNCI') && !JSON.stringify(r).includes('KUNCI'));
check('prefix data URI dibuang', JSON.parse(lastFetch.opt.payload).contents[0].parts[0].inlineData.data === 'AAAA');
r = run(`geminiOcrHandler_({token:${JSON.stringify(t2)}, image_base64:'${'A'.repeat(4000001)}'})`);
check('foto terlalu besar ditolak', r.status === 'error' && /terlalu besar/.test(r.message));
ctx.__fetch = () => ({ getResponseCode: () => 429, getContentText: () => 'quota' });
r = run(`geminiOcrHandler_({token:${JSON.stringify(t2)}, image_base64:'AAAA'})`);
check('HTTP 429 dipetakan pesan ramah', r.status === 'error' && /Kuota/.test(r.message));
props['GEMINI_LIMIT'] = '2';
for (const k of Object.keys(cacheStore)) if (k.startsWith('gemq_')) delete cacheStore[k];
ctx.__fetch = () => ({ getResponseCode: () => 200, getContentText: () => JSON.stringify({ candidates: [{ content: { parts: [{ text: '{"angka":"1","jumlah_digit":1,"terbaca":false}' }] } }] }) });
run(`geminiOcrHandler_({token:${JSON.stringify(t2)}, image_base64:'AAAA'})`); run(`geminiOcrHandler_({token:${JSON.stringify(t2)}, image_base64:'AAAA'})`);
r = run(`geminiOcrHandler_({token:${JSON.stringify(t2)}, image_base64:'AAAA'})`);
check('batas per-petugas berlaku', r.code === 'QUOTA');

// ── Penjaga aksi, sheet sensitif ──
check('REQUIRE_TOKEN mati → lolos', run("authGuard_({})") === null);
props['REQUIRE_TOKEN'] = 'true';
check('REQUIRE_TOKEN nyala tanpa token → AUTH', run("authGuard_({})").code === 'AUTH');
check('REQUIRE_TOKEN nyala + token sah → lolos', run(`authGuard_({token:${JSON.stringify(t2)}})`) === null);
delete props['REQUIRE_TOKEN'];
check('sheet LOGIN/Log diblokir (huruf besar/kecil)', run("isBlockedSheet_('LOGIN')") && run("isBlockedSheet_(' log ')") && !run("isBlockedSheet_('dbase')"));
let threw = false; try { run("assertSheetAllowed_('LOGIN')"); } catch (e) { threw = true; }
check('assertSheetAllowed_ melempar untuk LOGIN', threw);

// ── doPost: perutean & bentuk respons ──
const post = (obj, param) => JSON.parse(run(`doPost(${JSON.stringify({ postData: { contents: JSON.stringify(obj) }, parameter: param || {} })})`)._t);
check('doPost login', post({ action: 'login', user: 'sda01', password: 'baru123' }).status === 'success');
check('doPost gemini_ocr tanpa token → AUTH', post({ action: 'gemini_ocr', image_base64: 'AAAA' }).code === 'AUTH');
check('doPost changePassword/change_password dikenali', post({ action: 'changePassword', user: 'x', token: 'bad' }).code === 'AUTH' && post({ action: 'change_password', user: 'x', token: 'bad' }).code === 'AUTH');
props['REQUIRE_TOKEN'] = 'true';
check('doPost find_row tanpa token ditolak saat REQUIRE_TOKEN', post({ action: 'find_row', sheetName: 'dbase', idpel: '1' }).code === 'AUTH');
check('doPost readSheet tanpa token ditolak', post({ action: 'readSheet', sheetName: 'REPORT' }).code === 'AUTH');
delete props['REQUIRE_TOKEN'];
let blocked = post({ action: 'readSheet', sheetName: 'LOGIN' });
check('doPost readSheet LOGIN diblokir', blocked.ok === false || blocked.status === 'error');
blocked = post({ action: 'update_row_cells', sheetName: 'LOGIN', idpel: 'x', keyColumn: 'IDPEL', updates: {} });
check('doPost update_row_cells LOGIN diblokir', blocked.status === 'error');

// ── doGet read_sheet ──
var blockedW;
const get = (p) => run(`doGet(${JSON.stringify({ parameter: p })})`)._t;
check('GET read_sheet LOGIN diblokir', JSON.parse(get({ action: 'read_sheet', sheet: 'LOGIN' })).status === 'error');
props['ALLOW_LEGACY_LOGIN_READ'] = 'true';
check('transisi: GET LOGIN boleh bila ALLOW_LEGACY_LOGIN_READ', get({ action: 'read_sheet', sheet: 'LOGIN' }).startsWith('email,password'));
blockedW = post({ action: 'update_row_cells', sheetName: 'LOGIN', idpel: 'x', keyColumn: 'IDPEL', updates: {} });
check('transisi TIDAK membuka TULIS ke LOGIN', blockedW.status === 'error');
delete props['ALLOW_LEGACY_LOGIN_READ'];
sheets['dbase'] = mkSheet([['IDPEL', 'NAMA'], ['1', 'A, B']]);
check('GET read_sheet dbase → CSV', get({ action: 'read_sheet', sheet: 'dbase' }) === 'IDPEL,NAMA\n1,"A, B"');
props['REQUIRE_TOKEN'] = 'true';
check('GET read_sheet tanpa token ditolak saat REQUIRE_TOKEN', JSON.parse(get({ action: 'read_sheet', sheet: 'dbase' })).code === 'AUTH');
check('GET read_sheet dengan token sah lolos', get({ action: 'read_sheet', sheet: 'dbase', token: t2 }).startsWith('IDPEL'));
delete props['REQUIRE_TOKEN'];

// ── find_row dengan filter (bug lama: filter diabaikan) ──
sheets['REPORT'] = mkSheet([['Ticket_RP', 'IDPEL_RP', 'PENGADUAN_RP', 'STATUS_RP'], ['T1', '555', 'BUKA SEGEL', 'SELESAI'], ['T2', '555', 'PASANG KEMBALI', 'OPEN'], ['T3', '555', 'BUKA SEGEL', 'OPEN']]);
let fr = post({ action: 'find_row', sheetName: 'REPORT', idpel: '555', keyColumn: 'IDPEL_RP', filterColumn: 'PENGADUAN_RP', filterValue: 'BUKA SEGEL' });
check('find_row + filter → baris TERBARU yang cocok (T3 OPEN)', fr.status === 'success' && fr.row.Ticket_RP === 'T3' && fr.row.STATUS_RP === 'OPEN');
fr = post({ action: 'find_row', sheetName: 'REPORT', idpel: '555', keyColumn: 'IDPEL_RP' });
check('find_row tanpa filter → perilaku lama (baris pertama T1)', fr.row.Ticket_RP === 'T1');
fr = post({ action: 'find_row', sheetName: 'REPORT', idpel: 'T2', keyColumn: 'Ticket_RP' });
check('find_row by Ticket_RP', fr.status === 'success' && fr.row.IDPEL_RP === '555');

console.log(fails === 0 ? '\nSEMUA LULUS' : `\n${fails} GAGAL`);
process.exit(fails ? 1 : 0);
