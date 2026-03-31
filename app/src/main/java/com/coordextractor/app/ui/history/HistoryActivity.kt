package com.coordextractor.app.ui.history

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.coordextractor.app.R
import com.coordextractor.app.databinding.ActivityHistoryBinding
import com.coordextractor.app.model.Coordinate
import com.coordextractor.app.util.HistoryManager
import com.coordextractor.app.util.copyToClipboard
import com.coordextractor.app.util.openInMaps
import com.coordextractor.app.util.shareCoordinates
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Activity for displaying coordinate history
 */
class HistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyManager: HistoryManager
    private lateinit var adapter: HistoryAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        historyManager = HistoryManager.getInstance(this)
        
        setupToolbar()
        setupRecyclerView()
        observeHistory()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.history)
        }
        
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_clear_history -> {
                    showClearConfirmation()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            onItemClick = { coordinate ->
                copyToClipboard(coordinate.formatted)
            },
            onCopyClick = { coordinate ->
                copyToClipboard(coordinate.formatted)
            },
            onMapClick = { coordinate ->
                openInMaps(coordinate.latitude, coordinate.longitude)
            },
            onShareClick = { coordinate ->
                shareCoordinates(coordinate.formatted, coordinate.rawText)
            },
            onDeleteClick = { coordinate ->
                historyManager.removeFromHistory(coordinate)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = this@HistoryActivity.adapter
        }
    }
    
    private fun observeHistory() {
        lifecycleScope.launch {
            historyManager.historyFlow.collectLatest { history ->
                adapter.submitList(history)
                binding.emptyView.visibility = if (history.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (history.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }
    
    private fun showClearConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_history)
            .setMessage(R.string.clear_history_confirmation)
            .setPositiveButton(R.string.clear) { _, _ ->
                historyManager.clearHistory()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
