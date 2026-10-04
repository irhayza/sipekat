package com.sipekat.app.ui.ocr

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sipekat.app.data.local.MeterReadingDao
import com.sipekat.app.data.model.MeterReading
import com.sipekat.app.data.repository.OcrRepository
import com.sipekat.app.util.ImageUtils
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class MeterReadingState {
    object Idle : MeterReadingState()
    object Submitting : MeterReadingState()
    object Success : MeterReadingState()
    data class Error(val message: String) : MeterReadingState()
}

class MeterReadingViewModel : ViewModel() {

    private val repo = OcrRepository()
    private val _state = MutableLiveData<MeterReadingState>(MeterReadingState.Idle)
    val state: LiveData<MeterReadingState> = _state
    
    // We can inject Dao later, but for now we skip local save for simplicity and directly upload.
    // Or we can save to Room then upload.

    fun submitReading(
        customerId: String,
        petugasNama: String,
        stand: Int,
        photoFile: File,
        catatan: String,
        lat: Double?,
        lng: Double?,
        bulan: String
    ) {
        _state.value = MeterReadingState.Submitting
        viewModelScope.launch {
            try {
                val base64 = ImageUtils.compressAndEncodeBase64(photoFile)
                val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                val fileName = "METER_${customerId}_${formatter.format(Date())}.jpg"
                
                val payload = mapOf(
                    "action" to "sync_ocr_dapel",
                    "idpel" to customerId,
                    "petugas" to petugasNama,
                    "hasil_ocr" to stand,
                    "catatan" to catatan,
                    "lat" to lat,
                    "lng" to lng,
                    "image_base64" to base64,
                    "file_name" to fileName
                )
                
                val res = repo.postToOcr(payload)
                if (res["status"] == "success" || res["ok"] == true) {
                    _state.value = MeterReadingState.Success
                } else {
                    _state.value = MeterReadingState.Error(res["message"]?.toString() ?: "Gagal upload data")
                }
            } catch (e: Exception) {
                _state.value = MeterReadingState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }
}
