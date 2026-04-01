package com.management.refrigeratorreminder.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.management.refrigeratorreminder.R
import com.management.refrigeratorreminder.databinding.ItemSuggestionBinding
import com.management.refrigeratorreminder.domain.model.IngredientSource
import com.management.refrigeratorreminder.domain.model.IngredientSuggestion

class IngredientSuggestionAdapter(
    private val onSuggestionClick: (IngredientSuggestion) -> Unit,
) : ListAdapter<IngredientSuggestion, IngredientSuggestionAdapter.SuggestionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val binding = ItemSuggestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SuggestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SuggestionViewHolder(
        private val binding: ItemSuggestionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: IngredientSuggestion) {
            val context = binding.root.context
            if (item.isDirectAdd) {
                binding.textTitle.text = context.getString(R.string.suggestion_direct_add, item.displayName)
                binding.textSubtitle.text = context.getString(R.string.suggestion_direct_add_subtitle)
                binding.chipSource.text = context.getString(R.string.suggestion_source_direct)
            } else {
                binding.textTitle.text = item.displayName
                binding.textSubtitle.text = context.getString(item.category.labelRes)
                binding.chipSource.text = when (item.source) {
                    IngredientSource.USER -> context.getString(R.string.suggestion_source_user)
                    IngredientSource.MFDS,
                    IngredientSource.SEED,
                    -> context.getString(R.string.suggestion_source_default)
                }
            }
            binding.root.setOnClickListener { onSuggestionClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<IngredientSuggestion>() {
        override fun areItemsTheSame(oldItem: IngredientSuggestion, newItem: IngredientSuggestion): Boolean {
            return oldItem.id == newItem.id && oldItem.displayName == newItem.displayName
        }

        override fun areContentsTheSame(oldItem: IngredientSuggestion, newItem: IngredientSuggestion): Boolean {
            return oldItem == newItem
        }
    }
}
