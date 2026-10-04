package com.sipekat.app.ui.profil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sipekat.app.R
import com.sipekat.app.SiPekatApp
import com.sipekat.app.config.AppConfig
import com.sipekat.app.data.local.SessionCache
import com.sipekat.app.data.repository.KunjunganRepository
import com.sipekat.app.databinding.FragmentProfilBinding
import com.sipekat.app.ui.login.LoginActivity
import kotlinx.coroutines.launch

class ProfilFragment : Fragment() {

    private var _binding: FragmentProfilBinding? = null
    private val binding get() = _binding!!
    private var userNama = ""
    private var userEmail = ""

    companion object {
        private const val ARG_NAMA = "arg_nama"
        private const val ARG_EMAIL= "arg_email"
        fun newInstance(nama: String, email: String) = ProfilFragment().apply {
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
        _binding = FragmentProfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvNama.text = userNama
        binding.tvEmail.text = userEmail
        binding.tvVersion.text = AppConfig.APP_VERSION

        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Keluar Aplikasi?")
                .setMessage("Sesi aktif Anda akan dihapus. Yakin ingin keluar?")
                .setPositiveButton("Keluar") { _, _ -> logout() }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun showChangePasswordDialog() {
        // Asumsi layout ini dibuat nanti. Kita buat view secara programatis atau biarkan error kompilasi dulu.
        // Untuk amannya, kita buat basic view programmatically jika R.layout.dialog_change_password belum ada.
        // Tapi karena instruksi sebelumnya menggunakan R.layout, saya akan menuliskannya sesuai contoh.
        // Nanti XML-nya akan di-generate.
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etOld = dialogView.findViewById<EditText>(R.id.et_old_password)
        val etNew = dialogView.findViewById<EditText>(R.id.et_new_password)
        val etConfirm = dialogView.findViewById<EditText>(R.id.et_confirm_password)

        AlertDialog.Builder(requireContext())
            .setTitle("Ganti Password")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val old = etOld.text.toString()
                val new = etNew.text.toString()
                val confirm = etConfirm.text.toString()
                if (new != confirm) {
                    Toast.makeText(requireContext(), "Password baru tidak cocok!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (new.length < 4) {
                    Toast.makeText(requireContext(), "Password minimal 4 karakter!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                changePassword(old, new)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun changePassword(oldPw: String, newPw: String) {
        lifecycleScope.launch {
            try {
                KunjunganRepository().changePassword(userEmail, oldPw, newPw)
                Toast.makeText(requireContext(), "Password berhasil diubah!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message ?: "Gagal mengubah password.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun logout() {
        val app = requireActivity().application as SiPekatApp
        app.sessionManager.logout()
        SessionCache.clear()
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
