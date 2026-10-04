package com.sipekat.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile MeterReadingDao _meterReadingDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `meter_readings` (`id` TEXT NOT NULL, `pelangganId` TEXT NOT NULL, `petugasId` TEXT NOT NULL, `standAngka` INTEGER NOT NULL, `fotoPath` TEXT, `fotoUrl` TEXT, `fotoDriveId` TEXT, `lat` REAL, `lng` REAL, `catatan` TEXT, `bulan` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isSynced` INTEGER NOT NULL, `minus` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '5d758ef0fc50f52e3b4bd03b9a033266')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `meter_readings`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsMeterReadings = new HashMap<String, TableInfo.Column>(14);
        _columnsMeterReadings.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeterReadings.put("pelangganId", new TableInfo.Column("pelangganId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeterReadings.put("petugasId", new TableInfo.Column("petugasId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeterReadings.put("standAngka", new TableInfo.Column("standAngka", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeterReadings.put("fotoPath", new TableInfo.Column("fotoPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeterReadings.put("fotoUrl", new TableInfo.Column("fotoUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeterReadings.put("fotoDriveId", new TableInfo.Column("fotoDriveId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeterReadings.put("lat", new TableInfo.Column("lat", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeterReadings.put("lng", new TableInfo.Column("lng", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeterReadings.put("catatan", new TableInfo.Column("catatan", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeterReadings.put("bulan", new TableInfo.Column("bulan", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeterReadings.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeterReadings.put("isSynced", new TableInfo.Column("isSynced", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeterReadings.put("minus", new TableInfo.Column("minus", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMeterReadings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMeterReadings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMeterReadings = new TableInfo("meter_readings", _columnsMeterReadings, _foreignKeysMeterReadings, _indicesMeterReadings);
        final TableInfo _existingMeterReadings = TableInfo.read(db, "meter_readings");
        if (!_infoMeterReadings.equals(_existingMeterReadings)) {
          return new RoomOpenHelper.ValidationResult(false, "meter_readings(com.sipekat.app.data.model.MeterReading).\n"
                  + " Expected:\n" + _infoMeterReadings + "\n"
                  + " Found:\n" + _existingMeterReadings);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "5d758ef0fc50f52e3b4bd03b9a033266", "38abf63b82fef10818fe9c02f5dc3a76");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "meter_readings");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `meter_readings`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(MeterReadingDao.class, MeterReadingDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public MeterReadingDao meterReadingDao() {
    if (_meterReadingDao != null) {
      return _meterReadingDao;
    } else {
      synchronized(this) {
        if(_meterReadingDao == null) {
          _meterReadingDao = new MeterReadingDao_Impl(this);
        }
        return _meterReadingDao;
      }
    }
  }
}
