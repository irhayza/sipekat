package com.sipekat.app.ui.pengaduan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sipekat.app.data.local.SessionCache
import com.sipekat.app.data.repository.PengaduanRepository
import kotlinx.coroutines.launch

sealed class PengaduanUiState {
    object Idle : PengaduanUiState()
    object LoadingSearch : PengaduanUiState()
    data class CustomerFound(val nama: String, val alamat: String, val source: String) : PengaduanUiState()
    data class CustomerNotFound(val message: String) : PengaduanUiState()
    
    object Submitting : PengaduanUiState()
    object SubmitSuccess : PengaduanUiState()
    data class SubmitError(val message: String) : PengaduanUiState()
}

class PengaduanViewModel : ViewModel() {
    private val repo = PengaduanRepository()
    private val _state = MutableLiveData<PengaduanUiState>(PengaduanUiState.Idle)
    val state: LiveData<PengaduanUiState> = _state

    fun searchCustomer(idpel: String) {
        if (idpel.isBlank()) return
        _state.value = PengaduanUiState.LoadingSearch
        viewModelScope.launch {
            try {
                // Cari di cache lokal dulu
                val cached = SessionCache.findCustomerById(idpel)
                if (cached != null) {
                    _state.value = PengaduanUiState.CustomerFound(
                        nama = cached.nama,
                        alamat = cached.alamat,
                        source = cached.source
                    )
                    return@launch
                }
                
                // Jika tidak ada di cache, cari via API
                val apiResult = repo.checkCustomerId(idpel)
                if (apiResult != null) {
                    _state.value = PengaduanUiState.CustomerFound(
                        nama = apiResult["nama"] ?: "",
                        alamat = apiResult["alamat"] ?: "",
                        source = "Server"
                    )
                } else {
                    _state.value = PengaduanUiState.CustomerNotFound("ID Pelanggan tidak ditemukan")
                }
            } catch (e: Exception) {
                _state.value = PengaduanUiState.CustomerNotFound("Gagal mencari data: ${e.message}")
            }
        }
    }

    fun submitComplaint(payload: Map<String, Any?>) {
        _state.value = PengaduanUiState.Submitting
        viewModelScope.launch {
            try {
                val res = repo.submitComplaint(payload)
                if (res["ok"] == true || res["status"] == "success") {
                    _state.value = PengaduanUiState.SubmitSuccess
                } else {
                    _state.value = PengaduanUiState.SubmitError(res["message"]?.toString() ?: "Gagal mengirim pengaduan")
                }
            } catch (e: Exception) {
                _state.value = PengaduanUiState.SubmitError(e.message ?: "Terjadi kesalahan sistem")
            }
        }
    }
    
    fun resetState() {
        _state.value = PengaduanUiState.Idle
    }
}
