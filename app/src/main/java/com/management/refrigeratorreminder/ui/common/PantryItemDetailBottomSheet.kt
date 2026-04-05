package com.management.refrigeratorreminder.ui.common

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.management.refrigeratorreminder.databinding.BottomSheetPantryItemDetailBinding
import com.management.refrigeratorreminder.domain.model.FreshnessStatus
import com.management.refrigeratorreminder.domain.model.ItemCategory
import com.management.refrigeratorreminder.domain.model.StorageType
import com.management.refrigeratorreminder.ui.model.PantryItemPresentation
import java.time.LocalDate

class PantryItemDetailBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetPantryItemDetailBinding? = null
    private val binding get() = _binding!!

    private val itemId: String by lazy { requireArguments().getString(ARG_ITEM_ID).orEmpty() }
    private val itemName: String by lazy { requireArguments().getString(ARG_ITEM_NAME).orEmpty() }
    private val itemCategory: ItemCategory by lazy {
        ItemCategory.valueOf(requireArguments().getString(ARG_ITEM_CATEGORY).orEmpty())
    }
    private val itemStorageType: StorageType by lazy {
        StorageType.valueOf(requireArguments().getString(ARG_ITEM_STORAGE).orEmpty())
    }
    private val itemFreshnessStatus: FreshnessStatus by lazy {
        FreshnessStatus.valueOf(requireArguments().getString(ARG_ITEM_STATUS).orEmpty())
    }
    private val expiryDate: LocalDate by lazy {
        LocalDate.parse(requireArguments().getString(ARG_ITEM_EXPIRY_DATE).orEmpty())
    }
    private val daysUntilExpiry: Int by lazy { requireArguments().getInt(ARG_ITEM_DAYS_UNTIL) }
    private val quantityValue: String? by lazy { requireArguments().getString(ARG_ITEM_QUANTITY) }
    private val noteValue: String? by lazy { requireArguments().getString(ARG_ITEM_NOTE) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetPantryItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        binding.textName.text = itemName
        binding.textMeta.text = context.getString(itemCategory.labelRes) +
            " / " +
            context.getString(itemStorageType.labelRes)
        binding.textExpiryValue.text = UiFormatter.formatDate(expiryDate)
        binding.textCategoryValue.text = context.getString(itemCategory.labelRes)
        binding.textStorageValue.text = context.getString(itemStorageType.labelRes)
        binding.textQuantityValue.text = quantityValue?.ifBlank { null } ?: "-"
        binding.textNoteValue.text = noteValue?.ifBlank { null } ?: "-"
        binding.textDays.text = UiFormatter.formatDays(context, itemFreshnessStatus, daysUntilExpiry)
        binding.chipStatus.text = context.getString(itemFreshnessStatus.labelRes)

        binding.imageIngredient.setImageResource(
            IngredientVisuals.ingredientIconRes(itemName, itemCategory),
        )
        binding.imageIngredient.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, IngredientVisuals.ingredientTintRes(itemCategory)),
        )
        binding.layoutIngredientIcon.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, IngredientVisuals.ingredientBackgroundRes(itemCategory)),
        )
        binding.imageRisk.setImageResource(IngredientVisuals.riskIconRes(itemFreshnessStatus))
        binding.imageRisk.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, IngredientVisuals.riskTintRes(itemFreshnessStatus)),
        )
        binding.chipStatus.chipBackgroundColor = ContextCompat.getColorStateList(
            context,
            IngredientVisuals.statusBackgroundRes(itemFreshnessStatus),
        )
        binding.chipStatus.setTextColor(
            ContextCompat.getColor(context, IngredientVisuals.statusForegroundRes(itemFreshnessStatus)),
        )

        binding.buttonEdit.setOnClickListener { dispatchAction(ACTION_EDIT) }
        binding.buttonConsume.setOnClickListener { dispatchAction(ACTION_CONSUME) }
        binding.buttonDiscard.setOnClickListener { dispatchAction(ACTION_DISCARD) }
        binding.buttonDelete.setOnClickListener { dispatchAction(ACTION_DELETE) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun dispatchAction(action: String) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            bundleOf(
                RESULT_ACTION to action,
                RESULT_ITEM_ID to itemId,
            ),
        )
        dismiss()
    }

    companion object {
        const val TAG = "PantryItemDetailBottomSheet"
        const val REQUEST_KEY = "pantry_item_detail_request"
        const val RESULT_ACTION = "result_action"
        const val RESULT_ITEM_ID = "result_item_id"
        const val ACTION_EDIT = "edit"
        const val ACTION_CONSUME = "consume"
        const val ACTION_DISCARD = "discard"
        const val ACTION_DELETE = "delete"

        private const val ARG_ITEM_ID = "arg_item_id"
        private const val ARG_ITEM_NAME = "arg_item_name"
        private const val ARG_ITEM_CATEGORY = "arg_item_category"
        private const val ARG_ITEM_STORAGE = "arg_item_storage"
        private const val ARG_ITEM_STATUS = "arg_item_status"
        private const val ARG_ITEM_EXPIRY_DATE = "arg_item_expiry_date"
        private const val ARG_ITEM_DAYS_UNTIL = "arg_item_days_until"
        private const val ARG_ITEM_QUANTITY = "arg_item_quantity"
        private const val ARG_ITEM_NOTE = "arg_item_note"

        fun newInstance(item: PantryItemPresentation): PantryItemDetailBottomSheet {
            return PantryItemDetailBottomSheet().apply {
                arguments = bundleOf(
                    ARG_ITEM_ID to item.itemId,
                    ARG_ITEM_NAME to item.name,
                    ARG_ITEM_CATEGORY to item.category.name,
                    ARG_ITEM_STORAGE to item.storageType.name,
                    ARG_ITEM_STATUS to item.freshnessStatus.name,
                    ARG_ITEM_EXPIRY_DATE to item.expiryDate.toString(),
                    ARG_ITEM_DAYS_UNTIL to item.daysUntilExpiry,
                    ARG_ITEM_QUANTITY to item.quantity,
                    ARG_ITEM_NOTE to item.note,
                )
            }
        }
    }
}
