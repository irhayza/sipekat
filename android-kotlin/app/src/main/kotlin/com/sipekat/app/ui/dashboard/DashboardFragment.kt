package com.sipekat.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.sipekat.app.SiPekatApp
import com.sipekat.app.config.AppConfig
import com.sipekat.app.data.local.SessionCache
import com.sipekat.app.databinding.FragmentDashboardBinding
import com.sipekat.app.ui.login.LoginActivity
// Nanti uncomment jika sudah dibuat Activitynya
// import com.sipekat.app.ui.kunjungan.KunjunganDashboardActivity
// import com.sipekat.app.ui.ocr.OcrHomeActivity
// import com.sipekat.app.ui.pengaduan.PengaduanActivity
// import com.sipekat.app.ui.perbaikan.PerbaikanActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var userNama = ""
    private var userEmail = ""

    companion object {
        private const val ARG_NAMA = "arg_nama"
        private const val ARG_EMAIL= "arg_email"
        fun newInstance(nama: String, email: String) = DashboardFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_NAMA, nama)
                putString(ARG_EMAIL, email)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userNama = arguments?.getString(ARG_NAMA) ?: ""
        userEmail= arguments?.getString(ARG_EMAIL) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvWelcomeName.text = userNama
        binding.tvVersion.text = AppConfig.APP_VERSION

        if (!SessionCache.isPreloaded && !SessionCache.isPreloading) {
            CoroutineScope(Dispatchers.Main).launch {
                binding.warmingIndicator.visibility = View.VISIBLE
                SessionCache.preloadAll(userNama, userEmail)
                binding.warmingIndicator.visibility = View.GONE
            }
        }

        setupModuleCards()
        setupLogout()
    }

    private fun setupModuleCards() {
        // Nanti ganti intent ke masing-masing Activity saat sudah dibuat
        binding.cardPengaduan.setOnClickListener {
            startActivity(Intent(requireContext(), com.sipekat.app.ui.pengaduan.PengaduanActivity::class.java).apply {
                putExtra("user_nama", userNama)
                putExtra("user_email", userEmail)
            })
        }
        binding.cardMeter.setOnClickListener {
            startActivity(Intent(requireContext(), com.sipekat.app.ui.ocr.OcrHomeActivity::class.java).apply {
                putExtra("user_nama", userNama)
                putExtra("user_email", userEmail)
            })
        }
        binding.cardKunjungan.setOnClickListener {
            startActivity(Intent(requireContext(), com.sipekat.app.ui.kunjungan.KunjunganDashboardActivity::class.java))
        }
        binding.cardPerbaikan.setOnClickListener {
            startActivity(Intent(requireContext(), com.sipekat.app.ui.perbaikan.PerbaikanActivity::class.java))
        }
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Keluar Aplikasi?")
                .setMessage("Sesi aktif Anda akan dihapus. Yakin ingin keluar?")
                .setPositiveButton("Keluar") { _, _ ->
                    val app = requireActivity().application as SiPekatApp
                    app.sessionManager.logout()
                    SessionCache.clear()
                    val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
