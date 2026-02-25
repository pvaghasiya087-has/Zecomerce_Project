package com.example.zecomerceuser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheet(
    private val onApply: (Double?, Double?, String?, Boolean, String?) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.layout_bottom_filter, container, false)

        val etMinPrice = view.findViewById<EditText>(R.id.etMinPrice)
        val etMaxPrice = view.findViewById<EditText>(R.id.etMaxPrice)
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerCategory)
        val checkInStock = view.findViewById<CheckBox>(R.id.checkInStock)
        val spinnerSort = view.findViewById<Spinner>(R.id.spinnerSort)
        val btnApply = view.findViewById<Button>(R.id.btnApplyFilter)

        // Category Adapter
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("All", "Electronics", "Clothing", "Shoes")
        )
        spinnerCategory.adapter = categoryAdapter

        // Sort Adapter
        val sortAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Recommended", "Price Low to High", "Price High to Low")
        )
        spinnerSort.adapter = sortAdapter

        btnApply.setOnClickListener {

            val minPrice = etMinPrice.text.toString().toDoubleOrNull()
            val maxPrice = etMaxPrice.text.toString().toDoubleOrNull()

            val category =
                if (spinnerCategory.selectedItem.toString() == "All") null
                else spinnerCategory.selectedItem.toString()

            val sortType = when (spinnerSort.selectedItemPosition) {
                1 -> "LOW_HIGH"
                2 -> "HIGH_LOW"
                else -> "RECOMMENDED"
            }

            onApply(
                minPrice,
                maxPrice,
                category,
                checkInStock.isChecked,
                sortType
            )

            dismiss()
        }

        return view
    }
}
