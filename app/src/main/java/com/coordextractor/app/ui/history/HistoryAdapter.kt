package com.coordextractor.app.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.coordextractor.app.databinding.ItemHistoryBinding
import com.coordextractor.app.model.Coordinate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for history RecyclerView
 */
class HistoryAdapter(
    private val onItemClick: (Coordinate) -> Unit,
    private val onCopyClick: (Coordinate) -> Unit,
    private val onMapClick: (Coordinate) -> Unit,
    private val onShareClick: (Coordinate) -> Unit,
    private val onDeleteClick: (Coordinate) -> Unit
) : ListAdapter<Coordinate, HistoryAdapter.HistoryViewHolder>(CoordinateDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        
        fun bind(coordinate: Coordinate) {
            binding.apply {
                tvCoordinates.text = coordinate.formatted
                tvRawText.text = coordinate.rawText
                tvTimestamp.text = dateFormat.format(Date(coordinate.timestamp))
                
                // Click listeners
                root.setOnClickListener { onItemClick(coordinate) }
                btnCopy.setOnClickListener { onCopyClick(coordinate) }
                btnMap.setOnClickListener { onMapClick(coordinate) }
                btnShare.setOnClickListener { onShareClick(coordinate) }
                btnDelete.setOnClickListener { onDeleteClick(coordinate) }
            }
        }
    }
    
    private class CoordinateDiffCallback : DiffUtil.ItemCallback<Coordinate>() {
        override fun areItemsTheSame(oldItem: Coordinate, newItem: Coordinate): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }
        
        override fun areContentsTheSame(oldItem: Coordinate, newItem: Coordinate): Boolean {
            return oldItem == newItem
        }
    }
}
