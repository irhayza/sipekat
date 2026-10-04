import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../models/ocr/meter_reading.dart';

class OcrLocalDbService {
  OcrLocalDbService._();
  static final OcrLocalDbService instance = OcrLocalDbService._();

  Database? _db;

  Future<Database> get db async {
    _db ??= await _initDb();
    return _db!;
  }

  Future<Database> _initDb() async {
    final dbPath = await getDatabasesPath();
    return openDatabase(
      join(dbPath, 'ocr_meter.db'),
      version: 2,
      onCreate: (db, _) async {
        await db.execute('''
          CREATE TABLE stand_meter_offline (
            id            TEXT PRIMARY KEY,
            pelanggan_id  TEXT NOT NULL,
            petugas_id    TEXT NOT NULL,
            stand_angka   INTEGER NOT NULL,
            foto_path     TEXT,
            foto_url      TEXT,
            foto_drive_id TEXT,
            lat           REAL,
            lng           REAL,
            catatan       TEXT,
            bulan         TEXT NOT NULL,
            created_at    TEXT NOT NULL,
            is_synced     INTEGER DEFAULT 0,
            minus         INTEGER DEFAULT 0
          )
        ''');
      },
      onUpgrade: (db, oldVersion, newVersion) async {
        if (oldVersion < 2) {
          try {
            await db.execute('ALTER TABLE stand_meter_offline ADD COLUMN minus INTEGER DEFAULT 0');
          } catch (_) {}
        }
      },
    );
  }

  Future<void> saveReading(MeterReading r) async {
    final database = await db;
    await database.insert(
      'stand_meter_offline',
      r.toLocalDb(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<void> markSynced(String id) async {
    final database = await db;
    await database.update(
      'stand_meter_offline',
      {'is_synced': 1},
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  Future<void> deleteReading(String id) async {
    final database = await db;
    await database.delete(
      'stand_meter_offline',
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  Future<List<MeterReading>> getUnsynced() async {
    final database = await db;
    final rows     = await database.query(
      'stand_meter_offline',
      where: 'is_synced = 0',
    );
    return rows.map(MeterReading.fromLocalDb).toList();
  }

  Future<List<MeterReading>> getByPelanggan(String pelangganId) async {
    final database = await db;
    final rows     = await database.query(
      'stand_meter_offline',
      where: 'pelanggan_id = ?',
      whereArgs: [pelangganId],
      orderBy: 'bulan DESC',
      limit: 24,
    );
    return rows.map(MeterReading.fromLocalDb).toList();
  }

  Future<int> countUnsynced() async {
    final database = await db;
    final res = await database.rawQuery(
      'SELECT COUNT(*) as c FROM stand_meter_offline WHERE is_synced = 0',
    );
    return (res.first['c'] as int?) ?? 0;
  }

  Future<List<MeterReading>> getAllReadings() async {
    final database = await db;
    final rows     = await database.query(
      'stand_meter_offline',
      orderBy: 'created_at DESC',
      limit: 100,
    );
    return rows.map(MeterReading.fromLocalDb).toList();
  }
}
