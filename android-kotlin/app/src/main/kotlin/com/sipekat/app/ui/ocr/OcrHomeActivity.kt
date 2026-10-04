package com.sipekat.app.ui.ocr

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sipekat.app.data.local.SessionCache
import com.sipekat.app.data.model.Customer
import com.sipekat.app.databinding.ActivityOcrHomeBinding
import com.sipekat.app.databinding.ItemCustomerBinding

class OcrHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOcrHomeBinding
    private lateinit var adapter: CustomerAdapter
    
    private var allCustomers: List<Customer> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        loadData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = CustomerAdapter { customer ->
            val intent = Intent(this, MeterReadingActivity::class.java).apply {
                putExtra("customer_id", customer.id)
                putExtra("customer_no", customer.noPelanggan)
                putExtra("customer_nama", customer.nama)
                putExtra("customer_alamat", customer.alamat)
                putExtra("stand_awal", customer.standAwal)
            }
            startActivity(intent)
        }
        binding.rvCustomers.layoutManager = LinearLayoutManager(this)
        binding.rvCustomers.adapter = adapter
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
        // Ambil data langsung dari cache yang sudah di-preload di Dashboard
        allCustomers = SessionCache.ocrCustomers
        adapter.submitList(allCustomers)
        binding.progressBar.visibility = View.GONE
    }

    private fun filter(query: String) {
        if (query.isBlank()) {
            adapter.submitList(allCustomers)
            return
        }
        val q = query.lowercase().trim()
        val filtered = allCustomers.filter {
            it.nama.lowercase().contains(q) || it.noPelanggan.lowercase().contains(q)
        }
        adapter.submitList(filtered)
    }
}

class CustomerAdapter(
    private val onClick: (Customer) -> Unit
) : ListAdapter<Customer, CustomerAdapter.ViewHolder>(CustomerDiffCallback()) {

    inner class ViewHolder(private val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(c: Customer) {
            binding.tvNama.text = c.nama
            binding.tvIdpel.text = "ID: ${c.noPelanggan}"
            binding.tvAlamat.text = c.alamat ?: "-"
            binding.root.setOnClickListener { onClick(c) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
}

class CustomerDiffCallback : DiffUtil.ItemCallback<Customer>() {
    override fun areItemsTheSame(oldItem: Customer, newItem: Customer) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Customer, newItem: Customer) = oldItem == newItem
}
