package com.sipekat.app.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.sipekat.app.data.model.MeterReading;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MeterReadingDao_Impl implements MeterReadingDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MeterReading> __insertionAdapterOfMeterReading;

  private final EntityDeletionOrUpdateAdapter<MeterReading> __deletionAdapterOfMeterReading;

  private final EntityDeletionOrUpdateAdapter<MeterReading> __updateAdapterOfMeterReading;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  public MeterReadingDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMeterReading = new EntityInsertionAdapter<MeterReading>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `meter_readings` (`id`,`pelangganId`,`petugasId`,`standAngka`,`fotoPath`,`fotoUrl`,`fotoDriveId`,`lat`,`lng`,`catatan`,`bulan`,`createdAt`,`isSynced`,`minus`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MeterReading entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getPelangganId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getPelangganId());
        }
        if (entity.getPetugasId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getPetugasId());
        }
        statement.bindLong(4, entity.getStandAngka());
        if (entity.getFotoPath() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getFotoPath());
        }
        if (entity.getFotoUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getFotoUrl());
        }
        if (entity.getFotoDriveId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getFotoDriveId());
        }
        if (entity.getLat() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getLat());
        }
        if (entity.getLng() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getLng());
        }
        if (entity.getCatatan() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getCatatan());
        }
        if (entity.getBulan() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getBulan());
        }
        statement.bindLong(12, entity.getCreatedAt());
        final int _tmp = entity.isSynced() ? 1 : 0;
        statement.bindLong(13, _tmp);
        statement.bindLong(14, entity.getMinus());
      }
    };
    this.__deletionAdapterOfMeterReading = new EntityDeletionOrUpdateAdapter<MeterReading>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `meter_readings` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MeterReading entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
      }
    };
    this.__updateAdapterOfMeterReading = new EntityDeletionOrUpdateAdapter<MeterReading>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `meter_readings` SET `id` = ?,`pelangganId` = ?,`petugasId` = ?,`standAngka` = ?,`fotoPath` = ?,`fotoUrl` = ?,`fotoDriveId` = ?,`lat` = ?,`lng` = ?,`catatan` = ?,`bulan` = ?,`createdAt` = ?,`isSynced` = ?,`minus` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MeterReading entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getPelangganId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getPelangganId());
        }
        if (entity.getPetugasId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getPetugasId());
        }
        statement.bindLong(4, entity.getStandAngka());
        if (entity.getFotoPath() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getFotoPath());
        }
        if (entity.getFotoUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getFotoUrl());
        }
        if (entity.getFotoDriveId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getFotoDriveId());
        }
        if (entity.getLat() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getLat());
        }
        if (entity.getLng() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getLng());
        }
        if (entity.getCatatan() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getCatatan());
        }
        if (entity.getBulan() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getBulan());
        }
        statement.bindLong(12, entity.getCreatedAt());
        final int _tmp = entity.isSynced() ? 1 : 0;
        statement.bindLong(13, _tmp);
        statement.bindLong(14, entity.getMinus());
        if (entity.getId() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.getId());
        }
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE meter_readings SET isSynced = 1, fotoUrl = ?, fotoDriveId = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final MeterReading reading, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMeterReading.insert(reading);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final MeterReading reading, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfMeterReading.handle(reading);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final MeterReading reading, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfMeterReading.handle(reading);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markSynced(final String id, final String fotoUrl, final String driveId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSynced.acquire();
        int _argIndex = 1;
        if (fotoUrl == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, fotoUrl);
        }
        _argIndex = 2;
        if (driveId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, driveId);
        }
        _argIndex = 3;
        if (id == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, id);
        }
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MeterReading>> getAllFlow() {
    final String _sql = "SELECT * FROM meter_readings ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"meter_readings"}, new Callable<List<MeterReading>>() {
      @Override
      @NonNull
      public List<MeterReading> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPelangganId = CursorUtil.getColumnIndexOrThrow(_cursor, "pelangganId");
          final int _cursorIndexOfPetugasId = CursorUtil.getColumnIndexOrThrow(_cursor, "petugasId");
          final int _cursorIndexOfStandAngka = CursorUtil.getColumnIndexOrThrow(_cursor, "standAngka");
          final int _cursorIndexOfFotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "fotoPath");
          final int _cursorIndexOfFotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "fotoUrl");
          final int _cursorIndexOfFotoDriveId = CursorUtil.getColumnIndexOrThrow(_cursor, "fotoDriveId");
          final int _cursorIndexOfLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lat");
          final int _cursorIndexOfLng = CursorUtil.getColumnIndexOrThrow(_cursor, "lng");
          final int _cursorIndexOfCatatan = CursorUtil.getColumnIndexOrThrow(_cursor, "catatan");
          final int _cursorIndexOfBulan = CursorUtil.getColumnIndexOrThrow(_cursor, "bulan");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "isSynced");
          final int _cursorIndexOfMinus = CursorUtil.getColumnIndexOrThrow(_cursor, "minus");
          final List<MeterReading> _result = new ArrayList<MeterReading>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MeterReading _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpPelangganId;
            if (_cursor.isNull(_cursorIndexOfPelangganId)) {
              _tmpPelangganId = null;
            } else {
              _tmpPelangganId = _cursor.getString(_cursorIndexOfPelangganId);
            }
            final String _tmpPetugasId;
            if (_cursor.isNull(_cursorIndexOfPetugasId)) {
              _tmpPetugasId = null;
            } else {
              _tmpPetugasId = _cursor.getString(_cursorIndexOfPetugasId);
            }
            final int _tmpStandAngka;
            _tmpStandAngka = _cursor.getInt(_cursorIndexOfStandAngka);
            final String _tmpFotoPath;
            if (_cursor.isNull(_cursorIndexOfFotoPath)) {
              _tmpFotoPath = null;
            } else {
              _tmpFotoPath = _cursor.getString(_cursorIndexOfFotoPath);
            }
            final String _tmpFotoUrl;
            if (_cursor.isNull(_cursorIndexOfFotoUrl)) {
              _tmpFotoUrl = null;
            } else {
              _tmpFotoUrl = _cursor.getString(_cursorIndexOfFotoUrl);
            }
            final String _tmpFotoDriveId;
            if (_cursor.isNull(_cursorIndexOfFotoDriveId)) {
              _tmpFotoDriveId = null;
            } else {
              _tmpFotoDriveId = _cursor.getString(_cursorIndexOfFotoDriveId);
            }
            final Double _tmpLat;
            if (_cursor.isNull(_cursorIndexOfLat)) {
              _tmpLat = null;
            } else {
              _tmpLat = _cursor.getDouble(_cursorIndexOfLat);
            }
            final Double _tmpLng;
            if (_cursor.isNull(_cursorIndexOfLng)) {
              _tmpLng = null;
            } else {
              _tmpLng = _cursor.getDouble(_cursorIndexOfLng);
            }
            final String _tmpCatatan;
            if (_cursor.isNull(_cursorIndexOfCatatan)) {
              _tmpCatatan = null;
            } else {
              _tmpCatatan = _cursor.getString(_cursorIndexOfCatatan);
            }
            final String _tmpBulan;
            if (_cursor.isNull(_cursorIndexOfBulan)) {
              _tmpBulan = null;
            } else {
              _tmpBulan = _cursor.getString(_cursorIndexOfBulan);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            final int _tmpMinus;
            _tmpMinus = _cursor.getInt(_cursorIndexOfMinus);
            _item = new MeterReading(_tmpId,_tmpPelangganId,_tmpPetugasId,_tmpStandAngka,_tmpFotoPath,_tmpFotoUrl,_tmpFotoDriveId,_tmpLat,_tmpLng,_tmpCatatan,_tmpBulan,_tmpCreatedAt,_tmpIsSynced,_tmpMinus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getByPelanggan(final String pelangganId,
      final Continuation<? super List<MeterReading>> $completion) {
    final String _sql = "SELECT * FROM meter_readings WHERE pelangganId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (pelangganId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, pelangganId);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MeterReading>>() {
      @Override
      @NonNull
      public List<MeterReading> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPelangganId = CursorUtil.getColumnIndexOrThrow(_cursor, "pelangganId");
          final int _cursorIndexOfPetugasId = CursorUtil.getColumnIndexOrThrow(_cursor, "petugasId");
          final int _cursorIndexOfStandAngka = CursorUtil.getColumnIndexOrThrow(_cursor, "standAngka");
          final int _cursorIndexOfFotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "fotoPath");
          final int _cursorIndexOfFotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "fotoUrl");
          final int _cursorIndexOfFotoDriveId = CursorUtil.getColumnIndexOrThrow(_cursor, "fotoDriveId");
          final int _cursorIndexOfLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lat");
          final int _cursorIndexOfLng = CursorUtil.getColumnIndexOrThrow(_cursor, "lng");
          final int _cursorIndexOfCatatan = CursorUtil.getColumnIndexOrThrow(_cursor, "catatan");
          final int _cursorIndexOfBulan = CursorUtil.getColumnIndexOrThrow(_cursor, "bulan");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "isSynced");
          final int _cursorIndexOfMinus = CursorUtil.getColumnIndexOrThrow(_cursor, "minus");
          final List<MeterReading> _result = new ArrayList<MeterReading>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MeterReading _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpPelangganId;
            if (_cursor.isNull(_cursorIndexOfPelangganId)) {
              _tmpPelangganId = null;
            } else {
              _tmpPelangganId = _cursor.getString(_cursorIndexOfPelangganId);
            }
            final String _tmpPetugasId;
            if (_cursor.isNull(_cursorIndexOfPetugasId)) {
              _tmpPetugasId = null;
            } else {
              _tmpPetugasId = _cursor.getString(_cursorIndexOfPetugasId);
            }
            final int _tmpStandAngka;
            _tmpStandAngka = _cursor.getInt(_cursorIndexOfStandAngka);
            final String _tmpFotoPath;
            if (_cursor.isNull(_cursorIndexOfFotoPath)) {
              _tmpFotoPath = null;
            } else {
              _tmpFotoPath = _cursor.getString(_cursorIndexOfFotoPath);
            }
            final String _tmpFotoUrl;
            if (_cursor.isNull(_cursorIndexOfFotoUrl)) {
              _tmpFotoUrl = null;
            } else {
              _tmpFotoUrl = _cursor.getString(_cursorIndexOfFotoUrl);
            }
            final String _tmpFotoDriveId;
            if (_cursor.isNull(_cursorIndexOfFotoDriveId)) {
              _tmpFotoDriveId = null;
            } else {
              _tmpFotoDriveId = _cursor.getString(_cursorIndexOfFotoDriveId);
            }
            final Double _tmpLat;
            if (_cursor.isNull(_cursorIndexOfLat)) {
              _tmpLat = null;
            } else {
              _tmpLat = _cursor.getDouble(_cursorIndexOfLat);
            }
            final Double _tmpLng;
            if (_cursor.isNull(_cursorIndexOfLng)) {
              _tmpLng = null;
            } else {
              _tmpLng = _cursor.getDouble(_cursorIndexOfLng);
            }
            final String _tmpCatatan;
            if (_cursor.isNull(_cursorIndexOfCatatan)) {
              _tmpCatatan = null;
            } else {
              _tmpCatatan = _cursor.getString(_cursorIndexOfCatatan);
            }
            final String _tmpBulan;
            if (_cursor.isNull(_cursorIndexOfBulan)) {
              _tmpBulan = null;
            } else {
              _tmpBulan = _cursor.getString(_cursorIndexOfBulan);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            final int _tmpMinus;
            _tmpMinus = _cursor.getInt(_cursorIndexOfMinus);
            _item = new MeterReading(_tmpId,_tmpPelangganId,_tmpPetugasId,_tmpStandAngka,_tmpFotoPath,_tmpFotoUrl,_tmpFotoDriveId,_tmpLat,_tmpLng,_tmpCatatan,_tmpBulan,_tmpCreatedAt,_tmpIsSynced,_tmpMinus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPendingSync(final Continuation<? super List<MeterReading>> $completion) {
    final String _sql = "SELECT * FROM meter_readings WHERE isSynced = 0 ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MeterReading>>() {
      @Override
      @NonNull
      public List<MeterReading> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPelangganId = CursorUtil.getColumnIndexOrThrow(_cursor, "pelangganId");
          final int _cursorIndexOfPetugasId = CursorUtil.getColumnIndexOrThrow(_cursor, "petugasId");
          final int _cursorIndexOfStandAngka = CursorUtil.getColumnIndexOrThrow(_cursor, "standAngka");
          final int _cursorIndexOfFotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "fotoPath");
          final int _cursorIndexOfFotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "fotoUrl");
          final int _cursorIndexOfFotoDriveId = CursorUtil.getColumnIndexOrThrow(_cursor, "fotoDriveId");
          final int _cursorIndexOfLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lat");
          final int _cursorIndexOfLng = CursorUtil.getColumnIndexOrThrow(_cursor, "lng");
          final int _cursorIndexOfCatatan = CursorUtil.getColumnIndexOrThrow(_cursor, "catatan");
          final int _cursorIndexOfBulan = CursorUtil.getColumnIndexOrThrow(_cursor, "bulan");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "isSynced");
          final int _cursorIndexOfMinus = CursorUtil.getColumnIndexOrThrow(_cursor, "minus");
          final List<MeterReading> _result = new ArrayList<MeterReading>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MeterReading _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpPelangganId;
            if (_cursor.isNull(_cursorIndexOfPelangganId)) {
              _tmpPelangganId = null;
            } else {
              _tmpPelangganId = _cursor.getString(_cursorIndexOfPelangganId);
            }
            final String _tmpPetugasId;
            if (_cursor.isNull(_cursorIndexOfPetugasId)) {
              _tmpPetugasId = null;
            } else {
              _tmpPetugasId = _cursor.getString(_cursorIndexOfPetugasId);
            }
            final int _tmpStandAngka;
            _tmpStandAngka = _cursor.getInt(_cursorIndexOfStandAngka);
            final String _tmpFotoPath;
            if (_cursor.isNull(_cursorIndexOfFotoPath)) {
              _tmpFotoPath = null;
            } else {
              _tmpFotoPath = _cursor.getString(_cursorIndexOfFotoPath);
            }
            final String _tmpFotoUrl;
            if (_cursor.isNull(_cursorIndexOfFotoUrl)) {
              _tmpFotoUrl = null;
            } else {
              _tmpFotoUrl = _cursor.getString(_cursorIndexOfFotoUrl);
            }
            final String _tmpFotoDriveId;
            if (_cursor.isNull(_cursorIndexOfFotoDriveId)) {
              _tmpFotoDriveId = null;
            } else {
              _tmpFotoDriveId = _cursor.getString(_cursorIndexOfFotoDriveId);
            }
            final Double _tmpLat;
            if (_cursor.isNull(_cursorIndexOfLat)) {
              _tmpLat = null;
            } else {
              _tmpLat = _cursor.getDouble(_cursorIndexOfLat);
            }
            final Double _tmpLng;
            if (_cursor.isNull(_cursorIndexOfLng)) {
              _tmpLng = null;
            } else {
              _tmpLng = _cursor.getDouble(_cursorIndexOfLng);
            }
            final String _tmpCatatan;
            if (_cursor.isNull(_cursorIndexOfCatatan)) {
              _tmpCatatan = null;
            } else {
              _tmpCatatan = _cursor.getString(_cursorIndexOfCatatan);
            }
            final String _tmpBulan;
            if (_cursor.isNull(_cursorIndexOfBulan)) {
              _tmpBulan = null;
            } else {
              _tmpBulan = _cursor.getString(_cursorIndexOfBulan);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            final int _tmpMinus;
            _tmpMinus = _cursor.getInt(_cursorIndexOfMinus);
            _item = new MeterReading(_tmpId,_tmpPelangganId,_tmpPetugasId,_tmpStandAngka,_tmpFotoPath,_tmpFotoUrl,_tmpFotoDriveId,_tmpLat,_tmpLng,_tmpCatatan,_tmpBulan,_tmpCreatedAt,_tmpIsSynced,_tmpMinus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getById(final String id, final Continuation<? super MeterReading> $completion) {
    final String _sql = "SELECT * FROM meter_readings WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, id);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MeterReading>() {
      @Override
      @Nullable
      public MeterReading call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPelangganId = CursorUtil.getColumnIndexOrThrow(_cursor, "pelangganId");
          final int _cursorIndexOfPetugasId = CursorUtil.getColumnIndexOrThrow(_cursor, "petugasId");
          final int _cursorIndexOfStandAngka = CursorUtil.getColumnIndexOrThrow(_cursor, "standAngka");
          final int _cursorIndexOfFotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "fotoPath");
          final int _cursorIndexOfFotoUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "fotoUrl");
          final int _cursorIndexOfFotoDriveId = CursorUtil.getColumnIndexOrThrow(_cursor, "fotoDriveId");
          final int _cursorIndexOfLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lat");
          final int _cursorIndexOfLng = CursorUtil.getColumnIndexOrThrow(_cursor, "lng");
          final int _cursorIndexOfCatatan = CursorUtil.getColumnIndexOrThrow(_cursor, "catatan");
          final int _cursorIndexOfBulan = CursorUtil.getColumnIndexOrThrow(_cursor, "bulan");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "isSynced");
          final int _cursorIndexOfMinus = CursorUtil.getColumnIndexOrThrow(_cursor, "minus");
          final MeterReading _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpPelangganId;
            if (_cursor.isNull(_cursorIndexOfPelangganId)) {
              _tmpPelangganId = null;
            } else {
              _tmpPelangganId = _cursor.getString(_cursorIndexOfPelangganId);
            }
            final String _tmpPetugasId;
            if (_cursor.isNull(_cursorIndexOfPetugasId)) {
              _tmpPetugasId = null;
            } else {
              _tmpPetugasId = _cursor.getString(_cursorIndexOfPetugasId);
            }
            final int _tmpStandAngka;
            _tmpStandAngka = _cursor.getInt(_cursorIndexOfStandAngka);
            final String _tmpFotoPath;
            if (_cursor.isNull(_cursorIndexOfFotoPath)) {
              _tmpFotoPath = null;
            } else {
              _tmpFotoPath = _cursor.getString(_cursorIndexOfFotoPath);
            }
            final String _tmpFotoUrl;
            if (_cursor.isNull(_cursorIndexOfFotoUrl)) {
              _tmpFotoUrl = null;
            } else {
              _tmpFotoUrl = _cursor.getString(_cursorIndexOfFotoUrl);
            }
            final String _tmpFotoDriveId;
            if (_cursor.isNull(_cursorIndexOfFotoDriveId)) {
              _tmpFotoDriveId = null;
            } else {
              _tmpFotoDriveId = _cursor.getString(_cursorIndexOfFotoDriveId);
            }
            final Double _tmpLat;
            if (_cursor.isNull(_cursorIndexOfLat)) {
              _tmpLat = null;
            } else {
              _tmpLat = _cursor.getDouble(_cursorIndexOfLat);
            }
            final Double _tmpLng;
            if (_cursor.isNull(_cursorIndexOfLng)) {
              _tmpLng = null;
            } else {
              _tmpLng = _cursor.getDouble(_cursorIndexOfLng);
            }
            final String _tmpCatatan;
            if (_cursor.isNull(_cursorIndexOfCatatan)) {
              _tmpCatatan = null;
            } else {
              _tmpCatatan = _cursor.getString(_cursorIndexOfCatatan);
            }
            final String _tmpBulan;
            if (_cursor.isNull(_cursorIndexOfBulan)) {
              _tmpBulan = null;
            } else {
              _tmpBulan = _cursor.getString(_cursorIndexOfBulan);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            final int _tmpMinus;
            _tmpMinus = _cursor.getInt(_cursorIndexOfMinus);
            _result = new MeterReading(_tmpId,_tmpPelangganId,_tmpPetugasId,_tmpStandAngka,_tmpFotoPath,_tmpFotoUrl,_tmpFotoDriveId,_tmpLat,_tmpLng,_tmpCatatan,_tmpBulan,_tmpCreatedAt,_tmpIsSynced,_tmpMinus);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object pendingCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM meter_readings WHERE isSynced = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
