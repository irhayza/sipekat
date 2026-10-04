package com.sipekat.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "meter_readings")
data class MeterReading(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val pelangganId: String = "",
    val petugasId: String = "",
    val standAngka: Int = 0,
    val fotoPath: String? = null,
    val fotoUrl: String? = null,
    val fotoDriveId: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val catatan: String? = null,
    val bulan: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val minus: Int = 0
)
