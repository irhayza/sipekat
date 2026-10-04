package com.sipekat.app.ui.kunjungan

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sipekat.app.data.local.SessionCache
import com.sipekat.app.databinding.ActivityKunjunganDashboardBinding

class KunjunganDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityKunjunganDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKunjunganDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        val nama = SessionCache.officerNama.ifEmpty { "Petugas" }
        val role = SessionCache.officerRole.ifEmpty { "petugas" }
        binding.tvUserName.text = nama
        binding.tvUserRole.text = role.uppercase()
        binding.tvUserInitial.text = if (nama.isNotEmpty()) nama.substring(0, 1).uppercase() else "P"

        binding.cardKunjungan.setOnClickListener {
            val intent = Intent(this, VisitListActivity::class.java).apply {
                putExtra("tipe", "kunjungan")
            }
            startActivity(intent)
        }

        binding.cardPembukaan.setOnClickListener {
            val intent = Intent(this, VisitListActivity::class.java).apply {
                putExtra("tipe", "pembukaan")
            }
            startActivity(intent)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
}
