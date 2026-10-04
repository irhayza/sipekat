package com.sipekat.app.ui.kunjungan

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sipekat.app.databinding.ActivityKunjunganDashboardBinding

class KunjunganDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityKunjunganDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKunjunganDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

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
