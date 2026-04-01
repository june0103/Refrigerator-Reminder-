package com.management.refrigeratorreminder.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.management.refrigeratorreminder.R
import com.management.refrigeratorreminder.databinding.ItemPantryBinding
import com.management.refrigeratorreminder.domain.model.FreshnessStatus
import com.management.refrigeratorreminder.ui.model.PantryItemPresentation

class PantryItemAdapter(
    private val showActions: Boolean,
    private val onItemClick: (PantryItemPresentation) -> Unit,
    private val onActionClick: (PantryItemPresentation, View) -> Unit,
) : ListAdapter<PantryItemPresentation, PantryItemAdapter.PantryItemViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PantryItemViewHolder {
        val binding = ItemPantryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PantryItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PantryItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PantryItemViewHolder(
        private val binding: ItemPantryBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PantryItemPresentation) {
            val context = binding.root.context
            binding.textName.text = item.name
            binding.textMeta.text = UiFormatter.formatMeta(context, item.category, item.storageType, item.quantity)
            binding.textExpiry.text = context.getString(
                R.string.item_expiry_value,
                UiFormatter.formatDate(item.expiryDate),
            )
            binding.textDays.text = UiFormatter.formatDays(context, item.freshnessStatus, item.daysUntilExpiry)
            binding.chipStatus.text = context.getString(item.freshnessStatus.labelRes)
            binding.buttonAction.visibility = if (showActions) View.VISIBLE else View.GONE
            val (bgColor, fgColor) = statusColors(item.freshnessStatus)
            binding.chipStatus.chipBackgroundColor = ContextCompat.getColorStateList(context, bgColor)
            binding.chipStatus.setTextColor(ContextCompat.getColor(context, fgColor))
            binding.root.setOnClickListener { onItemClick(item) }
            binding.buttonAction.setOnClickListener { onActionClick(item, it) }
        }
    }

    private fun statusColors(status: FreshnessStatus): Pair<Int, Int> = when (status) {
        FreshnessStatus.EXPIRED -> R.color.status_expired_bg to R.color.status_expired_fg
        FreshnessStatus.TODAY -> R.color.status_today_bg to R.color.status_today_fg
        FreshnessStatus.SOON -> R.color.status_soon_bg to R.color.status_soon_fg
        FreshnessStatus.SAFE -> R.color.status_safe_bg to R.color.status_safe_fg
        FreshnessStatus.CONSUMED,
        FreshnessStatus.DISCARDED,
        -> R.color.status_done_bg to R.color.status_done_fg
    }

    private object DiffCallback : DiffUtil.ItemCallback<PantryItemPresentation>() {
        override fun areItemsTheSame(oldItem: PantryItemPresentation, newItem: PantryItemPresentation): Boolean {
            return oldItem.itemId == newItem.itemId
        }

        override fun areContentsTheSame(oldItem: PantryItemPresentation, newItem: PantryItemPresentation): Boolean {
            return oldItem == newItem
        }
    }
}
