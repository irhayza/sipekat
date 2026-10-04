package com.sipekat.app.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sipekat.app.data.local.SessionCache
import com.sipekat.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    data class Loading(val stage: String = "") : LoginUiState()
    data class Success(val nama: String, val email: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel : ViewModel() {
    private val repo = AuthRepository()
    private val _state = MutableLiveData<LoginUiState>(LoginUiState.Idle)
    val state: LiveData<LoginUiState> = _state

    fun login(userId: String, password: String) {
        if (userId.isBlank())  { _state.value = LoginUiState.Error("User ID wajib diisi");   return }
        if (password.isEmpty()){ _state.value = LoginUiState.Error("Password wajib diisi");  return }
        viewModelScope.launch {
            _state.value = LoginUiState.Loading("Melakukan autentikasi...")
            try {
                val result = repo.login(userId.trim(), password)
                _state.value = LoginUiState.Loading("Menyiapkan data awal...")
                SessionCache.preloadAll(
                    nama  = result.nama,
                    email = result.email,
                    onProgress = { stage -> _state.postValue(LoginUiState.Loading(stage)) }
                )
                _state.value = LoginUiState.Success(result.nama, result.email)
            } catch (e: Exception) {
                _state.value = LoginUiState.Error(e.message ?: "Login gagal. Coba lagi.")
            }
        }
    }
}
