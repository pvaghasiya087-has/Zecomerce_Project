package com.example.zecomerceuser

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton

class ProductAdapter(
    private val productList: List<Product>,
    private val onAddToCart: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)
        val txtName: TextView = itemView.findViewById(R.id.txtName)
        val txtPrice: TextView = itemView.findViewById(R.id.txtPrice)
        val txtDesc: TextView = itemView.findViewById(R.id.txtDesc)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        val txtReviews: TextView = itemView.findViewById(R.id.txtReviews)
        val txtStock: TextView = itemView.findViewById(R.id.txtStock)
        val btnAddCart: MaterialButton = itemView.findViewById(R.id.btnAddCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int = productList.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        holder.txtName.text = product.name.ifEmpty { "Product" }
        holder.txtPrice.text = if (product.price.isNotEmpty()) "₹${product.price}" else "₹0"
        holder.txtDesc.text = product.description.ifEmpty { "No description" }

        holder.ratingBar.rating = product.avgRating.toFloat()

        val stock = product.stock

        when {
            stock <= 0 -> {
                holder.txtStock.text = "Out of Stock"
                holder.txtStock.setTextColor(
                    holder.itemView.context.getColor(android.R.color.holo_red_dark)
                )
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

        val active = when (product.isActive) {
            true -> true
            false -> false
            null -> true
        }



        holder.btnAddCart.visibility = if (active) View.VISIBLE else View.GONE

       // holder.btnAddCart.visibility = if (active) View.VISIBLE else View.GONE
        // Safe review count
        val reviewCount = product.reviewCount
        holder.txtReviews.text = "($reviewCount reviews)"

        // Safe image loading
        Glide.with(holder.itemView.context)
            .load(if (product.imageUrl.isNotEmpty()) product.imageUrl else R.drawable.ic_launcher_background)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(holder.imgProduct)



        holder.btnAddCart.setOnClickListener {
            onAddToCart(product)
            Toast.makeText(holder.itemView.context, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ProductsDetailsActivity::class.java)
            intent.putExtra("productId", product.id)
            holder.itemView.context.startActivity(intent)
            Log.d("CLICK", "Clicked ${product.id}")
        }
        // Debug log
        Log.d("DEBUG", "Product ${product.name} isActive=${product.isActive} -> visibility=${holder.btnAddCart.visibility}")
    }
}
