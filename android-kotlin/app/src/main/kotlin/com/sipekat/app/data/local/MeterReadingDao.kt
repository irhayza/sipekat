package com.sipekat.app.data.local

import androidx.room.*
import com.sipekat.app.data.model.MeterReading
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterReadingDao {

    @Query("SELECT * FROM meter_readings ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<MeterReading>>

    @Query("SELECT * FROM meter_readings WHERE pelangganId = :pelangganId ORDER BY createdAt DESC")
    suspend fun getByPelanggan(pelangganId: String): List<MeterReading>

    @Query("SELECT * FROM meter_readings WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getPendingSync(): List<MeterReading>

    @Query("SELECT * FROM meter_readings WHERE id = :id")
    suspend fun getById(id: String): MeterReading?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: MeterReading)

    @Update
    suspend fun update(reading: MeterReading)

    @Delete
    suspend fun delete(reading: MeterReading)

    @Query("UPDATE meter_readings SET isSynced = 1, fotoUrl = :fotoUrl, fotoDriveId = :driveId WHERE id = :id")
    suspend fun markSynced(id: String, fotoUrl: String?, driveId: String?)

    @Query("SELECT COUNT(*) FROM meter_readings WHERE isSynced = 0")
    suspend fun pendingCount(): Int
}
