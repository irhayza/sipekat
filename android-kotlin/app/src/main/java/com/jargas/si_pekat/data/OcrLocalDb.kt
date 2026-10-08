package com.jargas.si_pekat.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.jargas.si_pekat.core.AppContext
import com.jargas.si_pekat.model.MeterReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Basis data lokal bacaan meter (antrean offline). Nama berkas ('ocr_meter.db'), versi (2) dan skema SAMA dengan
 * versi Flutter (sqflite) — jadi bila aplikasi ini dipasang menimpa versi Flutter (applicationId & kunci tanda
 * tangan sama), data bacaan offline yang belum terkirim ikut terbaca.
 */
private class OcrDbHelper(context: Context) : SQLiteOpenHelper(context, "ocr_meter.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
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
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE stand_meter_offline ADD COLUMN minus INTEGER DEFAULT 0")
            } catch (_: Exception) {
            }
        }
    }
}

object OcrLocalDb {
    private const val TABLE = "stand_meter_offline"
    private val helper by lazy { OcrDbHelper(AppContext.app) }

    private fun Cursor.str(col: String): String? = getColumnIndexOrThrow(col).let { if (isNull(it)) null else getString(it) }
    private fun Cursor.dbl(col: String): Double? = getColumnIndexOrThrow(col).let { if (isNull(it)) null else getDouble(it) }
    private fun Cursor.int(col: String): Int = getColumnIndexOrThrow(col).let { if (isNull(it)) 0 else getInt(it) }

    private fun Cursor.toReading() = MeterReading(
        id = str("id") ?: "",
        pelangganId = str("pelanggan_id") ?: "",
        petugasId = str("petugas_id") ?: "",
        standAngka = int("stand_angka"),
        fotoPath = str("foto_path"),
        fotoUrl = str("foto_url"),
        fotoDriveId = str("foto_drive_id"),
        lat = dbl("lat"),
        lng = dbl("lng"),
        catatan = str("catatan"),
        bulan = str("bulan") ?: "",
        createdAt = MeterReading.parseIso(str("created_at")),
        isSynced = int("is_synced") == 1,
        minus = int("minus"),
    )

    private fun query(where: String?, args: Array<String>?, orderBy: String?, limit: String?): List<MeterReading> =
        helper.readableDatabase.query(TABLE, null, where, args, null, null, orderBy, limit).use { c ->
            buildList { while (c.moveToNext()) add(c.toReading()) }
        }

    suspend fun saveReading(r: MeterReading) = withContext(Dispatchers.IO) {
        val v = ContentValues().apply {
            put("id", r.id); put("pelanggan_id", r.pelangganId); put("petugas_id", r.petugasId)
            put("stand_angka", r.standAngka); put("foto_path", r.fotoPath); put("foto_url", r.fotoUrl)
            put("foto_drive_id", r.fotoDriveId); put("lat", r.lat); put("lng", r.lng)
            put("catatan", r.catatan); put("bulan", r.bulan)
            put("created_at", MeterReading.formatIso(r.createdAt))
            put("is_synced", if (r.isSynced) 1 else 0); put("minus", r.minus)
        }
        helper.writableDatabase.insertWithOnConflict(TABLE, null, v, SQLiteDatabase.CONFLICT_REPLACE)
        Unit
    }

    suspend fun markSynced(id: String) = withContext(Dispatchers.IO) {
        helper.writableDatabase.update(TABLE, ContentValues().apply { put("is_synced", 1) }, "id = ?", arrayOf(id))
        Unit
    }

    suspend fun deleteReading(id: String) = withContext(Dispatchers.IO) {
        helper.writableDatabase.delete(TABLE, "id = ?", arrayOf(id))
        Unit
    }

    suspend fun getUnsynced(): List<MeterReading> = withContext(Dispatchers.IO) {
        query("is_synced = 0", null, null, null)
    }

    suspend fun getByPelanggan(pelangganId: String): List<MeterReading> = withContext(Dispatchers.IO) {
        query("pelanggan_id = ?", arrayOf(pelangganId), "bulan DESC", "24")
    }

    suspend fun countUnsynced(): Int = withContext(Dispatchers.IO) {
        helper.readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE WHERE is_synced = 0", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    suspend fun getAllReadings(): List<MeterReading> = withContext(Dispatchers.IO) {
        query(null, null, "created_at DESC", "100")
    }
}
