package com.example.zecomerceuser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CartAdapterReadOnly(
    private val items: List<CartItem>
) : RecyclerView.Adapter<CartAdapterReadOnly.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.cartItemName)
        val quantity: TextView = view.findViewById(R.id.cartItemQuantity)
        val price: TextView = view.findViewById(R.id.cartItemPrice)
        val image: ImageView = view.findViewById(R.id.cartItemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart_readonly, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.name.text = item.name
        holder.quantity.text = "Qty: ${item.quantity}"
        holder.price.text = "₹${item.price}"

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.ic_profile)   // fallback image
            .error(R.drawable.ic_profile)         // error image
            .into(holder.image)
    }

    override fun getItemCount(): Int = items.size
}
