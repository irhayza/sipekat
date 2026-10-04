package com.sipekat.app.ui.login

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.sipekat.app.R
import com.sipekat.app.SiPekatApp
import com.sipekat.app.data.local.SessionCache
import com.sipekat.app.databinding.ActivityLoginBinding
import com.sipekat.app.ui.main.MainActivity
import com.sipekat.app.util.GpsResult
import com.sipekat.app.util.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) proceedWithLogin()
        else showErr("Aplikasi membutuhkan izin lokasi/GPS. Mohon berikan izin lokasi lalu coba lagi.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SiPekatApp
        if (app.sessionManager.isLoggedIn) {
            val nama = app.sessionManager.sessionNama ?: ""
            val email= app.sessionManager.sessionUser ?: ""
            SessionCache.init(app.sessionManager)
            SessionCache.initFromLocal(nama, email)
            navigateToMain(nama, email)
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SessionCache.init((application as SiPekatApp).sessionManager)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etUserId.text.toString().trim()
            val pass  = binding.etPassword.text.toString()
            var ok = true
            if (email.isEmpty()) { binding.tilUserId.error = "User ID wajib diisi"; ok = false }
            else binding.tilUserId.error = null
            if (pass.isEmpty()) { binding.tilPassword.error = "Password wajib diisi"; ok = false }
            else binding.tilPassword.error = null
            if (ok) checkLocationPermissionThenLogin()
        }

        binding.ivPasswordToggle.setOnClickListener {
            val isHidden = binding.etPassword.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            if (isHidden) {
                binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivPasswordToggle.setImageResource(android.R.drawable.ic_menu_view)
            } else {
                binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivPasswordToggle.setImageResource(android.R.drawable.ic_menu_close_clear_cancel) // placeholders
            }
            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        }
    }

    private fun checkLocationPermissionThenLogin() {
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) checkGpsThenLogin()
        else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun checkGpsThenLogin() {
        CoroutineScope(Dispatchers.Main).launch {
            setLoading(true, "Memeriksa GPS perangkat...")
            val gps = LocationHelper.getCurrentLocation(this@LoginActivity, 6_000L)
            if (!gps.isOk) {
                setLoading(false)
                val errMsg = when (gps.result) {
                    GpsResult.SERVICE_DISABLED -> "GPS perangkat Anda mati. Mohon aktifkan GPS lalu coba lagi."
                    GpsResult.PERMISSION_DENIED, GpsResult.PERMISSION_DENIED_FOREVER -> "Aplikasi membutuhkan izin lokasi/GPS. Mohon berikan izin."
                    else -> "GPS perangkat Anda harus aktif untuk masuk ke aplikasi."
                }
                showErr(errMsg)
                return@launch
            }
            proceedWithLogin()
        }
    }

    private fun proceedWithLogin() = viewModel.login(
        binding.etUserId.text.toString().trim(),
        binding.etPassword.text.toString()
    )

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is LoginUiState.Idle -> setLoading(false)
                is LoginUiState.Loading -> setLoading(true, state.stage)
                is LoginUiState.Error -> { setLoading(false); showErr(state.message) }
                is LoginUiState.Success -> {
                    val app = application as SiPekatApp
                    app.sessionManager.sessionUser = state.email
                    app.sessionManager.sessionNama = state.nama
                    navigateToMain(state.nama, state.email)
                }
            }
        }
    }

    private fun showErr(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean, stage: String = "") {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.tvLoadingStage.visibility = if (loading && stage.isNotEmpty()) View.VISIBLE else View.GONE
        binding.tvLoadingStage.text = stage
        binding.btnLogin.isEnabled = !loading
        binding.etUserId.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
        if (loading) binding.tvError.visibility = View.GONE
    }

    private fun navigateToMain(nama: String, email: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_USER_NAMA, nama)
            putExtra(MainActivity.EXTRA_USER_EMAIL, email)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
