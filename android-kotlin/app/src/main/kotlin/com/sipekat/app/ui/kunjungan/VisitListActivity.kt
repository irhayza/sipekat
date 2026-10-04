package com.sipekat.app.ui.kunjungan

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sipekat.app.data.local.SessionCache
import com.sipekat.app.data.model.VisitCustomer
import com.sipekat.app.databinding.ActivityVisitListBinding
import com.sipekat.app.databinding.ItemVisitCustomerBinding
import java.text.NumberFormat
import java.util.Locale

class VisitListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVisitListBinding
    private lateinit var adapter: VisitCustomerAdapter
    
    private var allCustomers: List<VisitCustomer> = emptyList()
    private var tipe = "" // "kunjungan" atau "pembukaan"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVisitListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tipe = intent.getStringExtra("tipe") ?: "kunjungan"

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        loadData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (tipe == "kunjungan") "Kunjungan Pelanggan" else "Pembukaan / Pasang"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = VisitCustomerAdapter { customer ->
            if (tipe == "kunjungan") {
                // val intent = Intent(this, VisitDetailActivity::class.java).apply {
                //     putExtra("idpel", customer.idpel)
                // }
                // startActivity(intent)
            } else {
                // val intent = Intent(this, ReopenDetailActivity::class.java).apply {
                //     putExtra("idpel", customer.idpel)
                // }
                // startActivity(intent)
            }
        }
        binding.rvVisitList.layoutManager = LinearLayoutManager(this)
        binding.rvVisitList.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        // Ambil data dari cache
        allCustomers = if (tipe == "kunjungan") SessionCache.kunjunganList else SessionCache.pembukaanList
        
        adapter.submitList(allCustomers)
        binding.progressBar.visibility = View.GONE
        
        if (allCustomers.isEmpty()) binding.tvEmpty.visibility = View.VISIBLE
    }

    private fun filter(query: String) {
        if (query.isBlank()) {
            adapter.submitList(allCustomers)
            binding.tvEmpty.visibility = if (allCustomers.isEmpty()) View.VISIBLE else View.GONE
            return
        }
        val q = query.lowercase().trim()
        val filtered = allCustomers.filter {
            it.nama.lowercase().contains(q) || 
            it.idpel.lowercase().contains(q) ||
            it.nomgrt.lowercase().contains(q)
        }
        adapter.submitList(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }
}

class VisitCustomerAdapter(
    private val onClick: (VisitCustomer) -> Unit
) : ListAdapter<VisitCustomer, VisitCustomerAdapter.ViewHolder>(VisitDiffCallback()) {

    inner class ViewHolder(private val binding: ItemVisitCustomerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(c: VisitCustomer) {
            binding.tvNama.text = c.nama
            binding.tvIdpel.text = "ID: ${c.idpel}"
            binding.tvAlamat.text = c.alamat.takeIf { it.isNotBlank() } ?: "-"
            binding.tvNomgrt.text = "No MGRT: " + (c.nomgrt.takeIf { it.isNotBlank() } ?: "-")
            binding.tvKendala.text = "Ket: " + (c.kendala.takeIf { it.isNotBlank() } ?: "-")
            
            val rpFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            rpFormat.maximumFractionDigits = 0
            binding.tvRupiah.text = rpFormat.format(c.rupiah)

            binding.root.setOnClickListener { onClick(c) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVisitCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
}

class VisitDiffCallback : DiffUtil.ItemCallback<VisitCustomer>() {
    override fun areItemsTheSame(oldItem: VisitCustomer, newItem: VisitCustomer) = oldItem.idpel == newItem.idpel
    override fun areContentsTheSame(oldItem: VisitCustomer, newItem: VisitCustomer) = oldItem == newItem
}
