package com.sipekat.app.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.sipekat.app.R
import com.sipekat.app.databinding.ActivityMainBinding
import com.sipekat.app.ui.dashboard.DashboardFragment
import com.sipekat.app.ui.pesan.PesanFragment
import com.sipekat.app.ui.profil.ProfilFragment
import com.sipekat.app.ui.riwayat.RiwayatFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var userNama: String = ""
    private var userEmail: String = ""

    companion object {
        const val EXTRA_USER_NAMA = "extra_user_nama"
        const val EXTRA_USER_EMAIL = "extra_user_email"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userNama = intent.getStringExtra(EXTRA_USER_NAMA) ?: ""
        userEmail= intent.getStringExtra(EXTRA_USER_EMAIL) ?: ""

        if (savedInstanceState == null) {
            loadFragment(DashboardFragment.newInstance(userNama, userEmail))
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_beranda -> loadFragment(DashboardFragment.newInstance(userNama, userEmail))
                R.id.nav_riwayat -> loadFragment(RiwayatFragment())
                R.id.nav_pesan   -> loadFragment(PesanFragment())
                R.id.nav_profil  -> loadFragment(ProfilFragment.newInstance(userNama, userEmail))
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
