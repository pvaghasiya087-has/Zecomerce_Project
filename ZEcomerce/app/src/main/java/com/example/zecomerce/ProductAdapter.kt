package com.example.zecomerce

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class ProductAdapter(
    private val context: Context,
    private val productList: List<Product>,
    private val onUpdate: (Product, Int) -> Unit,
    private val onDelete: (Product, Int) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.productImage)
        val productName: TextView = itemView.findViewById(R.id.productName)
        val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        val productDesc: TextView = itemView.findViewById(R.id.productDesc)
        val productRating: RatingBar = itemView.findViewById(R.id.productRating)
        val reviewCount: TextView = itemView.findViewById(R.id.reviewCount)
        val btnUpdate: ImageView = itemView.findViewById(R.id.btnUpdate)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
        val txtStock: TextView = itemView.findViewById(R.id.txtStock)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun getItemCount(): Int = productList.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        holder.productName.text = product.name
        holder.productPrice.text = "₹${product.price}"
        holder.productDesc.text = product.description

        // Set placeholder initially
        holder.productRating.rating = product.avgRating.toFloat()
        holder.reviewCount.text = "(${product.reviewCount})"

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

        // Load latest avgRating & reviewCount from Firestore
        db.collection("products")
            .document(product.id)
            .get()
            .addOnSuccessListener { doc ->
                val avg = doc.getDouble("avgRating") ?: 0.0
                val count = doc.getLong("reviewCount") ?: 0
                holder.productRating.rating = avg.toFloat()
                holder.reviewCount.text = "($count reviews)"
            }

        // Load product image
        if (product.imageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(product.imageUrl)
                .into(holder.productImage)
        } else {
            holder.productImage.setImageResource(R.drawable.load_image)
        }

        holder.btnUpdate.setOnClickListener { onUpdate(product, position) }
        holder.btnDelete.setOnClickListener { onDelete(product, position) }

        // Click to open details
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ProductsDetailsActivity::class.java)
            intent.putExtra("product", product)
            context.startActivity(intent)
        }
    }
}
