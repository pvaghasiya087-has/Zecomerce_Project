package com.example.zecomerceuser

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton

class SearchProductAdapter(
    private var productList: List<Product>,
    private val onAddToCart: (Product) -> Unit
) : RecyclerView.Adapter<SearchProductAdapter.SearchViewHolder>() {

    private var filteredList: MutableList<Product> = productList.toMutableList()

    inner class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)
        val txtName: TextView = itemView.findViewById(R.id.txtName)
        val txtPrice: TextView = itemView.findViewById(R.id.txtPrice)
        val txtDesc: TextView = itemView.findViewById(R.id.txtDesc)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        val txtReviews: TextView = itemView.findViewById(R.id.txtReviews)
        val btnAddCart: MaterialButton = itemView.findViewById(R.id.btnAddCart)
        val txtStock: TextView = itemView.findViewById(R.id.txtStock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return SearchViewHolder(view)
    }

    override fun getItemCount(): Int = filteredList.size

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val product = filteredList[position]

        holder.txtName.text = product.name
        holder.txtPrice.text = "₹${product.price}"
        holder.txtDesc.text = product.description
        holder.ratingBar.rating = product.avgRating.toFloat()
        holder.txtReviews.text = "(${product.reviewCount} reviews)"
        val active = product.isActive ?: true
        holder.btnAddCart.visibility = if (active) View.VISIBLE else View.GONE

        val stock = product.stock

        when {
            stock <= 0 -> {
                holder.txtStock.text = "Out of Stock"
                holder.txtStock.setTextColor(
                    holder.itemView.context.getColor(android.R.color.holo_red_dark)
                )
                holder.btnAddCart.visibility = View.GONE
            }

            stock in 1..10 -> {
                holder.txtStock.text = "Only $stock items left"
                holder.txtStock.setTextColor(
                    holder.itemView.context.getColor(android.R.color.holo_orange_dark)
                )
            }

            else -> {
                holder.txtStock.text = "In Stock ($stock available)"
                holder.txtStock.setTextColor(
                    holder.itemView.context.getColor(android.R.color.holo_green_dark)
                )
            }
        }



        Glide.with(holder.itemView.context)
            .load(product.imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.imgProduct)

        holder.btnAddCart.setOnClickListener {
            onAddToCart(product)
            Toast.makeText(
                holder.itemView.context,
                "${product.name} added to cart",
                Toast.LENGTH_SHORT
            ).show()
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ProductsDetailsActivity::class.java)
            intent.putExtra("productId", product.id)
            holder.itemView.context.startActivity(intent)
        }
    }

    // ✅ Independent Search Filter
    fun filter(query: String) {

        val searchText = query.trim().lowercase()

        filteredList = if (searchText.isEmpty()) {
            productList.toMutableList()
        } else {
            productList.filter { product ->

                val nameMatch = product.name.lowercase().contains(searchText)
                val descMatch = product.description.lowercase().contains(searchText)
                val priceMatch = product.price.lowercase().contains(searchText)
                val categorymatch =product.category.lowercase().contains(searchText)

                nameMatch || descMatch || priceMatch || categorymatch
            }.toMutableList()
        }

        notifyDataSetChanged()
    }


    // Optional: update full list if needed
    fun updateList(newList: List<Product>) {
        productList = newList
        filteredList = newList.toMutableList()
        notifyDataSetChanged()
    }

    fun sortProducts(sortType: String) {

        when (sortType) {

            "LOW_HIGH" -> {
                filteredList.sortBy { it.price.toDoubleOrNull() ?: 0.0 }
            }

            "HIGH_LOW" -> {
                filteredList.sortByDescending { it.price.toDoubleOrNull() ?: 0.0 }
            }

            "RECOMMENDED" -> {
                // Example: sort by rating * review count
                filteredList.sortByDescending {
                    it.avgRating * it.reviewCount
                }
            }
        }

        notifyDataSetChanged()
    }

    fun applyAdvancedFilter(
        minPrice: Double?,
        maxPrice: Double?,
        category: String?,
        inStock: Boolean,
        sortType: String?
    ) {

        filteredList = productList.filter { product ->

            val priceValue = product.price.toDoubleOrNull() ?: 0.0

            val matchesMin = minPrice == null || priceValue >= minPrice
            val matchesMax = maxPrice == null || priceValue <= maxPrice
            val matchesCategory = category == null || product.category == category
            val matchesStock = !inStock || (product.isActive == true)

            matchesMin && matchesMax && matchesCategory && matchesStock
        }.toMutableList()

        // Apply Sorting
        sortType?.let {
            when (it) {
                "LOW_HIGH" -> filteredList.sortBy { p -> p.price.toDoubleOrNull() ?: 0.0 }
                "HIGH_LOW" -> filteredList.sortByDescending { p -> p.price.toDoubleOrNull() ?: 0.0 }
                "RECOMMENDED" -> filteredList.sortByDescending { p -> p.avgRating * p.reviewCount }
            }
        }

        notifyDataSetChanged()
    }


}
