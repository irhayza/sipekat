package com.sipekat.app.ui.perbaikan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sipekat.app.data.local.SessionCache
import com.sipekat.app.data.model.DropdownOptions
import com.sipekat.app.data.model.Ticket
import com.sipekat.app.data.repository.PerbaikanRepository
import kotlinx.coroutines.launch

sealed class PerbaikanUiState {
    object Loading : PerbaikanUiState()
    data class Ready(val options: DropdownOptions, val tickets: List<Ticket>) : PerbaikanUiState()
    data class Error(val message: String) : PerbaikanUiState()
}

sealed class SubmitState {
    object Idle : SubmitState()
    object Loading : SubmitState()
    data class Success(val ticketId: String) : SubmitState()
    data class Error(val message: String) : SubmitState()
}

class PerbaikanViewModel : ViewModel() {

    private val repository = PerbaikanRepository()

    private val _uiState = MutableLiveData<PerbaikanUiState>()
    val uiState: LiveData<PerbaikanUiState> = _uiState

    private val _submitState = MutableLiveData<SubmitState>(SubmitState.Idle)
    val submitState: LiveData<SubmitState> = _submitState

    private val _selectedTicket = MutableLiveData<Ticket?>()
    val selectedTicket: LiveData<Ticket?> = _selectedTicket

    // Search filter
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    private var allTickets: List<Ticket> = emptyList()

    init {
        loadData()
    }

    fun loadData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = PerbaikanUiState.Loading
            try {
                val cache = SessionCache
                val options = cache.perbaikanOptions
                    ?: repository.getDropdownOptions()
                val tickets = if (forceRefresh || cache.perbaikanTickets.isEmpty()) {
                    repository.getActiveTicketsFull(cache.officerEmail, cache.officerNama)
                } else {
                    cache.perbaikanTickets
                }
                allTickets = tickets
                _uiState.value = PerbaikanUiState.Ready(options, tickets)
            } catch (e: Exception) {
                _uiState.value = PerbaikanUiState.Error(e.message ?: "Gagal memuat data.")
            }
        }
    }

    fun refresh() = loadData(forceRefresh = true)

    fun selectTicket(ticket: Ticket) {
        _selectedTicket.value = ticket
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterTickets(query)
    }

    private fun filterTickets(query: String) {
        val current = _uiState.value
        if (current !is PerbaikanUiState.Ready) return
        val filtered = if (query.isBlank()) {
            allTickets
        } else {
            val q = query.trim().lowercase()
            allTickets.filter {
                it.ticket.lowercase().contains(q) ||
                it.nama.lowercase().contains(q) ||
                it.idPelanggan.lowercase().contains(q) ||
                it.area.lowercase().contains(q)
            }
        }
        _uiState.value = PerbaikanUiState.Ready(current.options, filtered)
    }

    fun submitPenyelesaian(
        ticket: Ticket,
        jenis: String,
        penanganan: String,
        petugasNama: String,
        tindakan: String,
        fotoSebBase64: String?,
        fotoSesBase64: String?,
        lat: Double?,
        lng: Double?
    ) {
        _submitState.value = SubmitState.Loading
        viewModelScope.launch {
            try {
                val payload = mutableMapOf<String, Any?>(
                    "ticket" to ticket.ticket,
                    "idpel" to ticket.idPelanggan,
                    "nama" to ticket.nama,
                    "jenis" to jenis,
                    "penanganan" to penanganan,
                    "petugas" to petugasNama,
                    "tindakan" to tindakan,
                    "status" to "SELESAI"
                )
                if (lat != null) payload["lat"] = lat.toString()
                if (lng != null) payload["lng"] = lng.toString()

                // Upload photos
                if (!fotoSebBase64.isNullOrEmpty()) {
                    val fotoSebUrl = repository.uploadPhoto(fotoSebBase64, "foto_seb_${ticket.ticket}.jpg")
                    payload["fotoSeb"] = fotoSebUrl
                }
                if (!fotoSesBase64.isNullOrEmpty()) {
                    val fotoSesUrl = repository.uploadPhoto(fotoSesBase64, "foto_ses_${ticket.ticket}.jpg")
                    payload["fotoSes"] = fotoSesUrl
                }

                repository.updateRowCells(
                    sheetName = "REPORT",
                    keyValue = ticket.ticket,
                    keyColumn = "Ticket",
                    updates = mapOf(
                        "STATUS_RP" to "SELESAI",
                        "JENIS_RP" to jenis,
                        "PENANGANAN_RP" to penanganan,
                        "PETUGAS_RP" to petugasNama,
                        "TINDAKAN_RP" to tindakan,
                        "LAT_RP" to (lat?.toString() ?: ""),
                        "LNG_RP" to (lng?.toString() ?: ""),
                        "FOTO_SEB" to (payload["fotoSeb"] ?: ""),
                        "FOTO_SES" to (payload["fotoSes"] ?: "")
                    )
                )

                // Best-effort notification
                repository.notifySelesai(payload)

                _submitState.value = SubmitState.Success(ticket.ticket)
            } catch (e: Exception) {
                _submitState.value = SubmitState.Error(e.message ?: "Gagal menyimpan data.")
            }
        }
    }

    fun resetSubmitState() {
        _submitState.value = SubmitState.Idle
    }
}
