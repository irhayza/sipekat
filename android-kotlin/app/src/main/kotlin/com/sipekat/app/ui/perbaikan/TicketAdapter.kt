package com.sipekat.app.ui.perbaikan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sipekat.app.databinding.ItemTicketBinding
import com.sipekat.app.data.model.Ticket

class TicketAdapter(
    private val onClick: (Ticket) -> Unit
) : ListAdapter<Ticket, TicketAdapter.ViewHolder>(TicketDiffCallback()) {

    inner class ViewHolder(private val binding: ItemTicketBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ticket: Ticket) {
            binding.tvTicketNo.text = ticket.ticket
            binding.tvStatus.text = ticket.status
            binding.tvNama.text = ticket.nama
            binding.tvIdpel.text = "ID: ${ticket.idPelanggan}"
            binding.tvAlamat.text = ticket.alamat
            binding.tvKendala.text = ticket.kendala
            binding.root.setOnClickListener { onClick(ticket) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTicketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class TicketDiffCallback : DiffUtil.ItemCallback<Ticket>() {
    override fun areItemsTheSame(oldItem: Ticket, newItem: Ticket) = oldItem.ticket == newItem.ticket
    override fun areContentsTheSame(oldItem: Ticket, newItem: Ticket) = oldItem == newItem
}
