package com.sipekat.app.ui.pengaduan

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.sipekat.app.R
import com.sipekat.app.databinding.ActivityPengaduanBinding

class PengaduanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPengaduanBinding
    private val viewModel: PengaduanViewModel by viewModels()

    private var userNama = ""
    private var userEmail = ""

    private val kategoriOptions = listOf(
        "Kebocoran Jargas",
        "Meter Tidak Berjalan / Rusak",
        "Tagihan Melonjak / Salah Catat",
        "Buka Segel",
        "Lainnya"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPengaduanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userNama = intent.getStringExtra("user_nama") ?: ""
        userEmail = intent.getStringExtra("user_email") ?: ""

        setupToolbar()
        setupViews()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViews() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kategoriOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerKategori.adapter = adapter

        binding.tilIdpel.setEndIconOnClickListener {
            val idpel = binding.etIdpel.text.toString().trim()
            if (idpel.isEmpty()) {
                binding.tilIdpel.error = "Masukkan ID Pelanggan"
                return@setEndIconOnClickListener
            }
            binding.tilIdpel.error = null
            viewModel.searchCustomer(idpel)
        }

        binding.btnSubmit.setOnClickListener {
            val idpel = binding.etIdpel.text.toString().trim()
            val pelapor = binding.etNamaPelapor.text.toString().trim()
            val hp = binding.etNoHp.text.toString().trim()
            val ket = binding.etKeterangan.text.toString().trim()
            val kategori = binding.spinnerKategori.selectedItem.toString()

            if (idpel.isEmpty() || pelapor.isEmpty() || hp.isEmpty() || ket.isEmpty()) {
                Toast.makeText(this, "Mohon lengkapi semua field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val payload = mapOf(
                "idpel" to idpel,
                "nama_pelapor" to pelapor,
                "no_hp" to hp,
                "kategori" to kategori,
                "keterangan" to ket,
                "petugas" to userNama
            )
            viewModel.submitComplaint(payload)
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is PengaduanUiState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                }
                is PengaduanUiState.LoadingSearch -> {
                    binding.tvCustomerInfo.visibility = View.GONE
                    binding.tilIdpel.error = null
                }
                is PengaduanUiState.CustomerFound -> {
                    binding.tvCustomerInfo.visibility = View.VISIBLE
                    binding.tvCustomerInfo.text = "Ditemukan: ${state.nama}\nAlamat: ${state.alamat}"
                    binding.tvCustomerInfo.setTextColor(resources.getColor(R.color.success, theme))
                }
                is PengaduanUiState.CustomerNotFound -> {
                    binding.tvCustomerInfo.visibility = View.GONE
                    binding.tilIdpel.error = state.message
                }
                is PengaduanUiState.Submitting -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSubmit.isEnabled = false
                }
                is PengaduanUiState.SubmitSuccess -> {
                    binding.progressBar.visibility = View.GONE
                    AlertDialog.Builder(this)
                        .setTitle("Berhasil")
                        .setMessage("Pengaduan berhasil dikirim dan tersimpan di server.")
                        .setPositiveButton("OK") { _, _ -> finish() }
                        .setCancelable(false)
                        .show()
                }
                is PengaduanUiState.SubmitError -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
